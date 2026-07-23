<script lang="ts">
    import {onMount, setContext} from "svelte";
    import ArrayList from "./elements/arrayList/ArrayList.svelte";
    import Notifications from "./elements/notifications/Notifications.svelte";
    import TabGui from "./elements/tabgui/TabGui.svelte";
    import Scoreboard from "./elements/Scoreboard.svelte";
    import Watermark from "./elements/watermark/Watermark.svelte";
    import Logo from "./elements/Logo.svelte";
    import Information from "./elements/Information.svelte";
    import ItemColumn from "./elements/inventory/ItemColumnHUD.svelte";
    import HotBar from "./elements/hotbar/HotBar.svelte";
    import Keystrokes from "./elements/keystrokes/Keystrokes.svelte";
    import BlockCounter from "./elements/BlockCounter.svelte";
    import ArmorItems from "./elements/ArmorItems.svelte";
    import InventoryContainer from "./elements/inventory/InventoryContainer.svelte";
    import Text from "./elements/Text.svelte";
    import Island from "./elements/island/Island.svelte";
    import StatusBar from "./elements/statusBar/StatusBar.svelte";
    import Message from "./elements/Message.svelte";
    import KeyBinds from "./elements/KeyBinds.svelte";
    import MotionGraph from "./elements/MotionGraph.svelte";
    import TitleControl from "./elements/TitleControl.svelte";
    import SessionInfo from "./elements/sessioninfo/SessionInfo.svelte";
    import PlayerList from "./elements/PlayerList.svelte";
    import ChatHUD from "./elements/chat/Chat.svelte";
    import type {
        HudComponent,
        Metadata,
    } from "../../integration/types";
    import type {ComponentsUpdateEvent, ScaleFactorChangeEvent} from "../../integration/events";
    import {
        getClientInfo,
        getComponents,
        getGameWindow,
        getMetadata,
        getNativeComponents
    } from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import {ScaleFactor} from "./Hud_store";
    import {hudScaleFactor} from "../../theme/theme_manager";
    import { os } from "../clickgui/clickgui_store";
    import {resolutionCoefficient} from "../../util/resolution_utils";
    import ProgressBar from "./elements/ProgressBar.svelte";
    import SilentHand from "./elements/SilentHand.svelte";
    import TargetHud from "./elements/targethud/TargetHud.svelte";
    import Effects from "./elements/effects/Effects.svelte";
    import DraggableComponent from "./elements/DraggableComponent.svelte";
    import HealthBar from "./elements/healthhud/HealthBar.svelte";
    import ClosedCaptions from "./elements/ClosedCaptions.svelte";
    import GenericPlayerInventory from "./elements/inventory/GenericPlayerInventory.svelte";
    import InventoryStatistics from "./elements/inventory/InventoryStatistics.svelte";
    import {
        HUD_EDITOR_ELEMENTS_CONTEXT,
        type HudEditorDragState
    } from "../clickgui/tabs/hud_editor/constants";
    import Image from "./elements/Image.svelte";

    export let inEditor = false;
    export let onDragStateChange: ((state: HudEditorDragState) => void) | undefined = undefined;
    export let magneticTargetIds: string[] = [];

    let zoom = 100;
    let metadata: Metadata;
    let nativeComponents: HudComponent[] = [];
    let themeComponents: HudComponent[] = [];

    $: renderedComponents = inEditor ? [...nativeComponents, ...themeComponents] : themeComponents;
    $: ScaleFactor.set($hudScaleFactor * $resolutionCoefficient);

    async function updateZoom(): Promise<void> {
        $ScaleFactor = $hudScaleFactor * $resolutionCoefficient;
    }
    setContext(HUD_EDITOR_ELEMENTS_CONTEXT, new Map<string, HTMLElement>());

    onMount(() => {
        const cleanup = () => window.removeEventListener("resize", updateZoom);

        (async () => {
            $os = (await getClientInfo()).os;
            const gameWindow = await getGameWindow();
            zoom = gameWindow.scaleFactor * 50;
            await updateZoom();
            metadata = await getMetadata();
            [nativeComponents, themeComponents] = await Promise.all([
                inEditor ? getNativeComponents() : Promise.resolve([]),
                getComponents(metadata.id)
            ]);
            window.addEventListener("resize", updateZoom);
        })();

        return cleanup;
    });

    listen("scaleFactorChange", (data: ScaleFactorChangeEvent) => {
        zoom = data.scaleFactor * 50;
    });

    listen("componentsUpdate", (event: ComponentsUpdateEvent) => {
        if (inEditor && event.source === "native") {
            nativeComponents = event.components;
        }

        if (event.source === "theme" && event.themeId === metadata?.id) {
            themeComponents = event.components;
        }
    });
</script>

