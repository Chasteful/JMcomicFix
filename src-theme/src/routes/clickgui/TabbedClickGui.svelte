<script lang="ts">
    import ClickGui from "./ClickGui.svelte";
    import GlobalSettings from "./tabs/GlobalSettings.svelte";
    import Tabs from "./tabs/Tabs.svelte";
    import {gridSize, os, scaleFactor, showGrid, snappingEnabled, panelLength,fontSize} from "./clickgui_store";
    import type {ConfigurableSetting, TogglableSetting} from "../../integration/types";
    import {onDestroy, onMount} from "svelte";
    import {getClientInfo, getGameWindow, getModuleSettings, setTyping} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import type {ClickGuiValueChangeEvent, ScaleFactorChangeEvent} from "../../integration/events";
    import {Resolution_utils} from "../../util/resolution_utils";

    const tabs = [
        {title: "ClickGUI", content: ClickGui},
        {title: "Settings", content: GlobalSettings}
    ];


    let resolutionScaler = new Resolution_utils({
        baseResolution: {width: 1920, height: 1080}
    });

    let activeTab = $state(0);
    let minecraftScaleFactor = $state(2);
    let clickGuiScaleFactor = $state(1);
    let panelLengthFactor = $state(64)
    let fontSizeFactor = $state(14)

    $effect(() => {
        updateScaleFactor()
        $panelLength = panelLengthFactor
        $fontSize = fontSizeFactor

    });
    function updateScaleFactor() {
        $scaleFactor =
            minecraftScaleFactor *
            clickGuiScaleFactor *
            resolutionScaler.getScaleFactor();
    }
    function applyValues(configurable: ConfigurableSetting) {
        const scaleValue = configurable.value.find(v => v.name === "Scale");
        const panelLength= configurable.value.find(v => v.name === "Length")
        const fontSize = configurable.value.find(v => v.name === "FontSize")
        const snappingValue = configurable.value.find(v => v.name === "Snapping") as TogglableSetting | undefined;

        if (scaleValue) {
            clickGuiScaleFactor = scaleValue.value as number;
        }

        if (panelLength) {
            panelLengthFactor = panelLength.value as number;
        }
        if (fontSize) {
            fontSizeFactor = fontSize.value as number
        }
        if (snappingValue) {
            $snappingEnabled = snappingValue.value.find(v => v.name === "Enabled")?.value as boolean ?? true;
            $gridSize = snappingValue.value.find(v => v.name === "GridSize")?.value as number ?? 10;
        }
    }

    fontSize.subscribe((fontSize) => {
        if (typeof document === 'undefined') return;
        document.documentElement.style.setProperty('--font-size', `${fontSize}px`);
    });

    const handleResize = () => {
        requestAnimationFrame(() => {
            resolutionScaler.updateScaleFactor();
            updateScaleFactor()
        });
    };

    onMount(async () => {

        resolutionScaler.updateScaleFactor();
        updateScaleFactor()

        $os = (await getClientInfo()).os;
        const gameWindow = await getGameWindow();
        minecraftScaleFactor = gameWindow.scaleFactor;

        const clickGuiSettings = await getModuleSettings("ClickGUI");
        applyValues(clickGuiSettings);

        await setTyping(false);

        window.addEventListener("resize", handleResize);

    });

    onDestroy(() => {
        window.removeEventListener("resize", handleResize);
    });

    listen("scaleFactorChange", (e: ScaleFactorChangeEvent) => {
        minecraftScaleFactor = e.scaleFactor;
    });

    listen("clickGuiValueChange", (e: ClickGuiValueChangeEvent) => {
        applyValues(e.configurable);
    });
</script>

<div
        class="tabbed-clickgui"
        class:grid={$showGrid}
        style="
    transform: scale({$scaleFactor * 50}%);
    width: {2 / $scaleFactor * 100}vw;
    height: {2 / $scaleFactor * 100}vh;
    background-size: {$gridSize}px {$gridSize}px;
  "
>
    <Tabs {tabs} bind:activeTab/>
</div>

<style lang="scss">

  $GRID_SIZE: 10px;

  .tabbed-clickgui {
    background-color: var(--clickgui-overlay-background-color);
    overflow: hidden;
    position: absolute;
    will-change: opacity;
    transform-origin: top left;
    left: 0;
    top: 0;

    &.grid {
      background-image: linear-gradient(to right, var(--clickgui-grid-color) 1px, transparent 1px),
      linear-gradient(to bottom, var(--clickgui-grid-color) 1px, transparent 1px);
    }
  }
</style>
