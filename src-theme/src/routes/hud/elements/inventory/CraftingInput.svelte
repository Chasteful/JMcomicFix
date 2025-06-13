<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import type {PlayerInventory, PlayerInventoryEvent} from "../../../../integration/events";
    import ItemStackView from "./ItemStackView.svelte";
    import {onMount} from "svelte";
    import {getPlayerInventory} from "../../../../integration/rest";

    let stacks: ItemStack[] = [];
    function updateStacks(inventory: PlayerInventory) {
        stacks = inventory.crafting;
    }
    listen("clientPlayerInventory", (data: PlayerInventoryEvent) => {
        updateStacks(data.inventory);
    });
    onMount(async () => {
        const inventory = await getPlayerInventory();
        updateStacks(inventory);
    });

</script>

<div class="container">
    {#each stacks as stack (stack)}
        <ItemStackView {stack}/>
    {/each}
</div>
<style lang="scss">
  @use "../../../../colors" as *;
  .container {
    box-shadow:
            0 4px 16px rgba($base, 0.6),
            inset 0 0 10px rgba(255, 255, 255, 0.05);
    background: rgba($base, 0.5);
    grid-template-columns: repeat(2, 1fr);
    padding: 4px;
    border-radius: 8px;
    min-width: 72px;
    min-height: 72px;
    display: grid;
    gap: 0.5rem;
  }

</style>
