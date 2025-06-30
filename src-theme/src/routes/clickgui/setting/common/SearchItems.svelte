<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import type {SearchSetting, ModuleSetting} from "../../../../integration/types";
    import {getRegistries} from "../../../../integration/rest";
    import ItemView from "../../../hud/common/ItemView/ItemView.svelte";
    import VirtualList from "./VirtualList.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";

    export let setting: ModuleSetting;
    export let type: 'blocks' | 'items' = 'items';
    const cSetting = setting as SearchSetting;

    interface T {
        name: string;
        identifier: string;
    }

    const dispatch = createEventDispatcher();
    let Items: T[] = [];
    let renderedItems: T[] = Items;
    let searchQuery = "";

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
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <input bind:value={searchQuery} class="search-input" placeholder="Search" spellcheck="false" type="text">
    <div class="results">
        <VirtualList items={renderedItems} let:item>
            <ItemView enabled={cSetting.value.includes(item.identifier)} identifier={item.identifier} name={item.name}
                      on:toggle={handleBlockToggle}/>
        </VirtualList>
    </div>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .setting {
    padding: 7px 0;
  }

  .results {
    height: 200px;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .name {
    color: $text;
    font-size: var(--font-size);
    font-weight: 500;
    margin-bottom: 5px;
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
