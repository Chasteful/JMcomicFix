<script lang="ts">
    import {REST_BASE} from "../../../../integration/host";
    import {createEventDispatcher} from "svelte";

    const dispatch = createEventDispatcher<{
        toggle: { identifier: string, enabled: boolean }
    }>();

    export let identifier: string;
    export let name: string;
    export let enabled: boolean;
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="block" on:click={() => dispatch("toggle", {enabled: !enabled, identifier})}>
    <img alt={identifier} class="icon" src="{REST_BASE}/api/v1/client/resource/itemTexture?id={identifier}"/>
    <div class="name">{name}</div>
    <div class="tick">
        {#if enabled}
            <img src="img/clickgui/icon-tick-checked.svg" alt="enabled">
        {:else}
            <img src="img/clickgui/icon-tick.svg" alt="disabled">
        {/if}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .block {
    display: grid;
    grid-template-columns: max-content 1fr max-content;
    align-items: center;
    column-gap: 5px;
    cursor: pointer;
    margin: 2px 5px 2px 0;
  }

  .icon {
    height: 25px;
    width: 25px;
  }

  .name {
    font-size: 14px;
    color: $text;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
  }
</style>
