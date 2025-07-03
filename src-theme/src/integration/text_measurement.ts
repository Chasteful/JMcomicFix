const canvas = document.createElement("canvas");
const context = canvas.getContext("2d")!!;

export function getTextWidth(text: string, font: string) {

    context.save();

    context.font = font;
    context.textBaseline = 'top';

    const metrics = context.measureText(text);


    context.restore();
    return metrics.width;
}
