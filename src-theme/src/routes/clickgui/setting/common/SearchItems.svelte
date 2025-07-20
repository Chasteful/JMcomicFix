<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import type {SearchSetting, ModuleSetting} from "../../../../integration/types";
    import {getRegistries} from "../../../../integration/rest";
    import ItemView from "../../../hud/common/ItemView/ItemView.svelte";
    import VirtualList from "./VirtualList.svelte";
    import {setItem} from "../../../../integration/persistent_storage";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import ExpandArrow from "./ExpandArrow.svelte";
    import {slide} from "svelte/transition";

    export let setting: ModuleSetting;
    export let path: string;
    export let type: 'blocks' | 'items' = 'items';
    const cSetting = setting as SearchSetting;

    interface T {
        name: string;
        identifier: string;
    }
    const thisPath = `${path}.${cSetting.name}`;
    const dispatch = createEventDispatcher();
    let Items: T[] = [];
    let renderedItems: T[] = Items;
    let searchQuery = "";
    let expanded = localStorage.getItem(thisPath) === "true";
    $: setItem(thisPath, expanded.toString());
    $: {
        let filteredItems = Items;
        if (searchQuery) {
            filteredItems = filteredItems.filter(b => b.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedItems = filteredItems;
    }

    onMount(async () => {
        let registry = await getRegistries();
        let data = registry[type];

        if (data !== undefined) {
            Items = data.sort((a, b) => a.identifier.localeCompare(b.identifier));
        }
    });

    function handleBlockToggle(e: CustomEvent<{ identifier: string, enabled: boolean }>) {
        if (e.detail.enabled) {
            cSetting.value = [...cSetting.value, e.detail.identifier];
        } else {
            cSetting.value = cSetting.value.filter(b => b !== e.detail.identifier);
        }

        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="head" class:expanded on:contextmenu|preventDefault={() => expanded = !expanded}>
        <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <ExpandArrow bind:expanded/>
    </div>
    {#if expanded}
        <div in:slide|global={{duration: 200, axis: "y"}} out:slide|global={{duration: 200, axis: "y"}}>
            <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery} spellcheck="false">
            <div class="results">
                <VirtualList items={renderedItems} let:item>
                    <ItemView identifier={item.identifier} name={item.name}
                           enabled={cSetting.value.includes(item.identifier)} on:toggle={handleBlockToggle}/>
                </VirtualList>
            </div>
        </div>
    {/if}
</div>


<style lang="scss">
  @use "../../../../colors" as *;

  .setting {
    padding: 7px 0;
  }
  .head {
    display: flex;
    justify-content: space-between;
    transition: ease margin-bottom .2s;

    &.expanded {
      margin-bottom: 10px;
    }

    .name {
      color: $text;
      font-size: var(--font-size);
      font-weight: 500;
    }
  }
  .results {
    height: 200px;
    overflow-y: auto;
    overflow-x: hidden;
    min-height: 100px;
    max-height: 500px;
    position: relative;
  }

  .search-input {
    width: 100%;
    border: none;
    border-bottom: solid 1px var(--primary-color);

    font-size: var(--font-size);
    padding: 5px;
    color: $text;
    margin-bottom: 5px;
    background-color: rgba($base, .36);
  }
</style>
