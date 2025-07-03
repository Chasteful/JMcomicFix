<script lang="ts">
    import {createEventDispatcher} from "svelte";

    let value = "";

    const dispatch = createEventDispatcher<{
        search: { query: string }
    }>();
</script>

<input bind:value={value} class="search" on:input={() => dispatch("search", {query: value})} placeholder="Search..."
       spellcheck="false" type="text">

<style lang="scss">
  @use "../../../colors.scss" as *;

  .search {
    background-color: rgba($base, 0.5);
    color: $text;
    font-family: "Inter", "Genshin", sans-serif;
    font-size: 20px;
    border-radius: 12px;
    border: none;
    border-bottom: solid 4px rgba($text, 0.7);
    background-image: url("/img/menu/icon-search.svg");
    background-repeat: no-repeat;
    background-position: 20px center;
    padding: 20px 20px 20px 60px;
    flex: 1;
  }
</style>
