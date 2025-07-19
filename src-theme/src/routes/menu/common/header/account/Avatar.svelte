<script lang="ts">
    import RippleLoader from "../../RippleLoader.svelte";

    export let url: string;

    let previewImageLoaded = false;
</script>

<div class="avatar-wrapper">
    {#if !previewImageLoaded}
        <div class="loader">
            <RippleLoader/>
        </div>
    {/if}

    <img alt="avatar" class="avatar" draggable="false" on:load={() => previewImageLoaded = true} src={url}>
</div>

<style lang="scss">
  .avatar-wrapper {
    grid-area: a;
    height: 50px;
    width: 50px;
    position: relative;
  }

  .avatar {
    height: 100%;
    width: 100%;
    border-radius: 5px;
  }

  .loader {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }
</style>
