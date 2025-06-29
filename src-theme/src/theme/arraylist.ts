import { getModuleSettings } from '../integration/rest';
import type { ChoiceSetting, ChooseSetting, ConfigurableSetting } from '../integration/types';
import { primaryRgb, secondaryRgb } from '../util/Theme/ThemeManager';
import type {Readable} from "svelte/store";


interface RGBColor { r: number; g: number; b: number; }
let currentPrimaryRgb = '';
let currentSecondaryRgb = '';

export function subscribeColors() {

    const unsub1 = (primaryRgb as Readable<string>).subscribe((v) => {
        currentPrimaryRgb = v;
        arraylistGradient();
    });
    const unsub2 = (secondaryRgb as Readable<string>).subscribe((v) => {
        currentSecondaryRgb = v;
        arraylistGradient();
    });
    return [unsub1, unsub2] as [() => void, () => void];
}


export async function getPrefixAsync(name: string) {
    const settings = await getModuleSettings(name);
    let value = '';
    let mode = settings.value.find(n => n.valueType == 'CHOICE');
    if (mode != null) {
        const cMode = mode as ChoiceSetting;
        value = ' ' + cMode.active;
    } else {
        mode = settings.value.find(n => n.valueType == 'CONFIGURABLE');
        if (mode != null) {
            const cMode = mode as ConfigurableSetting;
            const mode1 = cMode.value.find(n => n.valueType == 'CHOICE');
            const mode2 = cMode.value.find(n => n.valueType == 'CHOOSE');
            if (mode1 != null) {
                const cMode1 = mode1 as ChoiceSetting;
                value = ' ' + cMode1.active;
            } else if (mode2 != null) {
                const cMode1 = mode2 as ChooseSetting;
                value = ' ' + cMode1.value;
            }
        }
    }
    return value || '';
}


function parseRgbString(str: string): RGBColor {

    const parts = str.split(',').map((x) => parseInt(x.trim()));
    return { r: parts[0] || 0, g: parts[1] || 0, b: parts[2] || 0 };
}

export function colorInterpolate(a: RGBColor, b: RGBColor, t: number): RGBColor {
    t = Math.max(0, Math.min(1, t));
    return {
        r: Math.round(a.r * (1 - t) + b.r * t),
        g: Math.round(a.g * (1 - t) + b.g * t),
        b: Math.round(a.b * (1 - t) + b.b * t),
    };
}

let speed = 50;
let progress = 0;
const tSpeed = (0.04 / 20) * speed;

export function arraylistGradient() {
    const container = document.getElementById('arraylist');
    if (!container) return;

    const rgb1 = parseRgbString(currentPrimaryRgb);
    const rgb2 = parseRgbString(currentSecondaryRgb);
    const children = container.children as HTMLCollectionOf<HTMLElement>;
    const total = children.length;

    for (let i = 0; i < total; i++) {
        const el = children[i];
        if (el.id !== 'module-name') continue;

        const pct = 1 - i / total + 0.5 * Math.sin(0.5 * i + progress);
        const { r, g, b } = colorInterpolate(rgb1, rgb2, pct);

        el.style.color = `rgba(${r}, ${g}, ${b}, 0.8)`;
        el.style.textShadow = `0.25px 0.25px 0 rgb(${r}, ${g}, ${b})`;
        el.style.filter = `drop-shadow(0 0 4px rgba(${r}, ${g}, ${b}, 0.1))`;
        el.style.backgroundImage = `
      linear-gradient(rgba(0,0,0,0.1), rgba(0,0,0,0.1)),
      linear-gradient(rgba(${r}, ${g}, ${b}, 0.2), rgba(${r}, ${g}, ${b}, 0.2))
    `;
        el.style.backgroundBlendMode = 'overlay';

        const sidebar = el.querySelector('.side-bar') as HTMLElement;
        if (sidebar) {
            sidebar.style.backgroundColor = `rgba(${r}, ${g}, ${b}, 1)`;
            sidebar.style.boxShadow = `0 0 6px rgba(${r}, ${g}, ${b}, 0.8)`;
        }
    }

    progress += tSpeed;
    if (progress >= Math.PI * 2) progress = 0;
}

export function destroyGradient(intervalId: number, unsubs: [() => void, () => void]) {
    clearInterval(intervalId);
    unsubs[0]();
    unsubs[1]();
}
