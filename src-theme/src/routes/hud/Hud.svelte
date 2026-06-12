<script lang="ts">
    import {onDestroy, onMount} from "svelte";
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
    import type {ComponentsUpdateEvent} from "../../integration/events";
    import {getClientInfo, getComponents, getMetadata} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import {ScaleFactor} from "./Hud_store";
    import {hudScaleFactor} from "../../theme/theme_manager";
    import { os } from "../clickgui/clickgui_store";
    import {calcResolutionCoefficient} from "../../util/resolution_utils";
    import ProgressBar from "./elements/ProgressBar.svelte";
    import SilentHand from "./elements/SilentHand.svelte";
    import TargetHud from "./elements/targethud/TargetHud.svelte";
    import Effects from "./elements/effects/Effects.svelte";
    import DraggableComponent from "./elements/DraggableComponent.svelte";
    import HealthBar from "./elements/healthhud/HealthBar.svelte";
    import GenericPlayerInventory from "./elements/inventory/GenericPlayerInventory.svelte";
    import InventoryStatistics from "./elements/inventory/InventoryStatistics.svelte";

    let metadata: Metadata;
    let components: HudComponent[] = [];
    $: ScaleFactor.set($hudScaleFactor * calcResolutionCoefficient());

    async function updateZoom(): Promise<void> {
        $ScaleFactor = $hudScaleFactor * calcResolutionCoefficient();
    }

    onMount(() => {
        const cleanup = () => window.removeEventListener("resize", updateZoom);

        (async () => {
            $os = (await getClientInfo()).os;
            await updateZoom();
            metadata = await getMetadata();
            components = await getComponents(metadata.id);
            window.addEventListener("resize", updateZoom);
        })();

        return cleanup;
    });


    listen("componentsUpdate", (data: ComponentsUpdateEvent) => {
        if (data.id != metadata.id) {
            // reject
            return;
        }

        // force update to re-render
        components = [];
        components = data.components;
    });
</script>

<div class="hud" style="--hud-zoom: {$ScaleFactor}">
    {#each components as c}
        {#if c.settings.enabled}
            <DraggableComponent alignment={c.settings.alignment} >
            {#if c.name === 'Text'}
                <Text settings={c.settings}/>
            {:else if c.name === 'Hotbar'}
                <HotBar settings={c.settings}/>
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
            {:else if c.name === "CraftingInventory"}
                <GenericPlayerInventory
                        settings={c.settings}
                        rowLength={2} getRenderedStacks={it => it.crafting} />
            {:else if c.name === "EnderChestInventory"}
                <GenericPlayerInventory
                        settings={c.settings}
                        rowLength={9} getRenderedStacks={it => it.enderChest} />
            {:else if c.name === 'HealthBar'}
                <HealthBar settings={c.settings}/>
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
            {:else if c.name === 'Message'}
                <Message settings={c.settings}/>
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
            {/if}
            </DraggableComponent>
        {/if}
    {/each}
</div>

<style lang="scss">
  .hud {
    height: 100vh;
    width: 100vw;
    zoom: var(--hud-zoom);
  }
</style>
