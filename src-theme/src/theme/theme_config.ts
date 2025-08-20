import {listenAlways} from "../integration/ws";
import {getModuleSettings} from "../integration/rest";
import {writable} from "svelte/store";
import type {SpaceSeperatedNamesChangeEvent} from "../integration/events";

export let spaceSeperatedNames = writable(false);

export function convertToSpacedString(name: unknown): string {
    return String(name).replace(/([a-z])([A-Z])/g, "$1 $2");
}


async function updateSettings() {
    const hudSettings = await getModuleSettings("HUD");
    spaceSeperatedNames.set(hudSettings.value.find(n => n.name === "SpaceSeperatedNames")?.value as boolean ?? true);
}

listenAlways("spaceSeperatedNamesChange", (e: SpaceSeperatedNamesChangeEvent) => {
    spaceSeperatedNames.set(e.value);
});
updateSettings();
