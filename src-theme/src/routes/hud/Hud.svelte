<script lang="ts">
  import { onMount } from "svelte";
  import ArrayList from "./elements/ArrayList.svelte";
  import TargetHud from "./elements/targethud/TargetHud.svelte";
  import Notifications from "./elements/notifications/Notifications.svelte";
  import TabGui from "./elements/tabgui/TabGui.svelte";
  import HealthBar from "./elements/hotbar/HealthBar.svelte";
  import Scoreboard from "./elements/Scoreboard.svelte";
  import Watermark from "./elements/Watermark.svelte";
  import Logo from "./elements/Logo.svelte";
  import Information from "./elements/Information.svelte";
  import ItemColumnHUD from "./elements/inventory/ItemColumnHUD.svelte"
  import ItemColumn from "./elements/hotbar/ItemColumn.svelte"
  import Keystrokes from "./elements/keystrokes/Keystrokes.svelte";
  import Effects from "./elements/Effects.svelte";
  import BlockCounter from "./elements/BlockCounter.svelte";
  import ArmorItems from "./elements/inventory/ArmorItems.svelte";
  import InventoryContainer from "./elements/inventory/InventoryContainer.svelte";
  import CraftingInput from "./elements/inventory/CraftingInput.svelte";
  import Text from "./elements/Text.svelte";
  import Island from "./elements/island/Island.svelte";
  import type { Component } from "../../integration/types";
  import type {ComponentsUpdateEvent} from "../../integration/events";
  import StatusBar from "./elements/hotbar/StatusBar.svelte";
  import Message from "./elements/hotbar/Message.svelte";
  import KeyBinds from "./elements/KeyBinds.svelte";
  import MotionGraph from "./elements/MotionGraph.svelte";
  import TitleControl from "./elements/TitleControl.svelte";
  import SFZ from "./elements/targethud/Mode/SFZ.svelte";
  import SessionInfo from "./elements/SessionInfo.svelte";
  import { getComponents} from "../../integration/rest";
  import { listen } from "../../integration/ws";
  import PlayerListHUD from "./elements/PlayerListHUD.svelte";
  import ChatHUD from "./elements/chat/Chat.svelte";

  const baseResolution = { width: 1920, height: 1080 };
  let hudZoom = 100;
  let components: Component[] = [];
  const MIN_COEFF = 0.1337;
  const gameScale = 50 * 2;
  const MIN_ASPECT_RATIO = 2;

  function calcResolutionCoefficient() {
      const w = window.innerWidth;
      const h = window.innerHeight;
      const currentAspect = w / h;

      const wRatio = w / baseResolution.width;
      const hRatio = h / baseResolution.height;

      let coeff = Math.min(wRatio, hRatio);

      if (currentAspect < MIN_ASPECT_RATIO) {
          coeff = Math.max(coeff, 0.45);
      }

      return Math.min(1, Math.max(MIN_COEFF, coeff));
  }
  async function updateZoom(): Promise<void> {
    hudZoom = gameScale * calcResolutionCoefficient();
  }
  async function preloadComponents() {
      components = await getComponents();
  }
  onMount(() => {
      updateZoom();
      preloadComponents();
      window.addEventListener("resize", updateZoom);
      return () => window.removeEventListener("resize", updateZoom);
  });

  listen("componentsUpdate", (e: ComponentsUpdateEvent) => {
    components = [];
    components = e.components;
  });

</script>

    <div class="hud" style="zoom: {hudZoom}%;">
  <div class="hud">
    {#each components as c (c.name)}
      {#if c.settings.enabled}
        <div style="{c.settings.alignment}">
          {#if c.name === "Watermark"}         <Watermark/>
          {:else if c.name === "Island"}       <Island/>
          {:else if c.name === "Logo"}         <Logo/>
          {:else if c.name === "ArrayList"}    <ArrayList/>
          {:else if c.name === "TabGui"}       <TabGui/>
          {:else if c.name === "Notifications"}<Notifications/>
          {:else if c.name === "TargetHud"}    <TargetHud/>
          {:else if c.name === "HealthBar"}    <HealthBar/>
          {:else if c.name === "BlockCounter"} <BlockCounter/>
          {:else if c.name === "PlayerListHUD"}<PlayerListHUD/>
          {:else if c.name === "Scoreboard"}   <Scoreboard/>
          {:else if c.name === "ArmorItems"}   <ArmorItems/>
          {:else if c.name === "SessionInfo"}  <SessionInfo/>
          {:else if c.name === "ChatHUD"}      <ChatHUD/>
          {:else if c.name === "Inventory"}    <InventoryContainer/>
          {:else if c.name === "CraftingInput"}<CraftingInput/>
          {:else if c.name === "Information"}  <Information/>
          {:else if c.name === "KeyBinds"}     <KeyBinds/>
          {:else if c.name === "Keystrokes"}   <Keystrokes/>
          {:else if c.name === "MotionGraph"}  <MotionGraph/>
          {:else if c.name === "Effects"}      <Effects/>
          {:else if c.name === "Message"}      <Message/>
          {:else if c.name === "Status"}       <StatusBar/>
          {:else if c.name === "TitleControl"} <TitleControl/>
          {:else if c.name === "ItemColumn"}   <ItemColumn/>
          {:else if c.name === "ItemColumnHUD"}<ItemColumnHUD/>
          {:else if c.name === "SFZ"}          <SFZ/>
          {:else if c.name === "Text"}        <Text settings={c.settings}/>
          {:else if c.name === "Image"}       <img alt="" src="{c.settings.src}" style="scale: {c.settings.scale};"/>
          {/if}
        </div>
      {/if}
    {/each}
  </div>
</div>


<style lang="scss">
  @import "../../colors.scss";
  .hud {
    height: 100vh;
    width: 100vw;
  }
</style>

