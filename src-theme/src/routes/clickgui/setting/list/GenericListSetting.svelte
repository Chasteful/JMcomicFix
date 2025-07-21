<script lang="ts">
<<<<<<<< HEAD:src-theme/src/routes/clickgui/setting/common/SearchItems.svelte
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
========
    import {createEventDispatcher} from "svelte";
    import {slide} from "svelte/transition";
    import type {ListSetting, ModuleSetting, NamedItem} from "../../../../integration/types";
    import VirtualList from "../list/VirtualList.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {setItem} from "../../../../integration/persistent_storage";
    import ListItem from "./ListItem.svelte";

    export let setting: ModuleSetting;
    export let path: string;
    export let items: NamedItem[];

    const cSetting = setting as ListSetting;
    const thisPath = `${path}.${cSetting.name}`;

    const dispatch = createEventDispatcher();
    let renderedItems: NamedItem[] = items;
>>>>>>>> upstream/nextgen:src-theme/src/routes/clickgui/setting/list/GenericListSetting.svelte
    let searchQuery = "";
    let expanded = localStorage.getItem(thisPath) === "true";
    $: setItem(thisPath, expanded.toString());
    $: {
<<<<<<<< HEAD:src-theme/src/routes/clickgui/setting/common/SearchItems.svelte
        let filteredItems = Items;
========
        let filteredItems = items;
>>>>>>>> upstream/nextgen:src-theme/src/routes/clickgui/setting/list/GenericListSetting.svelte
        if (searchQuery) {
            filteredItems = filteredItems.filter(b => b.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedItems = filteredItems;
    }

<<<<<<<< HEAD:src-theme/src/routes/clickgui/setting/common/SearchItems.svelte
    onMount(async () => {
        let registry = await getRegistries();
        let data = registry[type];

        if (data !== undefined) {
            Items = data.sort((a, b) => a.identifier.localeCompare(b.identifier));
        }
    });

    function handleBlockToggle(e: CustomEvent<{ identifier: string, enabled: boolean }>) {
========
    function handleItemToggle(e: CustomEvent<{ value: string, enabled: boolean }>) {
>>>>>>>> upstream/nextgen:src-theme/src/routes/clickgui/setting/list/GenericListSetting.svelte
        if (e.detail.enabled) {
            cSetting.value = [...cSetting.value, e.detail.value];
        } else {
            cSetting.value = cSetting.value.filter(b => b !== e.detail.value);
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
<<<<<<<< HEAD:src-theme/src/routes/clickgui/setting/common/SearchItems.svelte
                    <ItemView identifier={item.identifier} name={item.name}
                           enabled={cSetting.value.includes(item.identifier)} on:toggle={handleBlockToggle}/>
========
                    <ListItem value={item.value} name={item.name} icon={item.icon}
                            enabled={cSetting.value.includes(item.value)} on:toggle={handleItemToggle}/>
>>>>>>>> upstream/nextgen:src-theme/src/routes/clickgui/setting/list/GenericListSetting.svelte
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
<<<<<<<< HEAD:src-theme/src/routes/clickgui/setting/common/SearchItems.svelte
      color: $text;
      font-size: var(--font-size);
      font-weight: 500;
========
      color: $clickgui-text-color;
      font-size: 12px;
      font-weight: 600;
>>>>>>>> upstream/nextgen:src-theme/src/routes/clickgui/setting/list/GenericListSetting.svelte
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
