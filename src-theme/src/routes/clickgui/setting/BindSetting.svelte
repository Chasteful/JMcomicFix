<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {BindSetting, ModuleSetting} from "../../../integration/types";
    import {listen} from "../../../integration/ws";
    import {getPrintableKeyName} from "../../../integration/rest";
    import type {KeyboardKeyEvent} from "../../../integration/events";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;

    const cSetting = setting as BindSetting;
    const UNKNOWN_KEY = "key.keyboard.unknown";
    const dispatch = createEventDispatcher();

    let binding = false;
    let printableKeyName = "";

    // Update printable key name reactively
    $: updatePrintableKeyName(cSetting.value.boundKey);

    async function updatePrintableKeyName(boundKey: string) {
        if (boundKey !== UNKNOWN_KEY) {
            try {
                const printableKey = await getPrintableKeyName(boundKey);
                printableKeyName = printableKey.localized;
                // Optionally store in setting if needed elsewhere
                cSetting.value.printableKeyName = printableKey.localized;
            } catch {
                printableKeyName = "?";
                cSetting.value.printableKeyName = "?";
            }
        } else {
            printableKeyName = "None";
            cSetting.value.printableKeyName = "None";
        }
    }

    // Rest of the component remains the same...
    listen("keyboardKey", async (e: KeyboardKeyEvent) => {
        if (
            e.screen === undefined ||
            !e.screen.class.startsWith("net.ccbluex.liquidbounce") ||
            !(e.screen.title === "ClickGUI" || e.screen.title === "VS-CLICKGUI")
        ) return;

        if (!binding) return;

        binding = false;

        if (e.keyCode !== 256) {
            cSetting.value.boundKey = e.key;
        } else {
            cSetting.value.boundKey = UNKNOWN_KEY;
        }

        setting = {...cSetting};
        dispatch("change");
    });

    function toggleBinding() {
        if (binding) {
            cSetting.value.boundKey = UNKNOWN_KEY;
        }
        binding = !binding;
        setting = {...cSetting};
        dispatch("change");
    }

    function toggleAction() {
        cSetting.value.action = cSetting.value.action === "Toggle" ? "Hold" : "Toggle";
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting" class:has-value={cSetting.value.boundKey !== UNKNOWN_KEY}>
    <button class="change-bind" on:click={toggleBinding}>
        {#if !binding}
            <div class="name">
                {$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}:
            </div>

            {#if cSetting.value.boundKey === UNKNOWN_KEY}
                <span class="none">None</span>
            {:else}
                <span>{printableKeyName}</span>
            {/if}
        {:else}
            <span>Press any key</span>
        {/if}
    </button>

    {#if cSetting.value.boundKey !== UNKNOWN_KEY}
        <button class="action-toggle" on:click={toggleAction}>
            <span class="fade-text">{cSetting.value.action}</span>
        </button>
    {/if}
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
    display: grid;
    grid-template-columns: 1fr;

    column-gap: 5px;

    &.has-value {
      grid-template-columns: 1fr max-content;
    }
  }

  .change-bind {
    background: rgba($mantle, 0.2);
    border: 4px solid rgba($text, 0.1);
    border-radius: 6px;
    cursor: pointer;
    padding: 4px;
    font-weight: 900;
    color: $text;
    font-size: calc(var(--font-size) - 2px);
    width: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    column-gap: 5px;

    transition: border-color 0.3s ease, box-shadow 0.3s ease;

    &:hover {
      border: 4px solid rgba($text, 0.2);
      box-shadow: 0 4px 8px rgba($text, 0.2);
    }

    .name {
      font-weight: 500;
    }

    .none {
      color: $subtext1;
      opacity: 75%;
      font-weight: 300;
    }
  }

  .action-toggle {
    background: rgba($mantle, 0.2);
    border: 4px solid rgba($text, 0.1);
    border-radius: 6px;
    padding: 4px 10px;
    font-size: var(--font-size);
    font-weight: 600;
    color: $text;
    cursor: pointer;
    width: 64px;
    overflow: hidden;
    position: relative;
    text-align: center;

    display: flex;
    align-items: center;
    justify-content: center;

    transition: border-color 0.3s ease, box-shadow 0.3s ease, transform 0.1s ease;

    &:hover {
      border-color: rgba($text, 0.2);
      box-shadow: 0 4px 8px rgba($text, 0.2);
    }

    &:active {
      transform: scale(0.95);
    }

    .fade-text {
      transition: opacity 0.3s ease;
    }
  }
</style>
