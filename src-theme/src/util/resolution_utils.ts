import { writable, derived } from "svelte/store";

const getWindowSize = () => {
    if (typeof window === 'undefined') return { w: 1920, h: 1080 }; // 默认回退值
    return { w: window.innerWidth, h: window.innerHeight };
};

const initial = getWindowSize();

export const width = writable(initial.w);
export const height = writable(initial.h);

if (typeof window !== 'undefined') {
    window.addEventListener("resize", () => {
        width.set(window.innerWidth);
        height.set(window.innerHeight);
    });
}

export const resolutionCoefficient = derived(
    [width, height],
    ([$width, $height]) => {
        const baseResolution = { width: 1920, height: 1080 };
        const currentAspect = $width / $height;

        const wRatio = $width / baseResolution.width;
        const hRatio = $height / baseResolution.height;

        let min = Math.min(wRatio, hRatio);

        if (currentAspect < 2) {
            min = Math.max(min, 0.45);
        }

        return Math.min(1, Math.max(0.1337, min));
    }
);

export function windowSize() {


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
