import { writable, derived, type Writable, type Readable } from 'svelte/store';
import { getModuleSettings } from '../../integration/rest';
import { listen } from '../../integration/ws';
import type { ConfigurableSetting, MultiChooseSetting, TextSetting } from '../../integration/types';
import type { ClickGuiValueChangeEvent } from "../../integration/events";
import { type ColorFormats, convertColor4b, type ThemeColors } from '../color_utils';

export const ArraylistRenderSettings = writable<Set<string>>(new Set());

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
    },
    shadowColor: {
        rgb: '20, 20, 20',
        hsl: 'hsl(0, 0%, 8%)',
        hex: '#141414',
        raw: -14474460
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

export const shadowColor: Readable<ColorFormats> = derived(
    themeColors,
    ($tc) => $tc.shadowColor
);

export const clientName = writable<string>("");
export const scoreboardIP = writable<string>("");
export const shadow = writable<boolean>(false);
export const vignette = writable<boolean>(false);
export const hudScaleFactor: Writable<number> = writable(1);
export const shadowStrength: Writable<number> = writable(16);
export const borderRadius: Writable<number> = writable(6);

export const primaryHex: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.primary.hex
);

export const secondaryHex: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.secondary.hex
);

export const shadowHex: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.shadowColor.hex
);

export const primaryRgb: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.primary.rgb
);

export const secondaryRgb: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.secondary.rgb
);

export const shadowRgb: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.shadowColor.rgb
);

export const primaryHsl: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.primary.hsl
);

export const secondaryHsl: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.secondary.hsl
);

export const shadowHsl: Readable<string> = derived(
    themeColors,
    ($tc) => $tc.shadowColor.hsl
);

function updateCssVariables(colors: ThemeColors, shadowStrengthValue: number, borderRadiusValue: number): void {
    if (typeof document === 'undefined') return;

    const primaryRgbValues = colors.primary.rgb;
    const secondaryRgbValues = colors.secondary.rgb;
    const shadowRgbValues = colors.shadowColor.rgb;

    const shadowY = shadowStrengthValue / 4;
    const insetBlur = (shadowY + shadowStrengthValue) / 2;

    document.documentElement.style.setProperty('--primary-color', colors.primary.hex);
    document.documentElement.style.setProperty('--secondary-color', colors.secondary.hex);
    document.documentElement.style.setProperty('--shadow-color', colors.shadowColor.hex);
    document.documentElement.style.setProperty('--primary-color-rgb', primaryRgbValues);
    document.documentElement.style.setProperty('--secondary-color-rgb', secondaryRgbValues);
    document.documentElement.style.setProperty('--shadow-color-rgb', shadowRgbValues);
    document.documentElement.style.setProperty('--primary-color-hsl', colors.primary.hsl);
    document.documentElement.style.setProperty('--secondary-color-hsl', colors.secondary.hsl);
    document.documentElement.style.setProperty('--shadow-color-hsl', colors.shadowColor.hsl);
    document.documentElement.style.setProperty('--border-radius', `${borderRadiusValue}px`);
    document.documentElement.style.setProperty(
        '--box-shadow',
        `0 ${shadowY}px ${shadowStrengthValue}px color-mix(in srgb, rgb(${shadowRgbValues}) 60%, transparent), inset 0 0 ${insetBlur}px rgba(255, 255, 255, 0.05)`
    );
}

async function loadInitialColors(): Promise<void> {
    try {
        const settings = await getModuleSettings('HUD');
        updateColorsFromSettings(settings);
    } catch (error) {
        console.error('Failed to load theme colors:', error);
    }
}

function updateColorsFromSettings(settings: ConfigurableSetting): void {
    let shadowStrengthSetting: number | undefined;
    let borderRadiusSetting: number | undefined;
    const customization = settings.value.find(v => v.name === "Customization") as ConfigurableSetting;
    const shadowSetting = settings.value.find((v) => v.name === 'Shadow')?.value as boolean;
    shadow.set(shadowSetting);


    if (!shadowSetting) {
        shadowStrength.set(0);
        shadowStrengthSetting = 0;
    } else {
        shadowStrengthSetting = customization.value.find(v => v.name === "ShadowStrength")?.value as number;
        shadowStrength.set(shadowStrengthSetting ?? 16);
    }

    const vignetteSetting = settings.value.find((v) => v.name === 'Vignette')?.value as boolean;
    vignette.set(vignetteSetting);


    const renderSetting = customization.value.find(v => v.name === 'ArraylistPrefixRender') as MultiChooseSetting;
    const primaryValue = customization.value.find((v) => v.name === 'Primary')?.value as number;
    const secondaryValue = customization.value.find((v) => v.name === 'Secondary')?.value as number;
    const shadowColorValue = customization.value.find((v) => v.name === 'Shadow')?.value as number;
    const scaleFactor = customization.value.find(v => v.name === "ScaleFactor")?.value as number ?? 1;
    hudScaleFactor.set(scaleFactor);
    const clientNameSetting = customization.value.find(v => v.name === "ClientName") as TextSetting;
    clientName.set(clientNameSetting?.value ?? "");

    const scoreboardIPSetting = customization.value.find(v => v.name === "ScoreboardIP") as TextSetting;
    scoreboardIP.set(scoreboardIPSetting?.value ?? "");

    borderRadiusSetting = customization.value.find(v => v.name === "BorderRadius")?.value as number;
    borderRadius.set(borderRadiusSetting ?? 6);

    ArraylistRenderSettings.set(new Set(renderSetting.value.map(v => v.toLowerCase())));

    if (primaryValue !== undefined && secondaryValue !== undefined && shadowColorValue !== undefined) {
        const newTheme: ThemeColors = {
            primary: convertColor4b(primaryValue),
            secondary: convertColor4b(secondaryValue),
            shadowColor: convertColor4b(shadowColorValue)
        };
        themeColors.set(newTheme);
        updateCssVariables(
            newTheme,
            shadowStrengthSetting ?? 16,
            borderRadiusSetting ?? 6
        );
    }
}

listen("hudValueChange", (e: ClickGuiValueChangeEvent) => {
    updateColorsFromSettings(e.configurable);
});

loadInitialColors().catch(console.error);
