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
    import ItemColumnHUD from "./elements/inventory/ItemColumnHUD.svelte";
    import ItemColumn from "./elements/hotbar/ItemColumn.svelte";
    import Keystrokes from "./elements/keystrokes/Keystrokes.svelte";
    import Effects from "./elements/Effects.svelte";
    import BlockCounter from "./elements/BlockCounter.svelte";
    import ArmorItems from "./elements/inventory/ArmorItems.svelte";
    import InventoryContainer from "./elements/inventory/InventoryContainer.svelte";
    import CraftingInput from "./elements/inventory/CraftingInput.svelte";
    import Text from "./elements/Text.svelte";
    import Island from "./elements/island/Island.svelte";
    import StatusBar from "./elements/hotbar/StatusBar.svelte";
    import Message from "./elements/hotbar/Message.svelte";
    import KeyBinds from "./elements/KeyBinds.svelte";
    import MotionGraph from "./elements/MotionGraph.svelte";
    import TitleControl from "./elements/TitleControl.svelte";
    import SFZ from "./elements/targethud/Mode/SFZ.svelte";
    import SessionInfo from "./elements/SessionInfo.svelte";
    import PlayerListHUD from "./elements/PlayerListHUD.svelte";
    import ChatHUD from "./elements/chat/Chat.svelte";
    import type { Component } from "../../integration/types";
    import type { ComponentsUpdateEvent } from "../../integration/events";
    import { getComponents } from "../../integration/rest";
    import { listen } from "../../integration/ws";
    import LayoutEditor from "../layouteditor/LayoutEditor.svelte";

    const baseResolution = { width: 1920, height: 1080 };
    let hudZoom = 100;
    let components: Component[] = [];
    const MIN_COEFF = 0.1337;
    const gameScale = 50 * 2;
    const MIN_ASPECT_RATIO = 2;

    type ComponentWrapperParams = {
        component: Component;
        hudZoom: number;
    };

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
        const serverComponents = await getComponents();


        for (const component of serverComponents) {
            const key = `hud-pos-${component.name.toLowerCase()}`;
            if (!localStorage.getItem(key)) {

                localStorage.setItem(key, JSON.stringify({
                    x: component.settings.x ?? 0,
                    y: component.settings.y ?? 0,
                }));
            }
        }

        components = serverComponents;
    }


    onMount(() => {
        updateZoom();
        preloadComponents();
        window.addEventListener("resize", updateZoom);
        return () => window.removeEventListener("resize", updateZoom);
    });

    listen("componentsUpdate", (e: ComponentsUpdateEvent) => {
        components = e.components;
    });
</script>

{#snippet componentWrapper({ component, hudZoom }: ComponentWrapperParams)}
    {#if component.name === 'Text'}
        <Text settings={component.settings} />
    {:else if component.name === 'Image'}
        <img alt="" src={component.settings.src} style="scale: {component.settings.scale};" />
    {:else}
        <LayoutEditor
                componentId={component.name.toLowerCase()}
                hudZoom={hudZoom}
                defaultPosition={{ x: component.settings.x ?? 0, y: component.settings.y ?? 0 }}
        >
            {#if component.name === 'Watermark'}<Watermark />{/if}
            {#if component.name === 'ArrayList'}<ArrayList />{/if}
            {#if component.name === 'TabGui'}<TabGui />{/if}
            {#if component.name === 'Island'}<Island />{/if}
            {#if component.name === 'Logo'}<Logo />{/if}
            {#if component.name === 'Notifications'}<Notifications />{/if}
            {#if component.name === 'TargetHud'}<TargetHud />{/if}
            {#if component.name === 'HealthBar'}<HealthBar />{/if}
            {#if component.name === 'BlockCounter'}<BlockCounter />{/if}
            {#if component.name === 'PlayerListHUD'}<PlayerListHUD />{/if}
            {#if component.name === 'Scoreboard'}<Scoreboard />{/if}
            {#if component.name === 'ArmorItems'}<ArmorItems />{/if}
            {#if component.name === 'SessionInfo'}<SessionInfo />{/if}
            {#if component.name === 'ChatHUD'}<ChatHUD />{/if}
            {#if component.name === 'InventoryContainer'}<InventoryContainer />{/if}
            {#if component.name === 'CraftingInput'}<CraftingInput />{/if}
            {#if component.name === 'Information'}<Information />{/if}
            {#if component.name === 'KeyBinds'}<KeyBinds />{/if}
            {#if component.name === 'Keystrokes'}<Keystrokes />{/if}
            {#if component.name === 'MotionGraph'}<MotionGraph />{/if}
            {#if component.name === 'Effects'}<Effects />{/if}
            {#if component.name === 'Message'}<Message />{/if}
            {#if component.name === 'StatusBar'}<StatusBar />{/if}
            {#if component.name === 'TitleControl'}<TitleControl />{/if}
            {#if component.name === 'ItemColumn'}<ItemColumn />{/if}
            {#if component.name === 'ItemColumnHUD'}<ItemColumnHUD />{/if}
            {#if component.name === 'SFZ'}<SFZ />{/if}
        </LayoutEditor>
    {/if}
{/snippet}

<div class="hud" style="zoom: {hudZoom}%;">
    {#each components as c (c.name)}
        {#if c.settings.enabled}
            <div style={c.settings.alignment}>
                {@render componentWrapper({ component: c, hudZoom })}
            </div>
        {/if}
    {/each}
</div>

<style lang="scss">
  @import "../../colors.scss";

  .hud {
    height: 100vh;
    width: 100vw;
  }
</style>
