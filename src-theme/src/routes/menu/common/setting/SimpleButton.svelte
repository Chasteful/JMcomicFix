<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import CircleLoader from "../CircleLoader.svelte";

    export let title: string;
    export let disabled = false;
    export let secondary = false;
    export let inset = false;
    export let listenForEnter = false;
    export let loading = false;

    const dispatch = createEventDispatcher();

    function handleKeyDown(e: KeyboardEvent) {
        if (!listenForEnter) {
            return;
        }
        if (e.key === "Enter") {
            dispatch("click");
        }
    }
</script>

<svelte:window on:keydown={handleKeyDown}/>
<button class="button-setting" class:inset class:secondary {disabled} on:click={() => dispatch("click")} type="button">
    {#if loading}
        <CircleLoader/>
    {/if}
    {title}
</button>

<style lang="scss">
  @use "sass:color";
  @use "../../../../colors.scss" as *;

  .button-setting {
    position: relative;
    color: #DDD;
    font-weight: 600;
    font-size: 20px;
    padding: 10px 24px 10px 24px;
    border: none;
    border-radius: 5px;
    transition: ease background-color .2s, ease opacity .2s, border-bottom .2s ease;
    background: rgba($base, 0.5);
    box-shadow: inset -2px -4px #0006, inset 2px 2px #FFF7;
    text-shadow: 2px 2px #000A;

    &.inset {
      margin: 0 30px;
    }

    &:not([disabled]):hover {
      background-color: color.adjust(color.adjust(rgba($accent-mix, 0.4), $saturation: -30%), $lightness: -10%);
      color: #FFFFA0;
      text-shadow: 2px 2px #202013CC;
      cursor: pointer;

      &.secondary {
        box-shadow: inset -2px -4px #0004, inset 2px 2px #FFF5;
        background-color: color.adjust(color.adjust($base, $saturation: -30%), $lightness: -10%);
      }
    }

    &[disabled] {
      opacity: .6;
    }
  }
</style>
