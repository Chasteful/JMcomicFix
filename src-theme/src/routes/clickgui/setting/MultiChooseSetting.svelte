<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ModuleSetting, MultiChooseSetting} from "../../../integration/types";
    import {slide} from "svelte/transition";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import {setItem} from "../../../integration/persistent_storage";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as MultiChooseSetting;
    const thisPath = `${path}.${cSetting.name}`;
    const dispatch = createEventDispatcher();

    let errorValue: string | null = null;
    let timeoutId: ReturnType<typeof setTimeout>;

    let expanded = localStorage.getItem(thisPath) === "true";
    $: setItem(thisPath, expanded.toString());

    function toggleExpanded() {
        expanded = !expanded;
    }

    function handleChange(v: string) {
        if (cSetting.value.includes(v)) {
            const filtered = cSetting.value.filter(item => item !== v);
            if (filtered.length === 0 && !cSetting.canBeNone) {
                errorValue = v;
                clearTimeout(timeoutId);
                timeoutId = setTimeout(() => (errorValue = null), 300);
                return;
            }
            cSetting.value = filtered;
        } else {
            cSetting.value = [...cSetting.value, v];
        }

        setting = {...cSetting};
        dispatch("change");
    }

    $: borderWidth = 2 + (cSetting.value.length / cSetting.choices.length) * 2;
    $: glowIntensity = Math.pow(cSetting.value.length / cSetting.choices.length, 1.8) * 0.6;
    $: borderProgress = cSetting.choices.length === 0
        ? 0
        : cSetting.value.length / cSetting.choices.length;


    $: borderOpacity = 0.1 + 0.9 * borderProgress;

    $: bgOpacity = 0.1 + borderProgress * 0.2;

    function toPct(n: number): string {
        return `${Math.round(n * 100)}%`;
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
        class="setting"
        class:has-selections={cSetting.value.length > 0}
        style="
    --selection-count: {cSetting.value.length};
    --total-choices: {cSetting.choices.length};
    --border-width: {borderWidth}px;
    --glow-intensity: {glowIntensity};
    --border-opacity-pct: {toPct(borderOpacity)};
    --bg-opacity-pct: {toPct(bgOpacity)};
  "
>
    <div class="head"
         class:expanded
         on:contextmenu|preventDefault={toggleExpanded}
    >
        <div class="title">
            {$spaceSeperatedNames
                ? convertToSpacedString(cSetting.name)
                : cSetting.name}
        </div>
        <div class="amount">
            {cSetting.value.length}/{cSetting.choices.length}
        </div>
        <ExpandArrow bind:expanded/>
    </div>

    {#if expanded}
        <div class="choices"
             in:slide|global={{ duration: 200, axis: "y" }}
             out:slide|global={{ duration: 200, axis: "y" }}
        >
            {#each cSetting.choices as choice(choice)}
                <div
                        class="choice"
                        class:active={cSetting.value.includes(choice)}
                        class:error={errorValue === choice}
                        on:click={() => handleChange(choice)}
                >
                    {$spaceSeperatedNames
                        ? convertToSpacedString(choice)
                        : choice}
                </div>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    position: relative;
    padding: 8px;
    border-radius: 8px;

    background: linear-gradient(
                    135deg in oklch,
                    color-mix(in srgb, var(--primary-color) var(--bg-opacity-pct), transparent),
                    color-mix(in srgb, var(--secondary-color) var(--bg-opacity-pct), transparent)
    );

    transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);
    box-sizing: border-box;
    overflow: hidden;
    margin: 12px;
    --border-opacity: 0.1;

    &.has-selections {
      --border-opacity: 0.2;
    }

    &::before {
      content: '';
      position: absolute;
      top: -2px;
      left: -2px;
      right: -2px;
      bottom: -2px;
      border-radius: 10px;

      background: linear-gradient(
                      135deg in oklch,
                      color-mix(in srgb, var(--primary-color) var(--border-opacity-pct), transparent),
                      color-mix(in srgb, var(--secondary-color) var(--border-opacity-pct), transparent)
      );

      z-index: 0;
      padding: 4px;
      -webkit-mask-composite: xor;
      mask-composite: exclude;
      pointer-events: none;
      opacity: var(--border-opacity);
      transition: all 0.4s ease;

      box-shadow: 0 0 calc(10px * var(--glow-intensity)) color-mix(in srgb, var(--primary-color) var(--border-opacity-pct), transparent),
      inset 0 0 calc(10px * var(--glow-intensity)) color-mix(in srgb, var(--secondary-color) var(--border-opacity-pct), transparent);
    }

    &.has-selections {
      --glow-size: calc(10px * var(--glow-intensity));
      box-shadow: 0 0 var(--glow-size) var(--border-color),
      inset 0 0 var(--glow-size) var(--border-color);
    }
  }

  .title {
    color: $text;
    font-size: var(--font-size);
    font-weight: 600;
  }

  .choice {
    color: $subtext0;
    background-color: transparent;
    border-radius: 3px;
    padding: 3px 6px;
    cursor: pointer;
    font-weight: 500;
    transition: ease 0.2s;
    overflow-wrap: anywhere;

    &:hover {
      color: rgba($text, 0.8);
    }

    &.error {
      background-color: rgba($menu-error-color, 0.1) !important;
      color: $menu-error-color !important;
      box-shadow: 0 0 8px 2px rgba($menu-error-color, 0.1);
    }

    &.active {
      background-color: rgba($accent, 0.1);
      color: $text;
      box-shadow: 0 0 8px 2px rgba($accent, 0.1);
    }
  }

  .amount {
    letter-spacing: 1px;
    font-weight: 500;
    font-size: var(--font-size);
    color: $text;
  }

  .head {
    display: grid;
    grid-template-columns: 1fr max-content max-content;
    transition: ease margin-bottom 0.2s;

    &.expanded {
      margin-bottom: 10px;
    }
  }

  .choices {
    padding: 4px 4px;
    display: flex;
    flex-wrap: wrap;
    gap: 7px;
    font-size: var(--font-size);
  }
</style>
