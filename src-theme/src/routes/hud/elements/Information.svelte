<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import type {ClientInfo, PlayerData} from "../../../integration/types";
    import {tweened} from 'svelte/motion';
    import {cubicOut, expoInOut} from 'svelte/easing';
    import {getClientInfo} from "../../../integration/rest";
    import {onMount} from "svelte";
    import {fly} from "svelte/transition"

    let clientInfo: ClientInfo | null = null;
    let playerData: PlayerData | null = {
        position: {x: 0, y: 0, z: 0},
    } as PlayerData;
    let lastX = 0;
    let lastZ = 0;

    const bps = tweened(0, {duration: 150, easing: cubicOut});
    const xPos = tweened(0, {duration: 300, easing: cubicOut});
    const yPos = tweened(0, {duration: 300, easing: cubicOut});
    const zPos = tweened(0, {duration: 300, easing: cubicOut});

    function roundToDecimal(value: number, decimal: number) {
        const rounded = Math.round(value * Math.pow(10, decimal)) / Math.pow(10, decimal);
        return rounded.toFixed(decimal);
    }

    function formatCoordinate(value: number): string {
        return value.toFixed(1);
    }

    function getBPS(
        lastX: number,
        currentX: number,
        lastZ: number,
        currentZ: number,
        tickrate: number
    ): number {
        const deltaX = currentX - lastX;
        const deltaZ = currentZ - lastZ;
        const distanceMoved = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        return distanceMoved * tickrate;
    }

    async function updateClientInfo() {
        clientInfo = await getClientInfo();
    }

    listen("clientPlayerData", ((event: ClientPlayerDataEvent) => {
        if (playerData) {
            lastX = playerData.position.x;
            lastZ = playerData.position.z;
        }
        playerData = event.playerData;
        if (playerData) {
            xPos.set(playerData.position.x);
            yPos.set(playerData.position.y);
            zPos.set(playerData.position.z);
            const calculatedBps = getBPS(lastX, playerData.position.x, lastZ, playerData.position.z, 20);
            if (calculatedBps <= 200) {
                bps.set(calculatedBps);
            }
        }
    }));


    onMount(async () => {
        await updateClientInfo();
        setInterval(async () => {
            await updateClientInfo();
        }, 1000);
    });

</script>
<style lang="scss">
  @use "../../../colors.scss" as *;

  .stats-container {
    display: inline-flex;
    flex-direction: column;
    gap: 4px;
    min-width: max-content;
    font-family: 'Alibaba', sans-serif;
  }

  .stat {
    display: flex;
    align-items: center;
    gap: 6px;
    white-space: nowrap;
  }

  .value, .label {
    font-size: 20px;
    text-align: right;
    color: #CCCCCC;
    opacity: 0.85;
    text-shadow: 0 0 3px rgba(204, 204, 204, 0.9);
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    filter: drop-shadow(0 2px 10px rgba(255, 255, 255, 0.3));
  }

  .value {
    position: relative;

    &::after {
      content: attr(data-value);
      position: absolute;
      top: 0;
      left: 0;
      color: transparent;
      text-shadow: 0 0 3px rgba(204, 204, 204, 0.9);
      z-index: -1;
    }
  }
</style>

<div class="stats-container" transition:fly={{duration: 700, x: -50, easing: expoInOut}}>
    {#if clientInfo}
        <div class="stat">
            <span class="label">FPS:&nbsp;</span>
            <span class="value">{clientInfo.fps}</span>
        </div>
    {/if}
    {#if playerData}
        {@const bpsValue = roundToDecimal($bps, 2).toString().padStart(6, " ")}
        <div class="stat">
            <span class="label">BPS:&nbsp;</span>
            <span class="value">{bpsValue}</span>
        </div>
        {@const x = formatCoordinate($xPos)}
        {@const y = formatCoordinate($yPos)}
        {@const z = formatCoordinate($zPos)}
        <div class="stat">
            <span class="label">XYZ:&nbsp;</span>
            <span class="value">{x}, {y}, {z}</span>
        </div>
    {/if}
</div>
