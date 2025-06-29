<script lang="ts">
  import {slide} from "svelte/transition";
  import {quintOut} from "svelte/easing";
  import {createEventDispatcher} from "svelte";
  import GenericSelect from "./GenericSelect.svelte";

  export let options: string[];
  export let value: string;
  export let title: string;

  const dispatch = createEventDispatcher<{
      change: { value: string }
  }>();

  function handleOptionClick(o: string) {
      value = o;
      dispatch("change", {value: o});
  }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<GenericSelect closeOnInternalClick={true}>
  <span slot="title"><span class="title">{title}</span> {value}</span>

  <svelte:fragment slot="options">
      {#each options as o}
          <div on:click={() => handleOptionClick(o)} class="option" class:active={o === value}
               transition:slide|global={{ duration: 200, easing: quintOut }}>{o}</div>
      {/each}
  </svelte:fragment>
</GenericSelect>

<style lang="scss">
  @use "../../../../../colors.scss" as *;

  .title {
    font-weight: 900;
  }

  .option {
    font-weight: 500;
    color: rgba($text ,0.5);
    font-size: 20px;
    padding: 15px 20px;
    transition: ease color .2s;

    &:hover {
      color: $text;
    }

    &.active {
      color: rgba($text,0.7);
      text-shadow: 0 0 4px rgba($text,0.7);
    }
  }
</style>
