<script lang="ts">
    import {fly} from "svelte/transition";
    import {SortableList} from "@jhubbardsf/svelte-sortablejs";
    import "./menulist.scss";
    import {createEventDispatcher} from "svelte";

    export let sortable = false;
    export let elementCount = -1;

    let sortableElement: HTMLElement | undefined;

    interface MenuListSortEvent {
        newOrder: number[]
    }

    const dispatch = createEventDispatcher<{
        sort: MenuListSortEvent
    }>();

    function handleChange(e: any) {
        dispatch("sort", {
            newOrder: calculateNewOrder(e.oldIndex, e.newIndex, elementCount)
        });
    }

    function calculateNewOrder(oldIndex: number, newIndex: number, length: number): number[] {
        const a = Array.from({length}, (x, i) => i);
        a.splice(oldIndex, 1);
        a.splice(newIndex, 0, oldIndex);
        return a;
    }
</script>

<div class="menu-list" transition:fly|global={{duration: 700, x: 1000}}>
    {#if sortable && elementCount > -1}
        <SortableList class="menu-list-items" onSort={handleChange} forceFallback={true} animation={150}>
            <slot/>
        </SortableList>
    {:else}
        <div class="menu-list-items">
            <slot/>
        </div>
    {/if}
</div>


<style lang="scss">
  @import "../../../../colors";

  .menu-list {
    background-color: rgba($base, 0.5);
    box-shadow: 0 0 8px rgba($base, 0.6);
    flex: 1;
    border-radius: 16px;
    margin-bottom: 32px;
    position: relative;
  }
</style>
