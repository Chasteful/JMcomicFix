<script lang="ts">
    import {afterUpdate} from "svelte";
    import {setModuleEnabled} from "../../../../integration/rest";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";

    export let name: string;
    export let enabled: boolean;
    export let selected: boolean;

    let moduleElement: HTMLElement;

    afterUpdate(() => {
        if (moduleElement && selected) {
            moduleElement.scrollIntoView({
                behavior: "smooth",
                block: "nearest",
            });
        }
    });

    async function handleKeyDown(e: KeyboardEvent) {
        if (selected && e.key === "Enter") {
            await setModuleEnabled(name, !enabled);
        }
    }
</script>

<svelte:window on:keydown={handleKeyDown}/>

<div bind:this={moduleElement} class="module" class:enabled class:selected>
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(name) : name}</div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .module {
    font-family: "Alibaba", sans-serif;
    font-weight: 500;
    color: rgba(150, 150, 150);
    font-size: 14px;

    padding: 6px 15px 6px 10px;
    transition: ease color 0.2s;

    .name {
      transition: ease transform 0.2s;
    }

    &.selected {
      color: $text;
      border-radius: 6px;
      background-color: rgba($base, 0.36);
    }

    &.enabled .name, &.selected.enabled .name {
      background-clip: text;
      background: linear-gradient(to right, var(--primary-color) 0%, var(--secondary-color) 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      text-shadow: 0 0 8px color-mix(in srgb, var(--secondary-color) 30%, transparent);
    }
  }
</style>
