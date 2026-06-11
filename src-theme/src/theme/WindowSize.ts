import {writable} from "svelte/store";

export function WindowSize() {
    const width = writable(window.innerWidth);
    const height = writable(window.innerHeight);

    function updateSize() {
        width.set(window.innerWidth);
        height.set(window.innerHeight);
    }

    window.addEventListener("resize", updateSize);


    updateSize();

    return {
        width,
        height,
        destroy() {
            window.removeEventListener("resize", updateSize);
        }
    };
}
