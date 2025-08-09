import {writable, derived, type Writable, type Readable} from 'svelte/store';
import {getModuleSettings} from '../../integration/rest';
import {listen} from '../../integration/ws';
import type {ConfigurableSetting, MultiChooseSetting, TextSetting} from '../../integration/types';
import type {ClickGuiValueChangeEvent} from "../../integration/events";
import {type ColorFormats, convertColor4b, type ThemeColors} from '../color_utils';

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
export const scoreboardIP = writable<string>("")
export const vignette=writable<boolean>(false);
export const hudScaleFactor: Writable<number> = writable(100);

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
    const Replacement1 = settings.value.find((v) => v.name === 'ClientName') as TextSetting
        clientName.set(Replacement1?.value ?? "");
    const Replacement2 = settings.value.find((v) => v.name === 'ScoreboardIP') as TextSetting
        scoreboardIP.set(Replacement2?.value ?? "");
    const Replacement3 = settings.value.find((v) => v.name === 'Vignette')?.value as boolean;
    vignette.set(Replacement3);
    const primaryValue = settings.value.find((v) => v.name === 'Primary')
        ?.value as number;
    const secondaryValue = settings.value.find((v) => v.name === 'Secondary')
        ?.value as number;
    const renderSetting = settings.value.find(v => v.name === 'ArraylistPrefixRender') as MultiChooseSetting;

    hudScaleFactor.set(settings.value.find(v => v.name === "ScaleFactor")?.value as number ?? 1);

    if (renderSetting) {
        ArraylistRenderSettings.set(new Set(renderSetting.value.map(v => v.toLowerCase())));
    }
    if (primaryValue !== undefined && secondaryValue !== undefined) {
        const newTheme: ThemeColors = {
            primary: convertColor4b(primaryValue),
            secondary: convertColor4b(secondaryValue)
        };
        themeColors.set(newTheme);
        updateCssVariables(newTheme);
    }
}

listen("hudValueChange", (e: ClickGuiValueChangeEvent) => {
    updateColorsFromSettings(e.configurable);
});

loadInitialColors().catch(console.error);

