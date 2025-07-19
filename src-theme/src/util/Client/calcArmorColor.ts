import type {ItemStack} from "../../integration/types";

export type TeamColor =
    '红' | '橙' | '黄' | '绿' |
    '青' | '蓝' | '紫' | '粉' |
    '黑' | '灰' | '白' | '棕' |
    null;

interface ColorRange {
    minHue: number;
    maxHue: number;
    minSaturation?: number;
    minValue?: number;
    maxValue?: number;
    maxSaturation?: number;
}

const TEAM_COLOR_RANGES: Record<Exclude<TeamColor, null>, ColorRange> = {
    红: {minHue: 0, maxHue: 15},
    橙: {minHue: 16, maxHue: 35},
    黄: {minHue: 36, maxHue: 60},
    绿: {minHue: 61, maxHue: 150},
    青: {minHue: 151, maxHue: 195},
    蓝: {minHue: 196, maxHue: 255},
    紫: {minHue: 256, maxHue: 285},
    粉: {minHue: 286, maxHue: 330},
    黑: {minHue: 0, maxHue: 360, minValue: 0, maxValue: 0.1},
    灰: {minHue: 0, maxHue: 360, minSaturation: 0, maxSaturation: 0.1, minValue: 0.2, maxValue: 0.8},
    白: {minHue: 0, maxHue: 360, minSaturation: 0, minValue: 0.9},
    棕: {minHue: 20, maxHue: 40, minSaturation: 0.5, minValue: 0.2, maxValue: 0.5},

};

/**
 * 将RGB颜色转换到HSV色彩空间
 */
function rgbToHsv(rgb: number): { h: number; s: number; v: number } {
    const r = ((rgb >> 16) & 0xFF) / 255;
    const g = ((rgb >> 8) & 0xFF) / 255;
    const b = (rgb & 0xFF) / 255;

    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    const delta = max - min;

    let h = 0;
    if (delta !== 0) {
        if (max === r) h = ((g - b) / delta) % 6;
        else if (max === g) h = (b - r) / delta + 2;
        else h = (r - g) / delta + 4;
        h = Math.round(h * 60);
        if (h < 0) h += 360;
    }

    const s = max === 0 ? 0 : delta / max;
    const v = max;

    return {h, s, v};
}
export function argbToRgba(color: number): string {
    const a = (color >> 24) & 0xff;
    const r = (color >> 16) & 0xff;
    const g = (color >> 8) & 0xff;
    const b = color & 0xff;
    return `rgba(${r}, ${g}, ${b}, ${a / 255})`;
}
export function detectTeamColor(rgb: number | null): TeamColor {
    if (rgb === null) return null;

    const {h, s, v} = rgbToHsv(rgb);
    if (v < 0.1) return '黑';
    if (s < 0.1 && v > 0.9) return '白';
    if (s < 0.1 && v > 0.2 && v < 0.8) return '灰';

    for (const [color, range] of Object.entries(TEAM_COLOR_RANGES)) {
        let hue = h;

        if (color === '棕' || color === '褐') {
            if (s >= (range.minSaturation || 0) &&
                v >= (range.minValue || 0) &&
                v <= (range.maxValue || 1) &&
                hue >= range.minHue && hue <= range.maxHue) {
                return color as TeamColor;
            }
            continue;
        }

        if (range.minHue > range.maxHue && (hue >= range.minHue || hue <= range.maxHue)) {
            return color as TeamColor;
        }
        if (hue >= range.minHue && hue <= range.maxHue) {
            return color as TeamColor;
        }
    }
    return null;
}

export function calcArmorColor(items: ItemStack[]): number | null {
    if (!items || items.length === 0) return null;

    let totalR = 0;
    let totalG = 0;
    let totalB = 0;
    let coloredPieces = 0;

    const dyedItems = items.filter(item => item?.hasDyedColor && item.dyedColor !== undefined);
    if (dyedItems.length === 0) return null;

    for (const item of dyedItems) {
        const color = item.dyedColor!;
        totalR += (color >> 16) & 0xFF;
        totalG += (color >> 8) & 0xFF;
        totalB += color & 0xFF;
        coloredPieces++;
    }

    const avgR = Math.round(totalR / coloredPieces);
    const avgG = Math.round(totalG / coloredPieces);
    const avgB = Math.round(totalB / coloredPieces);

    return (avgR << 16) | (avgG << 8) | avgB;
}
