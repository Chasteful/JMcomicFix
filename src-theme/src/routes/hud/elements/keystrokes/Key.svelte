<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import type {KeyEvent} from "../../../../integration/events";
    import type {MinecraftKeybind} from "../../../../integration/types";

    export let gridArea: string;
    export let key: MinecraftKeybind | undefined;
    export let asBar: boolean = false;

    let active = false;
    let actived = false;

    listen("key", (e: KeyEvent) => {
        if (e.key !== key?.key.translationKey) return;
        if (e.action === 1 || e.action === 2) {
            active = true;
            actived = false;
        } else {
            active = false;
            actived = true;
            setTimeout(() => (actived = false), 200);
        }
    });

</script>
<div class="key" class:active class:actived class:asBar style="grid-area: {gridArea};">
    {#if !asBar}
        {key?.key.localized ?? "???"}
    {:else}
        <div class="bar"/>
    {/if}
</div>


<style lang="scss">
  @use "../../../../colors.scss" as *;

  @keyframes activeEffect {
    0% {
      border-radius: 50%;
      transform: scale(0.1);
      opacity: 0.1;
    }
    10% {
      border-radius: 50%;
      transform: scale(0.3);
      opacity: 0.3;
    }
    100% {
      border-radius: inherit;
      transform: scale(1);
      opacity: 0.4;
    }
  }

  @keyframes activedEffect {
    0% {
      border-radius: inherit;
      transform: scale(1);
      opacity: 0.4;
    }
    100% {
      border-radius: 50%;
      transform: scale(0.1);
      opacity: 0;
    }
  }

  .key {
    height: 50px;
    color: $text;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: 900;
    text-transform: uppercase;
    position: relative;
    background: transparent;
    cursor: pointer;
    overflow: hidden;
    border-radius: 12px;
    will-change: transform, opacity;
    transition: box-shadow 0.1s ease;
    background: linear-gradient(
                    135deg,
                    rgba(20, 20, 20, 0.6) 0%,
                    rgba(darken($base, 5%), 0.5) 100%
    );
    border: 1px solid rgba(255, 255, 255, 0.08);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.28),
    0 0 0 1px rgba(255, 255, 255, 0.03) inset;

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background: radial-gradient(
                      circle at 50% 0%,
                      color-mix(in srgb, var(--primary-color) 15%, transparent) 0%,
                      transparent 70%
      );
      pointer-events: none;
      z-index: -1;
      opacity: 0;
      transition: opacity 0.5s ease,
      transform 0.3s cubic-bezier(0.2, 0.8, 0.4, 1.2);
    }

    .bar {
      width: 33%;
      height: 4px;
      background-color: currentColor;
      border-radius: 2px;
    }

    &:active {
      box-shadow: 0 0 10px rgba($key-color, 0.6) inset;

    }

    &:hover {
      box-shadow: 0 0 6px rgba($key-color, 0.3);
    }

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background: rgba(20, 20, 20, 0.2);
      border-radius: inherit;
      z-index: 0;
    }

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      background: rgba($key-color, 0.8);
      border-radius: 50%;
      border: 1px solid rgba(255, 255, 255, 0.8);
      opacity: 0;
      z-index: 1;
      transform-origin: center;
      pointer-events: none;
      transform: scale(0.1);
    }

    &.active::after {
      animation: activeEffect 0.2s ease forwards;
    }

    &.actived::after {
      animation: activedEffect 0.2s ease forwards;
    }
  }
</style>
