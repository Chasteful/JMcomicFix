<!-- ChatScreen.svelte
<script lang="ts">
    import { onMount} from "svelte";
    import EventForwarder from './elements/ChatEventForwarder.svelte';
    import { getComponents} from "../../integration/rest";
    import { listen } from "../../integration/ws";
    import type {ComponentsUpdateEvent} from "../../integration/events";
    import type {Component} from "../../integration/types";

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
                    {#if c.name === "ChatHUD"}
                        <EventForwarder/>
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
-->
