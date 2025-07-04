<script lang="ts">
    import {onDestroy, onMount} from "svelte";
    import ArrayList from "./elements/arrayList/ArrayList.svelte";
    import TargetHud from "./elements/targethud/TargetHud.svelte";
    import Notifications from "./elements/notifications/Notifications.svelte";
    import TabGui from "./elements/tabgui/TabGui.svelte";
    import HealthBar from "./elements/HealthBar.svelte";
    import Scoreboard from "./elements/Scoreboard.svelte";
    import Watermark from "./elements/watermark/Watermark.svelte";
    import Logo from "./elements/Logo.svelte";
    import Information from "./elements/Information.svelte";
    import ItemColumnHUD from "./elements/inventory/ItemColumnHUD.svelte";
    import ItemColumn from "./elements/itemcolumn/ItemColumn.svelte";
    import Keystrokes from "./elements/keystrokes/Keystrokes.svelte";
    import Effects from "./elements/Effects.svelte";
    import BlockCounter from "./elements/BlockCounter.svelte";
    import ArmorItems from "./elements/inventory/ArmorItems.svelte";
    import InventoryContainer from "./elements/inventory/InventoryContainer.svelte";
    import CraftingInput from "./elements/inventory/CraftingInput.svelte";
    import Text from "./elements/Text.svelte";
    import Island from "./elements/island/Island.svelte";
    import StatusBar from "./elements/StatusBar/StatusBar.svelte";
    import Message from "./elements/Message.svelte";
    import KeyBinds from "./elements/KeyBinds.svelte";
    import MotionGraph from "./elements/MotionGraph.svelte";
    import TitleControl from "./elements/TitleControl.svelte";
    import SFZ from "./elements/targethud/Mode/SFZ.svelte";
    import SessionInfo from "./elements/SessionInfo.svelte";
    import PlayerListHUD from "./elements/PlayerListHUD.svelte";
    import ChatHUD from "./elements/chat/Chat.svelte";
    import type {Component, ConfigurableSetting, TogglableSetting} from "../../integration/types";
    import type {ClickGuiValueChangeEvent, ComponentsUpdateEvent} from "../../integration/events";
    import {getComponents, getModuleSettings} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import LayoutEditor from "./LayoutEditor.svelte";
    import Vignette from "./elements/Vignette.svelte";
    import {gridSize, snappingEnabled, showGrid, hudZoom} from "./Hud_store";
    import {WindowSize} from "../../util/WindowSize";

    const {width, height, destroy} = WindowSize();
    const baseResolution = {width: 1920, height: 1080};

    let hudZoomFactor = 1
    let components: Component[] = [];

    const MIN_ZOOM = 0.1337;
    const gameScale = 50 * 2;
    const MIN_ASPECT_RATIO = 2;

    $: {
        hudZoom.set(gameScale * hudZoomFactor * calcResolutionCoefficient())
    }
    type ComponentWrapperParams = {
        component: Component;

    };
    const applyValues = (configurable: ConfigurableSetting) => {

        hudZoomFactor = configurable.value.find(v => v.name === "Zoom")?.value as number ?? 1;
        const snappingValue = configurable.value.find(v => v.name === "Snapping") as TogglableSetting;
        $snappingEnabled = snappingValue?.value.find(v => v.name === "Enabled")?.value as boolean ?? true;
        $gridSize = snappingValue?.value.find(v => v.name === "GridSize")?.value as number ?? 10;

    };

    function calcResolutionCoefficient() {
        const w = window.innerWidth;
        const h = window.innerHeight;
        const currentAspect = w / h;

        const wRatio = w / baseResolution.width;
        const hRatio = h / baseResolution.height;

        let min = Math.min(wRatio, hRatio);

        if (currentAspect < MIN_ASPECT_RATIO) {
            min = Math.max(min, 0.45);
        }

        return Math.min(1, Math.max(MIN_ZOOM, min));
    }

    async function updateZoom(): Promise<void> {
        $hudZoom = gameScale * hudZoomFactor * calcResolutionCoefficient();
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
        const cleanup = () => window.removeEventListener("resize", updateZoom);

        (async () => {
            await updateZoom();
            const clickGuiSettings = await getModuleSettings("HudLayoutEditor");
            applyValues(clickGuiSettings);
            await preloadComponents();
            window.addEventListener("resize", updateZoom);
        })();

        return cleanup;
    });

    onDestroy(destroy);

    listen("componentsUpdate", (e: ComponentsUpdateEvent) => {
        components = e.components;
    });
    listen("clickGuiValueChange", (e: ClickGuiValueChangeEvent) => {
        applyValues(e.configurable);
    });
