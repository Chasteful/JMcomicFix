export function mapToColor(value: number): string {
    if (value <= 0) {
        return 'rgb(255, 0, 0)';
    } else if (value <= 60) {
        return `rgb(255, ${Math.floor(value * 255 / 60)}, 0)`;
    } else if (value <= 120) {
        return `rgb(${Math.floor((120 - value) * 255 / 60)}, 255, 0)`;
    } else {
        return 'rgb(0, 255, 0)';
    }
}

export type ColorFormats = {
    rgb: string;
    hsl: string;
    hex: string;
    raw: number;
};

export type ThemeColors = {
    primary: ColorFormats;
    secondary: ColorFormats;
};

export function removeColorCodes(str: string): string {

    return str.replace(/§[0-9a-fA-F]/g, '');
}

export function color4bToRgb(color4b: number): { r: number; g: number; b: number } {
    const unsigned = color4b >>> 0;
    return {
        r: (unsigned >> 16) & 0xff,
        g: (unsigned >> 8) & 0xff,
        b: unsigned & 0xff
    };
}

export function rgbToHsl(r: number, g: number, b: number): { h: number; s: number; l: number } {
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

export function rgbToHex(r: number, g: number, b: number): string {
    return `#${[r, g, b]
        .map((x) => x.toString(16).padStart(2, '0'))
        .join('')}`;
}

export function convertColor4b(color4b: number): ColorFormats {
    const {r, g, b} = color4bToRgb(color4b);
    const {h, s, l} = rgbToHsl(r, g, b);

    return {
        rgb: `${r}, ${g}, ${b}`,
        hsl: `hsl(${h}, ${s}%, ${l}%)`,
        hex: rgbToHex(r, g, b),
        raw: color4b
    };
}
export function rgbaToInt(rgba: number[]): number {
    const [r, g, b, a] = rgba;
    return (
        ((a & 0xff) << 24) |
        ((r & 0xff) << 16) |
        ((g & 0xff) << 8) |
        ((b & 0xff) << 0)
    );
}

export function rgbaToHex(rgba: number[]): string {
    const [r, g, b, a] = rgba;
    const alpha = a === 255 ? "" : a.toString(16).padStart(2, "0");
    return `#${r.toString(16).padStart(2, "0")}${g
        .toString(16)
        .padStart(2, "0")}${b.toString(16).padStart(2, "0")}${alpha}`;
}

export function intToRgba(value: number): number[] {
    const red = (value >> 16) & 0xff;
    const green = (value >> 8) & 0xff;
    const blue = (value >> 0) & 0xff;
    const alpha = (value >> 24) & 0xff;
    return [red, green, blue, alpha];
}

export function hsvToRgba(h: number, s: number, v: number, a: number): string {
    h /= 360;
    s /= 100;
    v /= 100;
    let r = 0, g = 0, b = 0;
    const i = Math.floor(h * 6), f = h * 6 - i;
    const p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
    switch (i % 6) {
        case 0:
            r = v;
            g = t;
            b = p;
            break;
        case 1:
            r = q;
            g = v;
            b = p;
            break;
        case 2:
            r = p;
            g = v;
            b = t;
            break;
        case 3:
            r = p;
            g = q;
            b = v;
            break;
        case 4:
            r = t;
            g = p;
            b = v;
            break;
        case 5:
            r = v;
            g = p;
            b = q;
            break;
    }
    return `rgba(${Math.round(r * 255)}, ${Math.round(g * 255)}, ${Math.round(b * 255)}, ${a})`;
}
