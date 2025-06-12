<script lang="ts">
    import { REST_BASE } from "../../../../integration/host";
    import type { ItemStack } from "../../../../integration/types";

    export let itemStack: ItemStack;

    const {identifier, hasEnchantment } = itemStack;
    const itemIconUrl = `${REST_BASE}/api/v1/client/resource/itemTexture?id=${identifier}`;
    /*
    const hasDurability = itemStack.maxDamage > 0;
     $: value = hasDurability
       ? itemStack.maxDamage - itemStack.damage
       : itemStack.count;*/

</script>
<div class="item-box">
    <div class="content">
        <!-- svelte-ignore element_invalid_self_closing_tag -->
        {#if hasEnchantment}
            <div class="enchant-glint" style="mask-image: url({itemIconUrl})"></div>
        {/if}
        <img class="item-icon"  src={itemIconUrl} alt={identifier}/>
    </div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .item-box {
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .content {
    position: relative;
    width: 100%;
    height: 100%;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-start;
    z-index: 0;
  }
  .item-icon {
    width: 32px;
    height: 32px;
    z-index: 1;
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
    z-index: 1;
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
