/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

import { writable } from "svelte/store";
import {TimeoutManager} from "../../../../../util/Theme/TimeoutManager";


const timeoutManager = new TimeoutManager();
const userData = JSON.parse(
    localStorage.getItem('userSettings') ||
    JSON.stringify({
        username: 'Customer',
    })
);
export const randomCode = writable<string>("");

export const codeGenerator = {
    generate: (length = 6): string => {
        const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz ()[]{},;.:/?!§$%*+-АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЬЮЯ";
        const baseText = userData.username || "Customer";
        const tick = Date.now();
        let newText = "";

        for (let i = 0; i < baseText.length; i++) {
            const char = baseText[i];
            let pos = 0;
            for (let k = 0; k < alphabet.length; k++) {
                if (alphabet[k] === char) {
                    pos = (k + i + Math.floor(tick / 40)) % alphabet.length;
                    break;
                }
            }
            newText += alphabet[pos];
        }

        return newText.padEnd(length, alphabet[0]);
    },
    start: (intervalMs = 40) => {
        randomCode.set(codeGenerator.generate());
        timeoutManager.set("randomUsername", () => {
            randomCode.set(codeGenerator.generate());
        }, intervalMs);
    },
    stop: () => timeoutManager.clear("randomUsername")
};
