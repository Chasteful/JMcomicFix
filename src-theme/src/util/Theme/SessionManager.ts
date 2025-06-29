import {listen} from "../../integration/ws";
import type {OverlayTitleEvent} from "../../integration/events";
import type {TextComponent as TTextComponent} from "../../integration/types";
import {writable} from "svelte/store";


export const wins = writable(0);
export const kills = writable(0);
export const deathCount = writable(0);

const winKeywords = [
    "win", "victory", "you win",
    "胜", "赢",
    "victoire", "gagné",
    "gewonnen",
    "vittoria",
    "¡ganaste!", "victoria",
    "победа",
    "승리",
    "勝った", "勝利",
];


function extractText(component: TTextComponent | string): string {
    if (!component) return "";
    if (typeof component === "string") return component;

    let result = component.text ?? "";
    if (Array.isArray(component.extra)) {
        for (const child of component.extra) {
            result += extractText(child);
        }
    }
    return result;
}

listen("overlayTitle", (event: OverlayTitleEvent) => {
    const fullText = `${extractText(event.title)} ${extractText(event.subtitle)}`.toLowerCase();

    if (winKeywords.some(keyword => fullText.includes(keyword.toLowerCase()))) {
        wins.update(n => n + 1);
    }
});