</script>
<div class="snap" class:grid={$showGrid}
     style="background-size: {$gridSize}px {$gridSize}px;width: { $width }px; height: { $height }px;">
    <div class="hud" style="--hud-zoom: {$hudZoom / 100}">
        {#each components as c (c.name)}
            {#if c.settings.enabled}
                <div style={c.settings.alignment}>
                    {@render componentWrapper({component: c})}
                </div>
            {/if}
        {/each}
    </div>
</div>
{#snippet componentWrapper({component}: ComponentWrapperParams)}
    {#if component.name === 'Text'}
        <Text settings={component.settings}/>
    {:else if component.name === 'Image'}
        <img alt="" src={component.settings.src} style="scale: {component.settings.scale};"/>
    {:else}
        <LayoutEditor
                componentId={component.name.toLowerCase()}
                defaultPosition={{ x: component.settings.x ?? 0, y: component.settings.y ?? 0 }}
        >
            {#if component.name === 'Watermark'}
                <Watermark/>
            {/if}
            {#if component.name === 'ArrayList'}
                <ArrayList/>
            {/if}
            {#if component.name === 'TabGui'}
                <TabGui/>
            {/if}
            {#if component.name === 'Island'}
                <Island/>
            {/if}
            {#if component.name === 'Logo'}
                <Logo/>
            {/if}
            {#if component.name === 'Notifications'}
                <Notifications/>
            {/if}
            {#if component.name === 'TargetHud'}
                <TargetHud/>
            {/if}
            {#if component.name === 'HealthBar'}
                <HealthBar/>
            {/if}
            {#if component.name === 'BlockCounter'}
                <BlockCounter/>
            {/if}
            {#if component.name === 'PlayerListHUD'}
                <PlayerListHUD/>
            {/if}
            {#if component.name === 'Scoreboard'}
                <Scoreboard/>
            {/if}
            {#if component.name === 'ArmorItems'}
                <ArmorItems/>
            {/if}
            {#if component.name === 'SessionInfo'}
                <SessionInfo/>
            {/if}
            {#if component.name === 'ChatHUD'}
                <ChatHUD/>
            {/if}
            {#if component.name === 'InventoryContainer'}
                <InventoryContainer/>
            {/if}
            {#if component.name === 'CraftingInput'}
                <CraftingInput/>
            {/if}
            {#if component.name === 'Information'}
                <Information/>
            {/if}
            {#if component.name === 'KeyBinds'}
                <KeyBinds/>
            {/if}
            {#if component.name === 'Keystrokes'}
                <Keystrokes/>
            {/if}
            {#if component.name === 'MotionGraph'}
                <MotionGraph/>
            {/if}
            {#if component.name === 'Effects'}
                <Effects/>
            {/if}
            {#if component.name === 'Message'}
                <Message/>
            {/if}
            {#if component.name === 'StatusBar'}
                <StatusBar/>
            {/if}
            {#if component.name === 'TitleControl'}
                <TitleControl/>
            {/if}
            {#if component.name === 'ItemColumn'}
                <ItemColumn/>
            {/if}
            {#if component.name === 'ItemColumnHUD'}
                <ItemColumnHUD/>
            {/if}
            {#if component.name === 'SFZ'}
                <SFZ/>
            {/if}
        </LayoutEditor>
    {/if}
{/snippet}
{#each components as c (c.name)}
    {#if c.name === 'Vignette' && c.settings.enabled}
        <Vignette/>
    {/if}
{/each}


<style lang="scss">
  @import "../../colors.scss";

  $GRID_SIZE: 10px;
  .hud {
    height: 100vh;
    width: 100vw;
    zoom: var(--hud-zoom);
  }

  .snap {
    overflow: hidden;
    position: absolute;
    will-change: opacity;
    top: 0;
    left: 0;
    transform-origin: left top;

    &.grid {
      background-image: linear-gradient(to right, rgba($clickgui-grid-color, 0.3) 1px, transparent 1px),
      linear-gradient(to bottom, rgba($clickgui-grid-color, 0.3) 1px, transparent 1px);
      background-size: #{$GRID_SIZE}px #{$GRID_SIZE}px;
    }
  }
</style>
