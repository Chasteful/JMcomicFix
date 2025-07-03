import {listenAlways} from "../integration/ws";
import {getModuleSettings} from "../integration/rest";
import {writable} from "svelte/store";
import type {SpaceSeperatedNamesChangeEvent} from "../integration/events";

export let spaceSeperatedNames = writable(false);

export function convertToSpacedString(name: unknown): string {
    const regex = /[A-Z]?[a-z]+|[0-9]+|[A-Z]+(?![a-z])/g;
    // @ts-ignore
    return (name.match(regex) as string[]).join(" ");
}

async function updateSettings() {
    const hudSettings = await getModuleSettings("HUD");
    spaceSeperatedNames.set(hudSettings.value.find(n => n.name === "SpaceSeperatedNames")?.value as boolean ?? true);
}

listenAlways("spaceSeperatedNamesChange", (e: SpaceSeperatedNamesChangeEvent) => {
    spaceSeperatedNames.set(e.value);
});
updateSettings();
