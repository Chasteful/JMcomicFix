<script>

    import { fly } from "svelte/transition";

</script>
<div class="bottom-button-wrapper" transition:fly|global={{duration: 300, y: 100}}>
    <slot/>
</div>

<style lang="scss">
  .bottom-button-wrapper {
    display: flex;
    justify-content: space-between;
  }
</style>