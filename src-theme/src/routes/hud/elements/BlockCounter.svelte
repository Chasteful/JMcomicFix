<script lang="ts">
    import type {PlayerData} from "../../../integration/types";
    import type {BlockCountChangeEvent, ClientPlayerDataEvent, ModuleToggleEvent} from "../../../integration/events";
    import {listen} from "../../../integration/ws";
    import {getPlayerData, itemTextureUrl} from "../../../integration/rest";
    import {onMount, tick} from "svelte";
    import {FadeOut} from "../../../util/animate_utils";
    import {blockCount} from './island/Island';
    import {cubicOut} from "svelte/easing";
    import {Tween} from "svelte/motion";

    export let settings: { [name: string]: any };

    let playerData: PlayerData | null = null;

    let nextBlock: string | undefined = undefined;
    let count: number | undefined = undefined;

    let contentElement: HTMLDivElement;
    let firstAppear = true;
    let scaffoldEnable = false
    const maxWidth = new Tween(0, {duration: 150, easing: cubicOut});

    $: if (contentElement && count !== undefined) {
        updateMaxWidth(firstAppear);
        firstAppear = false;
    } else if (count === undefined) {
        maxWidth.set(0);
        firstAppear = true;
    }

    async function updateMaxWidth(isFirstAppear = false) {
        if (isFirstAppear) {
            await maxWidth.set(0, {duration: 0});
            await tick();
        }

        const style = getComputedStyle(contentElement);
        const fullWidth =
            contentElement.scrollWidth +
            parseFloat(style.paddingLeft) +
            parseFloat(style.paddingRight);

        await maxWidth.set(fullWidth);
    }

    listen("blockCountChange", (e: BlockCountChangeEvent) => {
        nextBlock = e.nextBlock;
        count = e.count;
        blockCount.set(e.count);
    });

    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        playerData = e.playerData;
    });

    listen("moduleToggle", (e: ModuleToggleEvent) => {
        if (e.moduleName === "Scaffold") {
            scaffoldEnable = e.enabled;
        }
    });

    onMount(async () => {
        playerData = await getPlayerData();
        if (contentElement && count !== undefined) {
            await updateMaxWidth(true);
        }

    });
</script>

<div class="main-wrapper" class:draggable={count === undefined} style="transform: scale({settings.scale});">
    {#if count !== undefined && scaffoldEnable}
        <div
                class="hud hud-container"
                bind:this={contentElement}
                style="max-width: {maxWidth.current}px"
                aria-hidden={count === undefined}
                out:FadeOut|global={{ duration: 200 }}>
            {#if nextBlock}
            <img class="icon" src={itemTextureUrl(nextBlock)} alt={nextBlock}/>
            {/if}
            <div class="count">Amount:
                <span class="count-number">{count}</span>
            </div>
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
      min-width: 150px;
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
    padding: 0 10px;
    border-radius: 12px;
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
    flex-shrink: 0;
    //filter: brightness(0) invert(1);
  }

  .count {
    color: #CCCCCC;
    text-shadow: 0 0 3px rgba(204, 204, 204, 0.9);
    padding: 0 8px 0 0;

    .count-number {
      font-weight: bold;
    }
  }
</style>
