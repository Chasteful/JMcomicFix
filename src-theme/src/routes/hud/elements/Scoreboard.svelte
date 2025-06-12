<script lang="ts">
    import {onDestroy, onMount, tick} from "svelte";
  import type { Module } from "../../../integration/types";
  import { getModules } from "../../../integration/rest";
  import { listen } from "../../../integration/ws";
  import { getTextWidth } from "../../../integration/text_measurement";
  import { flip } from "svelte/animate";
  import { fly } from "svelte/transition";
  import { convertToSpacedString, spaceSeperatedNames } from "../../../theme/theme_config";
  import { getPrefixAsync } from "../../../theme/arraylist";
  import { expoInOut } from "svelte/easing";
  import TextComponent from "../../menu/common/TextComponent.svelte";
  import type { PlayerData, Scoreboard } from "../../../integration/types";
  import type {ClientPlayerDataEvent} from "../../../integration/events";
    import {
        subscribeColors,
        arraylistGradient,
        destroyGradient
    } from '../../../theme/arraylist';

    let scoreboard: Scoreboard | null = null;
    let enabledModules: Array<Module & { prefix: string; formattedName: string; width: number }> = [];
    let intervalId: number;
    let unsubs: [() => void, () => void];
    async function updateEnabledModules() {
        await document.fonts.load("500 16px 'Product Sans'");
        const modules = await getModules();
        const visible = modules.filter((m) => m.enabled && !m.hidden);
        const prefixMap = new Map<string, string>();

        await Promise.all(
            visible.map(async (m) => {
                const prefix = await getPrefixAsync(m.name);
                prefixMap.set(m.name, prefix);
            })
        );

        const arr = visible.map((m) => {
            const formattedName = $spaceSeperatedNames
                ? convertToSpacedString(m.name)
                : m.name;
            const prefix = prefixMap.get(m.name) || '';
            const fullName = `${formattedName} ${prefix}`;
            const width = getTextWidth(
                fullName,
                "500 16px 'Product Sans', system-ui, sans-serif"
            );
            return { ...m, formattedName, prefix, width };
        });

        arr.sort((a, b) => b.width - a.width);
        enabledModules = arr;
        await tick();
    }

    onMount(async () => {

        await updateEnabledModules();

        spaceSeperatedNames.subscribe(() => updateEnabledModules());

        unsubs = subscribeColors();

        setTimeout(() => arraylistGradient(), 50);

        intervalId = window.setInterval(arraylistGradient, 50);
    });

    onDestroy(() => {
        destroyGradient(intervalId, unsubs);
    });
    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        const playerData: PlayerData = e.playerData;
        scoreboard = playerData.scoreboard;
    });
    listen('moduleToggle', () => updateEnabledModules());
    listen('refreshArrayList', () => updateEnabledModules());
</script>
<div class="combined-container">
  {#if enabledModules.length > 0}
      <div class="arraylist-section" id="arraylist" transition:fly|global={{duration: 500, y: -50, easing: expoInOut}}>
          {#each enabledModules as { formattedName, prefix, name } (name)}
              <div class="module" id="module-name" animate:flip={{ duration: 200 }} in:fly={{ x: 50, duration: 200 }} >
                {formattedName}{#if prefix}&nbsp;<span class="prefix">{prefix}</span>{/if}
                  <span class="side-bar" id="side-bar"></span>
              </div>
          {/each}
      </div>
  {/if}
  {#if scoreboard}
      <div class="scoreboard-section" transition:fly|global={{duration: 500, x: 50, easing: expoInOut}}>
          {#if scoreboard.header}
              <div class="header">
                  <TextComponent fontSize={18} allowPreformatting={true} textComponent={scoreboard.header}/>
              </div>
          {/if}
          <div class="entries">
              {#each scoreboard.entries as {name}}
                  <div class="row">
                      <TextComponent fontSize={16} allowPreformatting={true} textComponent={name}
                      />
                  </div>
              {/each}
          </div>
      </div>
  {/if}
</div>

<style lang="scss">
  @import "../../../colors.scss";

  :root {
    --primary-color-rgb: var(--primary-color-rgb);
    --secondary-color-rgb: var(--secondary-color-rgb);
  }
  .combined-container {
    position: fixed;
    top: 10px;
    right: 10px;
    display: flex;
    flex-direction: column;
    gap: 5px;

}
  .arraylist-section {
      border-radius: 0;
      overflow: visible;
      margin-bottom: 5px;
      margin-left: auto;

  }
  .module {
      position: relative;
      display: flex;
      align-items: center;
      background-color: rgba(0, 0, 0, 0.1);
       box-shadow:
            -10px 0px 20px rgba(0, 0, 0, 0.15),
            10px 0px 20px rgba(0, 0, 0, 0.15);
      color: var(--primary-color);
      text-shadow: var(--primary-color);
      font-family: 'Product Sans', system-ui, -apple-system, sans-serif;
      font-size: 16px;
      padding: 2px 6px;
      width: max-content;
      font-weight: 500;
      margin-left: auto;
      text-transform: capitalize;
      border-radius:3px;
      &::before {
          content: '';
          position: absolute;
          inset: 0;
          border-radius: inherit;
          z-index: -1;
      }
    &:first-child {
      border-radius: 3px 3px 0 0;
      &::after {
        border-radius: 3px 3px 0 0;
      }
    }
    &:last-child {
      border-radius: 0 0 3px 3px;
      &::after {
        border-radius: 0 0 3px 3px;
      }
    }
  }
  .prefix {
      color: #AAAAAA;
      text-shadow: 0 0 3px rgba(#AAAAAA,0.9);
  }
  .side-bar {
      position: absolute;
      right: -3px;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: calc(100% - 6px);
      background-color: currentColor;
      border-radius: 2px;
      box-shadow: 0 0 5px currentColor;
      pointer-events: none;
  }
  .scoreboard-section {
    position: relative;
    background: linear-gradient(
                    90deg,
                    transparent 0%,
                    rgba($scoreboard-base-color, 0.3) 100%);
    display: inline-block;
    right: 0;
    transition: width 0.2s ease;
    transform: translateX(0);
    margin-left: auto;
    font-family:  'Alibaba', sans-serif;


    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(
                      90deg,
                      rgba($scoreboard-base-color, 0.1) 0%,
                      rgba($scoreboard-base-color, 0.4) 100%);
      z-index: -1;
      filter: blur(10px);
      opacity: 0.7;
      transform: scale(0.95) translateY(5px);
    }
  }

  .entries {
    padding: 10px;
    position: relative;
    z-index: 1;
  }

  .row {
    display: flex;
    column-gap: 15px;
    gap: 4px;
    justify-content: space-between;
    white-space: nowrap;
  }

  .header {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 10px;
    text-shadow: 0 0 5px rgba($text-color, 0.4),
    1px 1px 1px rgba(darken($scoreboard-base-color, 20%), 0.6);
    text-align: center;
    white-space: nowrap;
    box-sizing: border-box;
  }
</style>
