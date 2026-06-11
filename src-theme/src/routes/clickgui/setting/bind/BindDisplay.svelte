<script lang="ts">
    import type { BindModifier } from "../../../../integration/types";
    import { os } from "../../clickgui_store";
    import { getPrintableKeyName } from "../../../../integration/rest";
    import { UNKNOWN_KEY } from "../../../../util/utils";

    export let boundKey: string | undefined;
    export let modifiers: Iterable<BindModifier> = [];
    export let literal: boolean = false;

    let printableKeyName: string | undefined;

    const formatKeyName = (key: string | undefined) => {
        if (!key) return key;
        const mouseButtonMap: Record<string, string> = {
            'Left Button': 'LMB', 'Right Button': 'RMB', 'Middle Button': 'MMB',
            'Button 4': 'MB4', 'Button 5': 'MB5', 'Left Control': 'L Ctrl',
            'Right Control': 'R Ctrl', 'Left Shift': 'L Shift', 'Right Shift': 'R Shift',
            'Left Alt': 'L Alt', 'Right Alt': 'R Alt', 'Left Win': 'Win', 'Caps Lock': 'Caps'
        };
        return mouseButtonMap[key] || key;
    };

    $: {
        if (!literal && boundKey !== undefined && boundKey !== UNKNOWN_KEY) {
            // 清理可能包裹在按键名开头的 Right/Left 侧前缀
            let cleanedKey = boundKey.replace(/^(Right|Left)/, "").trim();
            getPrintableKeyName(cleanedKey)
                .then(printableKey => {
                    printableKeyName = printableKey.localized;
                });
        } else {
            printableKeyName = boundKey === UNKNOWN_KEY ? undefined : boundKey;
        }
    }

    // 针对不同操作系统的修饰符渲染字符
    const getRenderString = (modifier: BindModifier) => {
        // 清理修饰符字符串中的左右侧干扰项
        const cleanModifier = modifier.replace(/^(Right|Left)/, "").trim() as BindModifier;

        switch ($os) {
            case "windows":
                return cleanModifier === "Control" ? "Ctrl" : (cleanModifier === "Super" ? "\u229e" : cleanModifier);
            case "mac":
                switch (cleanModifier) {
                    case "Shift": return "\u21e7";
                    case "Control": return "^";
                    case "Alt": return "\u2325";
                    case "Super": return "\u2318";
                    default: return cleanModifier;
                }
            default:
                return cleanModifier;
        }
    };
    $: validModifiers = Array.from(modifiers || [])
        .map(mod => mod.replace(/^(Right|Left)/, "").trim())
        .filter(mod => ["Shift", "Control", "Alt", "Super"].includes(mod)) as BindModifier[];
</script>

<span class="wrapper">
    {#if printableKeyName}
        {#each validModifiers as modifier (modifier)}
            <span class="modifier">{getRenderString(modifier)}</span>
            <span class="divider">+</span>
        {/each}
        <span class="boundKey">{formatKeyName(printableKeyName)}</span>
    {:else}
        <span class="dimmed">None</span>
    {/if}
</span>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .wrapper {
    display: inline-flex;
    align-items: center;
    column-gap: 2px;
  }

  .divider {
    color: var(--text-color);
    opacity: 0.8;
    font-size: 10px;
    line-height: 1;
    font-family: monospace;
  }

  .boundKey {
    font-size: 12px;
  }

  .dimmed {
    color: var(--text-color);
    opacity: 0.6;
  }
</style>
