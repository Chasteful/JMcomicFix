<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";


    export let stack: ItemStack;

    const {count, damage, identifier, maxDamage, hasEnchantment} = stack;
    const countColor = count <= 0 ? "red" : "white";
    const itemIconUrl = `${REST_BASE}/api/v1/client/resource/itemTexture?id=${identifier}`;
    const showDurability = maxDamage > 0;
</script>
<div class="item-stack">
    {#if hasEnchantment}
        <div class="enchant-glint" style="mask-image: url({itemIconUrl})"></div>
    {/if}
    <img alt={identifier} class="item-icon" src={itemIconUrl}/>
    {#if showDurability}
        <div class="durability-bar">
            <div class="durability"
                 style="width: {100 * (maxDamage - damage) / maxDamage}%;">
            </div>
        </div>
    {/if}
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

  .durability-bar {
    position: absolute;
    bottom: -4px;
    left: 10%;
    width: 80%;
    height: 4px;
    border-radius: 12px;
    box-shadow: 0 0 2px $text;
    background-color: rgba($text, 0.68);
  }

  .durability {
    height: 100%;
    border-radius: 12px;
    background-color: $text;
    transition: width 150ms;
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
    opacity: 0.5;
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
