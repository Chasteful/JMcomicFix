<script lang="ts">
    import {listen} from "../../../integration/ws";
    import {fly} from "svelte/transition";
    import {expoInOut} from "svelte/easing";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import type {PlayerData, Scoreboard} from "../../../integration/types";
    import type {ClientPlayerDataEvent} from "../../../integration/events";

    import Modules from "./arrayList/Modules.svelte";

    let scoreboard: Scoreboard | null = null;

    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        const playerData: PlayerData = e.playerData;
        scoreboard = playerData.scoreboard;
    });

</script>
<div class="combined-container">

    <div class="arraylist-section" id="arraylist"
         transition:fly|global={{duration: 500, y: -50, easing: expoInOut}}>
        <Modules/>
    </div>

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
    top: 0;
    right: 0;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    position: absolute;
    gap: 5px;

  }

  .arraylist-section {
    border-radius: 0;
    overflow: visible;
    margin-bottom: 5px;
    margin-left: auto;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    transform: translateZ(0);
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
    font-family: 'Alibaba', sans-serif;


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
