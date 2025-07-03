<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import type {PlayerInventory, PlayerInventoryEvent} from "../../../../integration/events";
    import {getPlayerInventory} from "../../../../integration/rest";
    import ItemStackView from "../../common/ItemView/ItemStackView.svelte";
    import {onMount} from "svelte";
    import {expoInOut} from "svelte/easing";
    import {fly} from "svelte/transition";
    import {emptySlotCount} from "../island/Island";
    import Line from "../../common/Line.svelte";

    let stacks: ItemStack[] = [];

    function updateStacks(inventory: PlayerInventory) {
        stacks = inventory.main.slice(9);

        const emptySlots = stacks.filter(
            slot => slot.identifier === "air" || slot.identifier === "minecraft:air"
        ).length;


        emptySlotCount.set(emptySlots);
    }

    listen("clientPlayerInventory", (data: PlayerInventoryEvent) => {
        updateStacks(data.inventory);
    });
    onMount(async () => {
        const inventory = await getPlayerInventory();
        updateStacks(inventory);
    });

</script>

<div class="inventory-hud" id="inventoryhud" transition:fly|global={{duration: 500, y: -50, easing: expoInOut}}>
    <div class="title">
        <img alt="inventory" class="icon" src="img/hud/inventory/inventory.svg"/>
        <span>Inventory List</span>
    </div>
    <Line/>
    <div class="container">
        {#each stacks as stack (stack)}
            <ItemStackView {stack}/>
        {/each}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .inventory-hud {
    position: relative;
    background-color: rgba($base, 0.5);
    border-radius: 6px;
    padding: 6px 10px;
    width: fit-content;
    color: white;
    box-shadow: 0 4px 16px rgba($base, 0.6),
    inset 0 0 10px rgba(255, 255, 255, 0.05);
    user-select: none;
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
      height: 1em;
      width: auto;
    }
  }

  .container {
    display: grid;
    grid-template-columns: repeat(9, 32px);
    gap: 4px;
    min-height: 104px;
  }


</style>
