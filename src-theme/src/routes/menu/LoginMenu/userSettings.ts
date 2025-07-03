import {writable} from 'svelte/store';


const STORAGE_KEY = 'userSettings';

function createUserSettingsStore() {
    let initialValue = {
        username: '',
        uid: '',
        isDev: false,
        isOwner: false,
        hwid: '',
        developer: '',
        avatar: ''
    };


    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
        try {
            initialValue = JSON.parse(stored);
        } catch (e) {
            console.error('Failed to parse userSettings from localStorage:', e);
        }
    }


    const {subscribe, set, update} = writable(initialValue);


    subscribe((value) => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
    });


    return {
        subscribe,
        set,
        update
    };
}

export const userSettings = createUserSettingsStore();
