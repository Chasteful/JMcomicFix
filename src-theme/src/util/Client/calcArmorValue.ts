import type {ItemStack} from "../../integration/types";


export function calcArmorValue(items: ItemStack[]): number {
    if (!items || items.length === 0) return 0;

    let total = 0;

    for (const item of items) {

        if (!item || !item.identifier) continue;

        const name = item.identifier.toLowerCase();

        if (name.includes("helmet")) total += getArmorValue("helmet", item);
        else if (name.includes("chestplate")) total += getArmorValue("chestplate", item);
        else if (name.includes("leggings")) total += getArmorValue("leggings", item);
        else if (name.includes("boots")) total += getArmorValue("boots", item);
    }

    return total;
}

function getArmorValue(slot: "helmet" | "chestplate" | "leggings" | "boots", item: ItemStack): number {

    const baseValues: Record<string, Record<"helmet" | "chestplate" | "leggings" | "boots", number>> = {
        leather: {helmet: 1, chestplate: 3, leggings: 2, boots: 1},
        gold: {helmet: 2, chestplate: 5, leggings: 3, boots: 1},
        chainmail: {helmet: 2, chestplate: 5, leggings: 4, boots: 1},
        iron: {helmet: 2, chestplate: 6, leggings: 5, boots: 2},
        diamond: {helmet: 3, chestplate: 8, leggings: 6, boots: 3},
        netherite: {helmet: 3, chestplate: 8, leggings: 6, boots: 3}
    };

    const key = Object.keys(baseValues).find(k => item.identifier.toLowerCase().includes(k));
    if (!key) return 0;

    return baseValues[key][slot] || 0;
}
