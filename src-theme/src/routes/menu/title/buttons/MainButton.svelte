<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {backIn, backOut} from "svelte/easing";
    import {fade, fly} from "svelte/transition";

    export let title: string;
    export let icon: string;
    export let index: number;

    let hovered = false;
    const dispatch = createEventDispatcher();
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="main-button"
     in:fly|global={{duration: 400, x: -500, delay: index * 100, easing: backOut}}
     on:click={() => { hovered = false; dispatch("click"); }}
     on:mouseenter={() => hovered = true}
     on:mouseleave={() => hovered = false}
     out:fly|global={{duration: 400, x: -500, delay: index * 100, easing: backIn}}
>
    <div class="glass-content">
        <div class="button-content">
            <div class="icon">
                <img alt={icon} draggable="false" src="img/menu/icon-{icon}.svg" transition:fade={{duration: 200}}>
            </div>
            <div class="title">{title}</div>
        </div>

    </div>
    <div class="glass-filter"></div>
    <div class="glass-overlay"></div>
    <div class="glass-specular"></div>
    <svg xmlns="http://www.w3.org/2000/svg" style="display:none">
        <filter id="lensFilter" x="0%" y="0%" width="100%" height="100%" filterUnits="objectBoundingBox">
            <feComponentTransfer in="SourceAlpha" result="alpha">
                <feFuncA type="identity" />
            </feComponentTransfer>
            <feGaussianBlur in="alpha" stdDeviation="50" result="blur" />
            <feDisplacementMap in="SourceGraphic" in2="blur" scale="40" xChannelSelector="A" yChannelSelector="A" />
        </filter>
    </svg>

    <div class="wrapped-content">
        <slot parentHovered={hovered}/>
    </div>
</div>



<style lang="scss">
  @use "../../../../colors.scss" as *;

  .main-button {
    width: 600px;
    padding: 25px 35px;
    display: grid;
    grid-template-columns: max-content 1fr max-content;
    align-items: center;
    cursor: pointer;
    background: transparent;
    border-radius: 36px;

    z-index: 1;
    text-decoration: none;
    overflow: hidden;
    letter-spacing: 2px;
    transition: 0.5s;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      width: 100%;
      height: 100%;
      background: linear-gradient(to right, rgba(255, 255, 255, 0.1), transparent);
      transform: skewX(45deg) translateX(-10%);
      transition: 0.5s;
    }

    &:hover::before {
      transform: skewX(45deg) translateX(200%);
    }

    &:hover {
      transform: scale(1.1);
    }
    .glass-filter,
    .glass-overlay,
    .glass-specular {
      position: absolute;
      inset: 0;
      border-radius: inherit;
      z-index: 0;
    }

    .glass-filter {
      z-index: 0;
      backdrop-filter: blur(10px);
      filter: url(#lensFilter) saturate(120%) brightness(1.15);
    }

    .glass-overlay {
      z-index: 1;
      background: rgba(255, 255, 255, 0.1);
      border-top: 1px solid rgba(255, 255, 255, 0.25);
      border-bottom: 1px solid rgba(255, 255, 255, 0.15);
    }

    .glass-specular {
      z-index: 2;
      box-shadow: inset 1px 1px 0 rgba(255, 255, 255, 0.6),
      inset 0 0 5px rgba(255, 255, 255, 0.6);

    }
    .glass-content {
      position: relative;
      display: contents;
    }
  }

  .button-content {
    display: flex;
    align-items: center;
    gap: 20px;
    flex-grow: 1;
    transition: letter-spacing 0.5s;
  }

  .main-button:hover .button-content {
    letter-spacing: 5px;
  }

  .icon {
    width: 90px;
    height: 90px;
    position: relative;
    flex-shrink: 0;

    img {
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
      width: 40px;
      height: 40px;
      object-fit: contain;
    }
  }

  .title {
    font-size: 26px;
    color: var(--text-color);
    font-family: 'DreamScape', serif;
    font-weight: 600;
    text-align: center;

  }
</style>
