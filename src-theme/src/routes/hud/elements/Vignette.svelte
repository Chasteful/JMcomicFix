<script lang="ts">
    import {fade} from "svelte/transition";
    import {onDestroy} from "svelte";
    import {WindowSize} from "../../../util/WindowSize";

    const {width, height, destroy} = WindowSize();

    onDestroy(destroy);
</script>

<div class="vignette"
     style="width: { $width }px; height: { $height }px;"
     transition:fade|global={{duration: 300}}>
</div>

<style lang="scss">
  .vignette {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 9999;
    pointer-events: none;
    background: url('/Vignette.png') no-repeat;
    background-size: 100% 100%;
  }
</style>

