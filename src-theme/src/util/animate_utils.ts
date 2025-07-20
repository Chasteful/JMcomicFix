import {elasticOut, quintOut} from "svelte/easing";

export function easeOutQuad(t: number) {
    return t * (2 - t);
}

export function easeInQuad(t: number) {
    return t * t;
}

export function easeInBack(t: number) {
    const c1 = 1.5;
    const c3 = c1 + 1;
    return c3 * t * t * t - c1 * t * t;
}


export function composeStyle(styles: string[], origin = 'center') {
    return styles.join('\n') + `\ntransform-origin: ${origin};`;
}


export function fadeBase(invert: boolean) {
    return function (node: Element, {delay = 0, duration = 200} = {}) {
        return {
            delay,
            duration,
            css: (t: number) => {
                const progress = invert ? 1 - t : t;
                const eased = easeInBack(progress);
                const scale = invert
                    ? 1 - eased * 0.5
                    : 1 - (1 - t) * 0.5;
                const opacity = invert ? 1 - eased : eased;

                return composeStyle([
                    `transform: scale(${scale});`,
                    `opacity: ${opacity};`,
                    `transition-timing-function: cubic-bezier(0.68, -0.55, 0.27, 1.55);`
                ]);
            }
        };
    };
}

export const FadeIn = fadeBase(false);
export const FadeOut = fadeBase(true);

export function popBase(
    node: Element,
    {delay = 0, duration = 400},
    compute: (t: number, u: number, baseTransform: string, opacity: number) => string
) {
    const style = getComputedStyle(node);
    const baseTransform = style.transform === 'none' ? '' : style.transform;
    const opacity = +style.opacity;
    return {
        delay,
        duration,
        css: (t: number) => compute(t, 1 - t, baseTransform, opacity)
    };
}

export function popIn(node: Element, opts = {}) {
    return popBase(node, opts, (t, _u, baseTransform, opacity) => {
        const scale = elasticOut(t) * 0.2 + 0.8;
        const fade = quintOut(t) * opacity;
        return `
            transform: ${baseTransform} scale(${scale});
            opacity: ${fade};
        `;
    });
}

export function popOut(node: Element, opts = {}) {
    return popBase(node, {duration: 1000, ...opts}, (t, u, baseTransform, opacity) => {
        const progress = u;
        const scaleProgress = progress < 0.5
            ? easeOutQuad(progress * 2)
            : easeInQuad(1 - (progress - 0.5) * 2);
        const scale = 1 + scaleProgress * 0.2;
        const opacityEased = progress < 0.5
            ? 1
            : easeInQuad(1 - (progress - 0.5) * 2);
        return `
            transform: ${baseTransform} scale(${scale});
            opacity: ${opacity * opacityEased};
        `;
    });
}

export function popFly(node: Element, {delay = 0, duration = 600} = {}) {
    return {
        delay,
        duration,
        easing: elasticOut,
        css: (t: number, u: number) => `
            transform:
                scale(${0.5 + 0.5 * t * t})
                translateY(${Math.sin(u * Math.PI) * 30}px)
                rotate(${(1 - t) * 8}deg);
            opacity: ${t * t};
        `
    };
}

export function popScale(node: Element, {delay = 0, duration = 300} = {}) {
    return {
        delay,
        duration,
        css: (t: number) => {
            const eased = easeInBack(1 - t);
            return composeStyle([
                `transform: translateY(${eased * 100}px) scale(${1 - eased * 0.5});`,
                `opacity: ${1 - eased};`,
                `transition-timing-function: cubic-bezier(0.68, -0.55, 0.27, 1.55);`
            ], 'top center');
        }
    };
}
export function shrinkOut(node: Element, {delay = 0, duration = 300} = {}) {
    return {
        delay,
        duration,
        css: (t: number) => {
            const eased = easeInBack(1 - t);
            return `
        transform: 
        scale(${1 - eased * 0.5});
        opacity: ${1 - eased};
        transition-timing-function: cubic-bezier(0.68, -0.55, 0.27, 1.55);
        transform-origin: top center;
      `;
        }
    };
}
export function springTransition(node: HTMLElement, params: { delay?: number }) {
    return {
        delay: params.delay || 0,
        duration: 400,
        css: (t: number) => {
            const eased = 1 - Math.pow(1 - t, 3);
            return `
          transform: scale(${eased});
          opacity: ${eased};
        `;
        }
    };
}
