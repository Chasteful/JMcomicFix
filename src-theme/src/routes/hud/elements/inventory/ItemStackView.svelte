<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";
    import {mapToColor} from "../../../../util/color_utils";
    import  {fade} from "svelte/transition";

    export let stack: ItemStack;

    const {count, damage, identifier, maxDamage, hasEnchantment } = stack;
    const countColor = count <= 0 ? "red" : "white";
    const valueColor = mapToColor(120 * (maxDamage - damage) / maxDamage);
    const itemIconUrl = `${REST_BASE}/api/v1/client/resource/itemTexture?id=${identifier}`;

</script>
<div class="item-stack">
    {#if hasEnchantment}
        <div class="enchant-glint" style="mask-image: url({itemIconUrl})"></div>
    {/if}
    <img class="item-icon" src={itemIconUrl} alt={identifier}/>
    <div class="durability-bar" class:hidden={damage === 0}>
        <div class="durability"
             style="width: {100 * (maxDamage - damage) / maxDamage}%; background-color: {valueColor}">
        </div>
    </div>
    <div class="count" class:hidden={count === 0 || count === 1} style="color: {countColor}">
        {count}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors" as *;
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
    bottom: 0;
    left: 10%;
    width: 80%;
    height: 2px;
    background-color: rgba($item-damage-base-color, 0.68);
  }
  .durability {
    height: 100%;
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
