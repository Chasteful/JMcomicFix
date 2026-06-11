<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {PlayerData, Scoreboard} from "../../../integration/types";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import {expoInOut} from "svelte/easing";
    import { fly } from "svelte/transition";
    import {scoreboardIP} from "../../../theme/theme_manager";
    import GradientAnimatedText from "../common/FontRenderer/GradientAnimatedText.svelte";

    export let settings: { [name: string]: any };

    const cSettings = settings as HudScoreboardSettings;

    let scoreboard: Scoreboard | null = null;

    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        const playerData: PlayerData = e.playerData;
        scoreboard = playerData.scoreboard;
    });
</script>

{#if scoreboard}
    <div class="scoreboard hud-container" style="transform: scale({settings.scale});"
         transition:fly|global={{duration: 500, x: 50, easing: expoInOut}}>
        {#if scoreboard.header && cSettings.show.includes('Header')}
            <div class="header">
                <TextComponent fontSize={14} allowPreformatting={true} textComponent={scoreboard.header}/>
            </div>
        {/if}
        <div class="entries">
            {#each scoreboard.entries as {name, score}, i}
                <div class="row">
                    {#if cSettings.show.includes('Name')}
                        {#if i === scoreboard.entries.length - 1 && $scoreboardIP}
                            <div class="ip-address">
                                <GradientAnimatedText text={$scoreboardIP}/>
                            </div>
                        {:else}
                            <TextComponent fontSize={16} allowPreformatting={true} textComponent={name}/>
                        {/if}
                    {/if}
                    {#if cSettings.show.includes('Score')}
                        <TextComponent fontSize={14} allowPreformatting={true} textComponent={score}/>
                    {/if}
                </div>
            {/each}
        </div>
    </div>
{/if}

<style lang="scss">
  @use "sass:color";
  @use "../../../colors.scss" as *;

  .scoreboard {
    position: relative;
    display: inline-block;
    width: max-content;
    max-width: 240px;
    right: 0;
    transition: width 0.2s ease;
    transform: translateX(0);
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
    align-items: center;
  }

  .header {
    text-shadow: 0 0 5px rgba(255,255,255, 0.4),
    1px 1px 1px rgba(color.scale(black, $lightness: -20%), 0.6);
    text-align: center;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 10px;
    white-space: nowrap;
    box-sizing: border-box;
  }

  .ip-address {
    text-align: left;
    flex-grow: 1;
  }
</style>
