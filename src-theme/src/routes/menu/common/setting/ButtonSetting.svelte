<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import CircleLoader from "../CircleLoader.svelte";

    export let title: string;
    export let disabled = false;
    export let secondary = false;
    export let inset = false;
    export let listenForEnter = false;
    export let loading = false;
    export let centerText = false;
    export let matchWidth = false;

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
<button
        class="button-setting"
        class:inset
        class:secondary
        class:centerText
        class:matchWidth
        {disabled}
        on:click={() => dispatch("click")}
        type="button"
>
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
    color: rgba($text, 0.6);
    font-weight: 600;
    font-size: 20px;
    padding: 20px;
    border: none;
    border-radius: 12px;
    transition: ease background-color .2s, ease opacity .2s, border-bottom .2s ease;
    background: rgba($base, 0.5);
    box-shadow: 0 0 8px rgba($base, 0.6);

    &.centerText {
      display: flex;
      justify-content: center;
      align-items: center;
    }

    &.matchWidth {
      width: 100%;
    }

    &.inset {
      margin: 0 30px;
    }

    &:not([disabled]):hover {
      color: $text;
      background-color: color.adjust(color.adjust(rgba($accent-mix, 0.4), $saturation: -30%), $lightness: -10%);
      cursor: pointer;

      &.secondary {
        background-color: color.adjust(color.adjust($base, $saturation: -30%), $lightness: -10%);
      }
    }

    &[disabled] {
      opacity: .6;

    }
  }
</style>
