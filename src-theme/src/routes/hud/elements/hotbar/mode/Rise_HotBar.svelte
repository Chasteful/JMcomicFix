<script lang="ts">
    import {listen} from "../../../../../integration/ws";
    import type {ClientPlayerDataEvent, PlayerInventoryEvent} from "../../../../../integration/events";
    import type {PlayerData, ItemStack} from "../../../../../integration/types";
    import {getPlayerData, getPlayerInventory} from "../../../../../integration/rest";
    import {onMount} from "svelte";
    import ItemStackView from "../../../common/ItemView/ItemStackView.svelte";

    let currentSlot = 0;
    let playerData: PlayerData | null = null;
    let hotbar: ItemStack[] = [];

    function updatePlayerData(s: PlayerData) {
        playerData = s;
        currentSlot = playerData.selectedSlot;
    }

    function updateInventory(inventory: any) {
        hotbar = [...inventory.main.slice(0, 9)];
    }

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        updatePlayerData(event.playerData);
    });

    listen("clientPlayerInventory", (event: PlayerInventoryEvent) => {
        updateInventory(event.inventory);
    });

    onMount(async () => {
        updatePlayerData(await getPlayerData());
        updateInventory(await getPlayerInventory());
    });
</script>

{#if playerData && playerData.gameMode !== "spectator"}
    <div class="hotbar">
        <div class="hotbar-elements">
            <div class="slider" style="left: {currentSlot * 45}px"></div>
            <div class="slots">
                {#each hotbar as stack (stack)}
                    <div class="slot">
                        {#if stack && stack.identifier !== "minecraft:air"}
                            <ItemStackView {stack}/>
                        {/if}
                    </div>
                {/each}
            </div>
        </div>
    </div>
{/if}

<style lang="scss">
  @import "../../../../../colors.scss";

  .hotbar {
    display: flex;
    justify-content: center;
  }

  .hotbar-elements {
    background-color: rgba(0, 0, 0, 0.4);
    position: relative;
    border-radius: 16px;
    box-shadow: 0 0 2px 2px rgba(0,0,0,0.4);
    overflow: hidden;

    .slider {
      border: none;
      height: 45px;
      width: 45px;
      position: absolute;
      border-radius: 16px;
      transition: ease-in left 0.1s;
      background-color: rgba(0, 0, 0, 0.4);
      filter: blur(2px);
    }

    .slots {
      display: flex;
    }

    .slot {
      height: 45px;
      width: 45px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
</style>
