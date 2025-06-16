<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";

    export let index: ItemStack;

    const {count, identifier, hasEnchantment} = index;
    const countColor = count <= 0 ? "red" : "white";
    const itemIconUrl = `${REST_BASE}/api/v1/client/resource/itemTexture?id=${identifier}`;

</script>
<div class="item-stack">
    {#if hasEnchantment}
        <div class="enchant-glint" style="mask-image: url({itemIconUrl})"></div>
    {/if}
    <img alt={identifier} class="item-icon" src={itemIconUrl}/>
    <div class="count" class:hidden={count === 0 || count === 1} style="color: {countColor}">
        {count}
    </div>
</div>

<style lang="scss">
  @import "../../../../colors";

  .hidden {
    display: none;
  }

  .item-stack {
    position: relative;
    width: 32px;
    height: 32px;
  }

  .item-icon {
    width: 100%;
    height: 100%;
    mask-image: none;
  }

  .count {
    position: absolute;
    bottom: 0;
    right: 0;
    font-size: 12px;
    text-shadow: 1px 1px black;
  }

  .enchant-glint {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;

    background-image: url('/img/hud/inventory/glint.png');
    background-size: 200% 200%;
    animation: enchantGlint 1.5s linear infinite;

    mix-blend-mode: screen;
    opacity: 0.2;
    mask-size: 100% 100%;
    mask-repeat: no-repeat;
  }

  @keyframes enchantGlint {
    0% {
      background-position: 0 0;
    }
    100% {
      background-position: 100% 100%;
    }
  }
</style>
