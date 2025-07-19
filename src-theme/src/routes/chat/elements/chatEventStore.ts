import {writable} from 'svelte/store';

export const chatEvents = {
    wheel: writable<WheelEvent | null>(null),
    keydown: writable<KeyboardEvent | null>(null)
};
