import {writable, type Writable} from "svelte/store";

export const showGrid: Writable<boolean> = writable(false);

export const snappingEnabled: Writable<boolean> = writable(true);

export const gridSize: Writable<number> = writable(10);

export const ScaleFactor: Writable<number> = writable(100);
