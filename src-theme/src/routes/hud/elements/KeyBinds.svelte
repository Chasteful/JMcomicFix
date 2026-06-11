<script lang="ts">
    import { onMount } from "svelte";
    import { getModules } from "../../../integration/rest";
    import { listen } from "../../../integration/ws";
    import { expoInOut } from "svelte/easing";
    import { fly } from "svelte/transition";
    import { convertToSpacedString, spaceSeperatedNames } from "../../../theme/theme_config";
    import type { Module } from "../../../integration/types";
    import { UNKNOWN_KEY } from "../../../util/utils";
    import BindDisplay from "../../clickgui/setting/bind/BindDisplay.svelte";
    import Line from "../common/Trims/Line.svelte";

    let { settings }: { settings: { [name: string]: any } } = $props();

    let modules: Module[] = $state([]);

    async function updateModulesWithBinds() {
        modules = (await getModules()).filter(m => m.keyBind.boundKey !== UNKNOWN_KEY);
    }

    listen("moduleToggle", updateModulesWithBinds);
    listen("valueChanged", async (e) => {
        if (e.value.name === "Bind") {
            await updateModulesWithBinds();
        }
    })

    onMount(async () => {
        await updateModulesWithBinds();
    });

</script>
    <div
            class="hud-container" style="transform: scale({settings.scale});"
            transition:fly|global={{ duration: 500, y: -50, easing: expoInOut }}
    >
        {#if settings?.title}
            <div class="title">
                <img class="icon" src="img/hud/keybinds/icon-keybinds.svg" alt="keyboard"/>
                <span>Keybindings</span>
            </div>
        {/if}

        {#if settings?.divider}
            <Line gradient={settings?.gradient}/>
        {/if}

        {#each modules as m (m.name)}
            <div class="binding-item" class:disabled={!m.enabled}>
                <span class="module-name">
                    {$spaceSeperatedNames ? convertToSpacedString(m.name) : m.name}
                </span>
                <span class="key-info">
                    [<BindDisplay boundKey={m.keyBind.boundKey} modifiers={m.keyBind.modifiers}/>]
                </span>
            </div>
        {:else}
            <div class="no-binds">No key bindings</div>
        {/each}
    </div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .hud-container {
    width: max-content;
    position: absolute;
    padding: 0.5em 0.8em;
    color: var(--text-color);
    min-width: 225px;
    font-size: 1rem;
  }

  .title {
    display: flex;
    align-items: center;
    font-size: 1em;
    font-weight: bold;
    letter-spacing: 0.1em;
    margin-bottom: 0.5em;
    color: white;

    .icon {
      margin: 0 0.3em;
      height: 1.4em;
      width: auto;
    }
  }

  .binding-item {
    display: flex;
    justify-content: space-between;
    padding: 0.2em 0;
    white-space: nowrap;
    transition: opacity 0.1s;
  }

  .disabled {
    opacity: 0.5;
    filter: grayscale(1);
  }

  .module-name {
    margin-right: 1em;
  }

  .key-info {
    color: var(--text-color);
    font-weight: 500;
    display: flex;
    align-items: center;
    column-gap: 2px;
  }

  .no-binds {
    font-style: italic;
    opacity: 0.6;
    padding: 0.2em 0;
  }
</style>