<div class="hud-resize-with-resolution" style="--hud-zoom: {$ScaleFactor}">
    {#each renderedComponents as c (c.id)}
        {#if c.settings.enabled}
            <DraggableComponent
                    {inEditor}
                    {onDragStateChange}
                    componentId={c.id}
                    componentName={c.name}
                    alignment={c.settings.alignment}
                    zIndex={c.settings.zIndex ?? 0}
                    magneticallyReferenced={magneticTargetIds.includes(c.id)}
                    width={c.width}
                    height={c.height}
            >
            {#if c.name === 'Text'}
                <Text settings={c.settings}/>
            {:else if c.name === 'Effects'}
                <Effects settings={c.settings}/>
            {:else if c.name === 'Image'}
                <img alt="" src={c.settings.uRL} style="transform: scale({c.settings.scale});"/>
            {:else if c.name === 'ArmorItems'}
                <ArmorItems settings={c.settings}/>
            {:else if c.name === 'ArrayList'}
                <ArrayList settings={c.settings}/>
            {:else if c.name === 'BlockCounter'}
                <BlockCounter settings={c.settings}/>
            {:else if c.name === 'ChatHUD'}
                <ChatHUD settings={c.settings}/>
            {:else if c.name === "ClosedCaptions"}
                <ClosedCaptions/>
            {:else if c.name === "CraftingInventory"}
                <GenericPlayerInventory
                        settings={c.settings}
                        rowLength={2} getRenderedStacks={it => it.crafting} />
            {:else if c.name === "EnderChestInventory"}
                <GenericPlayerInventory
                        settings={c.settings}
                        rowLength={9} getRenderedStacks={it => it.enderChest} />
            {:else if c.name === 'Information'}
                <Information settings={c.settings}/>
            {:else if c.name === 'Inventory'}
                <InventoryContainer settings={c.settings}/>
            {:else if c.name === "InventoryStatistics"}
                <InventoryStatistics settings={c.settings} />
            {:else if c.name === 'Island'}
                <Island settings={c.settings}/>
            {:else if c.name === 'ItemColumn'}
                <ItemColumn settings={c.settings}/>
            {:else if c.name === 'KeyBinds'}
                <KeyBinds settings={c.settings}/>
            {:else if c.name === 'Keystrokes'}
                <Keystrokes settings={c.settings}/>
            {:else if c.name === 'Logo'}
                <Logo settings={c.settings}/>
            {:else if c.name === 'MotionGraph'}
                <MotionGraph settings={c.settings}/>
            {:else if c.name === 'Notifications'}
                <Notifications settings={c.settings}/>
            {:else if c.name === 'ProgressBar'}
                <ProgressBar settings={c.settings}/>
            {:else if c.name === 'PlayerList'}
                <PlayerList settings={c.settings}/>
            {:else if c.name === 'Scoreboard'}
                <Scoreboard settings={c.settings}/>
            {:else if c.name === 'SessionInfo'}
                <SessionInfo settings={c.settings}/>
            {:else if c.name === 'SilentHand'}
                <SilentHand settings={c.settings}/>
            {:else if c.name === 'StatusBar'}
                <StatusBar settings={c.settings}/>
            {:else if c.name === 'TabGui'}
                <TabGui settings={c.settings}/>
            {:else if c.name === 'TargetHud'}
                <TargetHud settings={c.settings}/>
            {:else if c.name === 'TitleControl'}
                <TitleControl settings={c.settings}/>
            {:else if c.name === 'Watermark'}
                <Watermark settings={c.settings}/>
            {:else if c.width !== undefined && c.height !== undefined}
                <div></div>
            {/if}
            </DraggableComponent>
        {/if}
    {/each}
</div>

<div class="hud-resize-with-game" style="zoom: {zoom}%">
    {#each renderedComponents as c (c.id)}
        {#if c.settings.enabled}
            <DraggableComponent
                    {inEditor}
                    {onDragStateChange}
                    componentId={c.id}
                    componentName={c.name}
                    alignment={c.settings.alignment}
                    magneticallyReferenced={magneticTargetIds.includes(c.id)}
                    width={c.width}
                    height={c.height}
            >
                {#if c.name === 'HealthBar'}
                    <HealthBar settings={c.settings}/>
                {:else if c.name === 'Hotbar'}
                    <HotBar settings={c.settings}/>
                {:else if c.name === 'Message'}
                    <Message settings={c.settings}/>
                {:else if c.width !== undefined && c.height !== undefined}
                    <div></div>
                {/if}
            </DraggableComponent>
        {/if}
    {/each}
</div>

<style lang="scss">
  .hud-resize-with-resolution {
    height: 100vh;
    width: 100vw;
    zoom: var(--hud-zoom);
  }
  .hud-resize-with-game {
    height: 100vh;
    width: 100vw;
  }
</style>
