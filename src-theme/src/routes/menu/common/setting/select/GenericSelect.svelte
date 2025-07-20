<script lang="ts">
    import {quintOut} from "svelte/easing";
    import {fade} from "svelte/transition";

    export let closeOnInternalClick: boolean;

    let expanded = false;
    let selectElement: HTMLElement;
    let headerElement: HTMLElement;

    function handleWindowClick(e: MouseEvent) {
        if (!selectElement.contains(e.target as Node)) {
            expanded = false;
        }
    }

    function handleSelectClick(e: MouseEvent) {
        if (closeOnInternalClick) {
            expanded = !expanded;
        } else {
            if (!expanded) {
                expanded = true;
            } else {
                expanded = !headerElement.contains(e.target as Node);
            }
        }
    }
</script>
<svelte:window on:click={handleWindowClick}/>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div bind:this={selectElement} class="select" class:expanded on:click={handleSelectClick}>
    <div bind:this={headerElement} class="header">
        <span class="title">
            <slot name="title"/>
        </span>
        <img
                alt="expand"
                class="arrow"
                class:expanded
                draggable="false"
                src="img/menu/icon-select-arrow.svg"
        >
    </div>
    {#if expanded}
        <div class="options" transition:fade|global={{ duration: 200, easing: quintOut }}>
            <slot name="options"></slot>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;

  .select {
    cursor: pointer;
    min-width: 250px;
    position: relative;

    &.expanded {
      .header {
        border-radius: 12px 12px 0 0;
      }
    }
  }

  .header {
    background: rgba($base, 0.5);
    box-shadow: 0 0 8px rgba($base, 0.6);
    padding: 20px;
    display: flex;
    column-gap: 20px;
    align-items: center;
    justify-content: space-between;
    border-radius: 12px;
    transition: ease border-radius .2s;


    .title {
      color: $text;
      font-size: 20px;
      font-weight: 900;
    }
  }

  .arrow {
    width: 16px;
    height: 16px;
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &.expanded {
      transform: rotate(180deg);
    }
  }

  .options {
    position: absolute;
    z-index: 1000;
    width: 100%;
    border-radius: 0 0 12px 12px;
    max-height: 250px;
    border-top: 1px solid rgba(white, 0.1);
    overflow: auto;
    color: $text;
    background-color: rgba($base, 0.5);
    border-left: $text;
    box-shadow: 0 4px 8px rgba($base, 0.6);
  }
</style>
