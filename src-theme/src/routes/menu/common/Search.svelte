<script lang="ts">
    import {createEventDispatcher} from "svelte";

    let value = "";

    const dispatch = createEventDispatcher<{
        search: { query: string }
    }>();
</script>

<input class="search" type="text" placeholder="Search..." bind:value={value}
       on:input={() => dispatch("search", {query: value})} spellcheck="false">

<style lang="scss">

  .search {
    background-color: color-mix(in srgb,var(--menu-search-background-color) 70%, transparent);
    border: none;
    color: var(--menu-text-color);
    font-family: "Inter", sans-serif;
    font-size: 20px;
    border-radius: 12px;
    border-bottom: solid 4px color-mix(in srgb,var(--menu-search-border-color) 70%, transparent);
    background-image: url("/img/menu/icon-search.svg");
    background-repeat: no-repeat;
    background-position: 20px center;
    padding: 20px 20px 20px 60px;
    flex: 1;
  }
</style>
