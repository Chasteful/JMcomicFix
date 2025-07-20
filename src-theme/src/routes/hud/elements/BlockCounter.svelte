<script lang="ts">
    import {REST_BASE} from "../../../integration/host";
    import type {PlayerData} from "../../../integration/types";
    import type {BlockCountChangeEvent, ClientPlayerDataEvent} from "../../../integration/events";
    import {listen} from "../../../integration/ws";
    import {getPlayerData} from "../../../integration/rest";
    import {onMount, tick} from "svelte";
    import {FadeOut} from "../../../util/animate_utils";
    import {blockCount} from './island/Island';
    import {tweened} from "svelte/motion";
    import {cubicOut} from "svelte/easing";

    let playerData: PlayerData | null = null;
    let count: number | undefined;
    let contentElement: HTMLDivElement;

    const maxWidth = tweened(0, { duration: 150, easing: cubicOut });

    listen("blockCountChange", (e: BlockCountChangeEvent) => {
        count = e.count;
        blockCount.set(e.count);
    });

    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        playerData = e.playerData;
    });

    onMount(async () => {
        playerData = await getPlayerData();

        await tick();
        if (contentElement) {
            const style = getComputedStyle(contentElement);
            const full =
                contentElement.scrollWidth +
                parseFloat(style.paddingLeft) +
                parseFloat(style.paddingRight);
            await maxWidth.set(full);
        }
    });


    $: if (contentElement) {
        if (count !== undefined) {
            const style = getComputedStyle(contentElement);
            const full =
                contentElement.scrollWidth +
                parseFloat(style.paddingLeft) +
                parseFloat(style.paddingRight);
            maxWidth.set(full);
        } else {
            maxWidth.set(0);
        }
    }
</script>

<div class="main-wrapper" class:draggable={count === undefined}>
    {#if count !== undefined}
        <div
                class="hud hud-container"
                bind:this={contentElement}
                style="max-width: {$maxWidth}px"
                aria-hidden={count === undefined}
                out:FadeOut|global={{ duration: 200 }}>
            {#if playerData?.mainHandStack}
                <img
                        class="icon"
                        src="{REST_BASE}/api/v1/client/resource/itemTexture?id={playerData.mainHandStack.identifier}"
                        alt={playerData.mainHandStack.identifier}
                />
            {/if}
            <div class="count">{count} Blocks</div>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .main-wrapper {
    display: flex;
    justify-content: center;
    align-items: center;
    position: absolute;
    border: 6px dashed transparent;
    border-radius: 10px;
    min-height: 60px;
    transition: background-color, border-color 0.3s ease;
    will-change: transform;
    &:hover {
      background: rgba(204, 204, 204, 0.2);
      border-color: #ccc;
      min-width: 200px;
    }

    &.draggable {
      cursor: move;
      &:hover {
        border-color: rgba(255, 255, 255, 0.8) !important;
        background: rgba(204, 204, 204, 0.3);
      }
    }
  }

  .hud {
    display: flex;
    align-items: center;
    height: 48px;
    padding: 0 16px;
    gap: 12px;
    background: rgba(0, 0, 0, 0.25);
    border-radius: 24px;
    overflow: hidden;
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    white-space: nowrap;
    user-select: none;
    transition: max-width 0.15s ease;
    width: auto;
  }

  .icon {
    width: 32px;
    height: 32px;
    object-fit: contain;
    display: block;
  }

  .count {
    padding-right: 4px;
  }
</style>
