<script lang="ts">
    import {fade} from "svelte/transition";
    import {onMount} from "svelte";

    let width = window.innerWidth;
    let height = window.innerHeight;

    function updateSize() {
        width = window.innerWidth;
        height = window.innerHeight;
    }

    onMount(() => {
        updateSize();
        window.addEventListener("resize", updateSize);
        return () => window.removeEventListener("resize", updateSize);
    });
</script>

<div class="vignette"
     style="width: {width}px; height: {height}px;"
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

