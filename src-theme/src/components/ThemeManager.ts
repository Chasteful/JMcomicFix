import {writable, derived, type Writable, type Readable} from 'svelte/store';
import {getModuleSettings} from '../integration/rest';
import {listen} from '../integration/ws';
import type {ConfigurableSetting, TextSetting} from '../integration/types';
import type {ClickGuiValueChangeEvent} from "../integration/events";

type ColorFormats = {
    rgb: string;
    hsl: string;
    hex: string;
    raw: number;
};

type ThemeColors = {
    primary: ColorFormats;
    secondary: ColorFormats;
};


function color4bToRgb(color4b: number): { r: number; g: number; b: number } {
    const unsigned = color4b >>> 0;
    return {
        r: (unsigned >> 16) & 0xff,
        g: (unsigned >> 8) & 0xff,
        b: unsigned & 0xff
    };
}

function rgbToHsl(r: number, g: number, b: number): { h: number; s: number; l: number } {
    r /= 255;
    g /= 255;
    b /= 255;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    let h = 0;
    let s = 0;
    const l = (max + min) / 2;

    if (max !== min) {
        const d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch (max) {
            case r:
                h = (g - b) / d + (g < b ? 6 : 0);
                break;
            case g:
                h = (b - r) / d + 2;
                break;
            case b:
                h = (r - g) / d + 4;
                break;
        }
        h /= 6;
    }

    return {
        h: Math.round(h * 360),
        s: Math.round(s * 100),
        l: Math.round(l * 100)
    };
}

function rgbToHex(r: number, g: number, b: number): string {
    return `#${[r, g, b]
        .map((x) => x.toString(16).padStart(2, '0'))
        .join('')}`;
}

function convertColor4b(color4b: number): ColorFormats {
    const {r, g, b} = color4bToRgb(color4b);
    const {h, s, l} = rgbToHsl(r, g, b);

    return {
        rgb: `${r}, ${g}, ${b}`,
        hsl: `hsl(${h}, ${s}%, ${l}%)`,
        hex: rgbToHex(r, g, b),
        raw: color4b
    };
}


export const themeColors: Writable<ThemeColors> = writable({
    primary: {
        rgb: '0, 7, 21',
        hsl: 'hsl(000, 72%, 1%)',
        hex: '#0721ff',
        raw: -12158977
    },
    secondary: {
        rgb: '0, 7, 21',
        hsl: 'hsl(000, 72%, 1%)',
        hex: '#0721ff',
        raw: -3457180
    }
});


export const primaryColor: Readable<ColorFormats> = derived(
    themeColors,
    ($tc) => $tc.primary
);

export const secondaryColor: Readable<ColorFormats> = derived(
    themeColors,
    ($tc) => $tc.secondary
);
export const clientName = writable<string>("")

export const primaryHex: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.primary.hex
);

export const secondaryHex: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.secondary.hex
);

export const primaryRgb: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.primary.rgb
);

export const secondaryRgb: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.secondary.rgb
);

export const primaryHsl: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.primary.hsl
);

export const secondaryHsl: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.secondary.hsl
);

async function loadInitialColors(): Promise<void> {
    try {
        const settings = await getModuleSettings('HUD');
        updateColorsFromSettings(settings);
    } catch (error) {
        console.error('Failed to load theme colors:', error);
    }
}

function updateCssVariables(colors: ThemeColors): void {
    if (typeof document === 'undefined') return;

    const primaryRgbValues = colors.primary.rgb;
    const secondaryRgbValues = colors.secondary.rgb;

    document.documentElement.style.setProperty(
        '--primary-color',
        colors.primary.hex
    );
    document.documentElement.style.setProperty(
        '--secondary-color',
        colors.secondary.hex
    );
    document.documentElement.style.setProperty(
        '--primary-color-rgb',
        primaryRgbValues
    );
    document.documentElement.style.setProperty(
        '--secondary-color-rgb',
        secondaryRgbValues
    );
    document.documentElement.style.setProperty(
        '--primary-color-hsl',
        colors.primary.hsl
    );
    document.documentElement.style.setProperty(
        '--secondary-color-hsl',
        colors.secondary.hsl
    );
}

function updateColorsFromSettings(settings: ConfigurableSetting): void {
    const Replacement = settings.value.find((v) => v.name === 'ClientName') as TextSetting
    clientName.set(Replacement?.value ?? "");
    const primaryValue = settings.value.find((v) => v.name === 'Primary')
        ?.value as number;
    const secondaryValue = settings.value.find((v) => v.name === 'Secondary')
        ?.value as number;

    if (primaryValue !== undefined && secondaryValue !== undefined) {
        const newTheme: ThemeColors = {
            primary: convertColor4b(primaryValue),
            secondary: convertColor4b(secondaryValue)
        };
        themeColors.set(newTheme);
        updateCssVariables(newTheme);
    }
}

listen("clickGuiValueChange", (e: ClickGuiValueChangeEvent) => {
    updateColorsFromSettings(e.configurable);
});

loadInitialColors().catch(console.error);
