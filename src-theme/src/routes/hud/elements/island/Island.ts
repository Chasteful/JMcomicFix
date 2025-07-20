import {writable} from 'svelte/store';
import type {TextComponent} from '../../../../integration/types';

export let totemCount = writable<number>(0);
export const blockCount = writable<number | undefined>(undefined);
export let emptySlotCount = writable(36);
export const armorValue = writable<number | undefined>(undefined);
export const targetId = writable<string | null>(null);
export const armorThreshold = 3;

interface ArmorSlot {
    identifier: string;
    durability: number;
    maxDurability: number;
    displayName: string | TextComponent;
}

export const armorDurabilityStore = writable<{
    helmet: ArmorSlot | null;
    chestplate: ArmorSlot | null;
    leggings: ArmorSlot | null;
    boots: ArmorSlot | null;
}>({
    helmet: null,
    chestplate: null,
    leggings: null,
    boots: null
});
export const DURABILITY_RECOVERY = 0.15;
export const DURABILITY_THRESHOLD = 0.1;
