import {writable} from 'svelte/store';

const STORAGE_KEY = 'logoVariant';
const logoVariants = 2;

function createLogoStorage() {

    const initialValue = typeof window !== 'undefined'
        ? parseInt(localStorage.getItem(STORAGE_KEY) || '1')
        : 1;

    const {subscribe, set, update} = writable(initialValue);

    return {
        subscribe,
        set: (value: number) => {
            if (typeof window !== 'undefined') {
                localStorage.setItem(STORAGE_KEY, value.toString());
            }
            set(value);
        },
        update: (updater: (value: number) => number) => {
            update(current => {
                const newValue = updater(current);
                if (typeof window !== 'undefined') {
                    localStorage.setItem(STORAGE_KEY, newValue.toString());
                }
                return newValue;
            });
        }
    };
}

export const currentLogo = createLogoStorage();
export {logoVariants};
