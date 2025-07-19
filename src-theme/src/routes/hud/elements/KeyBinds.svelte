<script lang="ts">
    import {onMount} from "svelte";
    import type {ModuleSetting, BindSetting} from "../../../integration/types";
    import {getModules, getModuleSettings, getPrintableKeyName} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import {expoInOut} from "svelte/easing";
    import {fly} from "svelte/transition";
    import Line from "../common/Line.svelte";

    type BindInfo = {
        moduleName: string;
        enabled: boolean;
        keyName: string;
        action: string;
    };

    let bindings: BindInfo[] = [];
    let loaded = false;
    const keyNameCache: Record<string, string> = {};

    async function getCachedPrintableKeyName(key: string): Promise<string> {
        if (keyNameCache[key]) return keyNameCache[key];
        try {
            const res = await getPrintableKeyName(key);
            keyNameCache[key] = res.localized;
            return res.localized;
        } catch {
            return key;
        }
    }

    function isBindSetting(setting: ModuleSetting): setting is BindSetting {
        return setting.valueType === "BIND" &&
            typeof (setting as BindSetting).value?.boundKey === "string";
    }

    async function updateBindings() {
        try {
            const modules = await getModules();
            const results: BindInfo[] = [];
            const UNKNOWN_KEY = "key.keyboard.unknown";

            await Promise.all(modules.map(async (module) => {
                if (module.hidden) return;
                const settings = await getModuleSettings(module.name);
                const bindSetting = settings.value.find(isBindSetting);

                if (bindSetting && bindSetting.value.boundKey !== UNKNOWN_KEY) {
                    const printable = await getCachedPrintableKeyName(bindSetting.value.boundKey);

                    results.push({
                        moduleName: module.name,
                        enabled: module.enabled,
                        keyName: printable,
                        action: bindSetting.value.action
                    });
                }
            }));

            bindings = results.sort((a, b) => a.moduleName.localeCompare(b.moduleName));
        } catch (error) {
            bindings = [];
        }
    }

    onMount(async () => {
        await updateBindings();
        loaded = true;
    });

    listen("moduleToggle", updateBindings);
</script>
{#if loaded}
    <div class="hud-container hud-container" transition:fly|global={{duration: 500, y: -50, easing: expoInOut}}>
        <div class="title">
            <img class="icon" src="img/hud/keybinds/keyboard.svg" alt="keyboard"/>
            <span>Keybindings</span>
        </div>
        <Line/>
        {#each bindings as binding (binding.moduleName)}
            <div class:disabled={!binding.enabled} class="binding-item">
                <span class="module-name">{binding.moduleName}</span>
                <span class="key-info">[{binding.keyName}]</span>
            </div>
        {/each}
    </div>
{/if}
<style lang="scss">
  @use "../../../colors.scss" as *;

  .hud-container {
    position: absolute;
    padding: 0.5em 0.8em;
    color: $text-color;
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
    color: $text-color;
    font-weight: 500;
  }

</style>
