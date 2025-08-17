import {Interval} from "../timeout_utils";
import type {ConfigurableSetting, TextSetting} from "../../integration/types";
import {getModules} from "../../integration/rest";
import { writable, get} from "svelte/store";


const timeoutManager = new Interval();
const userData = JSON.parse(
    localStorage.getItem('userSettings') ||
    JSON.stringify({
        username: 'Customer',
    })
);
export const showUsername= writable<boolean>(false);
export const nameProtect = writable<string>("");
export const useGarbled = writable<boolean>(false);
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
export function NameProtectSetting(configurable: ConfigurableSetting) {
    const Replacement = configurable.value.find(v => v.name === "Replacement") as TextSetting;
    nameProtect.set(Replacement?.value ?? "Customer");
    useGarbled.set(configurable?.value.find(v => v.name === "Garbled")?.value as boolean ?? false);
}
export const checkUsernameVisibility = async (): Promise<void> => {
    const modules = await getModules();
    showUsername.set(modules.some(module =>
        module.name === "NameProtect" && !module.enabled
    ));

    if (!get(showUsername)) codeGenerator.start();
    else codeGenerator.stop();
};
