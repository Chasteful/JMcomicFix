<script lang="ts">
    import {REST_BASE} from "../../../integration/host";
    import type {PlayerData} from "../../../integration/types";
    import type {BlockCountChangeEvent, ClientPlayerDataEvent} from "../../../integration/events";
    import {listen} from "../../../integration/ws";
    import {getPlayerData} from "../../../integration/rest";
    import {onMount} from "svelte";
    import {FadeOut, FadeIn} from "../../../util/animate_utils";
    import {blockCount} from './island/Island';

    let playerData: PlayerData | null = null;
    let count: number | undefined;
    listen("blockCountChange", (e: BlockCountChangeEvent) => {
        count = e.count;
        blockCount.set(e.count);
    });

    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        playerData = e.playerData;
    });
    onMount(async () => {
        playerData = await getPlayerData();
    });

</script>
<div class="main-wrapper" class:draggable={count === undefined}>
    {#if count !== undefined}
        <div class="hud"
             in:FadeIn|global={{ duration: 200 }}
             out:FadeOut|global={{ duration: 200 }}>
            <div class="blocks-icon">
                {#if playerData?.mainHandStack }
                    <div class="item-box">
                        <div class="content">
                            <!-- svelte-ignore element_invalid_self_closing_tag -->
                            <div class="bg"/>
                            <img
                                    class="icon"
                                    src="{REST_BASE}/api/v1/client/resource/itemTexture?id={playerData.mainHandStack.identifier}"
                                    alt={playerData.mainHandStack.identifier}
                            />
                        </div>
                    </div>
                {/if}
            </div>
            <div class="count">{count}</div>
        </div>
    {:else}
        <div class="empty-placeholder"/>
    {/if}
</div>
<style lang="scss">
  @use "../../../colors.scss" as *;

  .main-wrapper {
    display: flex;
    justify-content: center;
    flex-direction: column;
    align-items: center;
    position: absolute;
    border: 6px dashed transparent;
    border-radius: 10px;
    min-width: 80px;
    min-height: 80px;
    transition: background-color, border-color 0.3s ease;

    &:hover {
      background: rgba(204, 204, 204, 0.2);
      border-color: #ccc;

    }

    .empty-placeholder {
      display: none;
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
    position: relative;
    width: 80px;
    padding: 8px 0;
    background-color: rgba($base, 0.5);
    border-radius: 8px;
    text-align: center;
    color: #fff;
    user-select: none;
    box-shadow: 0 4px 16px rgba($base, 0.6),
    inset 0 0 10px rgba(255, 255, 255, 0.05);

  }

  .item-box {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .content {
    position: relative;
    width: 100%;
    height: 100%;
    border-radius: 8px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-start;
    z-index: 0;
  }

  .icon {
    width: 32px;
    height: 32px;
    image-rendering: pixelated;
    z-index: 1;
    margin-bottom: 4px;
  }

  .count {
    font-size: 16px;
    font-weight: bold;

  }

</style>
