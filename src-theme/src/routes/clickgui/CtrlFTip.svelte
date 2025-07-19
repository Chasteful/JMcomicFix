<script lang="ts">
    import {type Writable} from "svelte/store";
    import {onDestroy, onMount} from "svelte";
    import {quintOut} from "svelte/easing";
    import {fly} from "svelte/transition";

    export let showSearch: Writable<boolean>;
    let isHoveringTip = false;
    let tipCooldown = false;
    let tipCooldownTimeout: ReturnType<typeof setTimeout> | null = null;

    $: showTip = !$showSearch && isHoveringTip && !tipCooldown;

    function resetSearchState() {
        isHoveringTip = false;
    }

    function handleTipClick() {
        showSearch.set(true);
        resetSearchState();
    }

    const handleKeydown = (event: KeyboardEvent) => {
        if (event.ctrlKey && event.key === 'f') {
            event.preventDefault();
            showSearch.update(current => {
                const next = !current;
                if (!next) {
                    resetSearchState();
                    tipCooldown = true;
                    if (tipCooldownTimeout) clearTimeout(tipCooldownTimeout);
                    tipCooldownTimeout = setTimeout(() => {
                        tipCooldown = false;
                    }, 500);
                }
                return next;
            });
        }
    };

    onMount(() => {
        window.addEventListener("keydown", handleKeydown);
    });

    onDestroy(() => {
        window.removeEventListener("keydown", handleKeydown);
        if (tipCooldownTimeout) clearTimeout(tipCooldownTimeout);
    });
</script>


<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="ctrl-f-tip"
     class:hidden={$showSearch}
     class:visible={showTip}
     in:fly|global={{y: -30, duration: 200, easing: quintOut}}
     on:click={handleTipClick}
     on:mouseenter={() => isHoveringTip = true}
     on:mouseleave={() => isHoveringTip = false}
     out:fly|global={{y: -30, duration: 200, easing: quintOut}}>
    Press CTRL+F to open search bar
</div>


<style lang="scss">
  @use "../../colors.scss" as *;

  .ctrl-f-tip {
    position: absolute;
    bottom: 100px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 32px;
    color: rgba($text, 0.3);
    text-shadow: 0 0 16px rgba($text, 0.5);
    font-weight: bold;
    padding: 8px 16px;
    border-radius: 20px;
    background: rgba($base, 0);
    backdrop-filter: blur(5px);
    cursor: pointer;
    transition: all 0.3s ease;
    pointer-events: auto;
    font-family: "Genshin", sans-serif;

    &.visible {
      &:hover {
        color: rgba($text, 0.5);
        transform: translateX(-50%) scale(1.05);
        text-shadow: 4 8 12px rgba($base, 0.52),
        4 8 16px rgba($text, 0.8);
      }
    }
  }

  .ctrl-f-tip.hidden {
    visibility: hidden;
    opacity: 0;
    pointer-events: none;
  }
</style>
