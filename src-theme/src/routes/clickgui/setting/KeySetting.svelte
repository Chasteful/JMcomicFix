<script lang="ts">
    import type {KeySetting, ModuleSetting} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {getPrintableKeyName} from "../../../integration/rest";
    import {createEventDispatcher} from "svelte";
    import {listen} from "../../../integration/ws";
    import type {KeyboardKeyEvent} from "../../../integration/events";

    export let setting: ModuleSetting;

    const cSetting = setting as KeySetting;

    const dispatch = createEventDispatcher();
    const UNKNOWN_KEY = "key.keyboard.unknown";

    let binding = false;
    let printableKeyName = "";

    $: {
        if (cSetting.value !== UNKNOWN_KEY) {
            getPrintableKeyName(cSetting.value)
                .then(printableKey => {
                    printableKeyName = printableKey.localized;
                });
        }
    }

    async function toggleBinding() {
        if (binding) {
            cSetting.value = UNKNOWN_KEY;
        }

        binding = !binding;

        setting = {...cSetting};

        dispatch("change");
    }

    listen("keyboardKey", async (e: KeyboardKeyEvent) => {
        if (e.screen === undefined || !e.screen.class.startsWith("net.ccbluex.liquidbounce") ||
            !(e.screen.title === "ClickGUI" || e.screen.title === "VS-CLICKGUI")) {
            return;
        }

        if (!binding) {
            return;
        }

        binding = false;

        if (e.keyCode !== 256) {
            cSetting.value = e.key;
        } else {
            cSetting.value = UNKNOWN_KEY;
        }

        setting = {...cSetting};

        dispatch("change");
    });
</script>

<div class="setting">
    <button class="change-bind" on:click={toggleBinding}>
        {#if !binding}
            <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}:</div>

            {#if cSetting.value === UNKNOWN_KEY}
                <span class="none">None</span>
            {:else}
                <span>{printableKeyName}</span>
            {/if}
        {:else}
            <span>Press any key</span>
        {/if}
    </button>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
  }

  .change-bind {
    background-color: transparent;
    border: solid 2px color-mix(in srgb, var(--primary-color) 40%, transparent);
    border-radius: 3px;
    cursor: pointer;
    padding: 4px;
    font-weight: 500;
    color: $text;
    font-size: var(--font-size);
    width: 100%;
    display: flex;
    justify-content: center;
    column-gap: 5px;

    .name {
      font-weight: 500;
    }

    .none {
      color: $subtext0;
    }
  }
</style>
