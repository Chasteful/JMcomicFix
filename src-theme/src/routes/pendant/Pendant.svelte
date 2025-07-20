<script lang="ts">
    import {fade} from "svelte/transition";
    import {getModules, getModuleSettings} from "../../integration/rest.js";
    import {onMount} from "svelte";
    import type {ConfigurableSetting} from "../../integration/types.js";

    let shouldShow = false;
    let isLoading = true;
    let ScaleFactor = 1;
    let AlphaFactor = 255;
    let CustomURL: string | null = null;
    $: opacityValue = AlphaFactor / 255;

    async function checkShouldShow(): Promise<void> {
        const modules = await getModules();
        shouldShow = modules.some(module =>
            module.name === "Pendant" && module.enabled
        );
    }

    const applyValues = (configurable: ConfigurableSetting) => {
        ScaleFactor = configurable.value.find(v => v.name === "Scale")?.value as number ?? 1;
        AlphaFactor = configurable.value.find(v => v.name === "Alpha")?.value as number ?? 255;
        const urlSetting = configurable.value.find(v => v.name === "CustomURL");
        CustomURL = urlSetting?.value as string ?? null;
        isLoading = false;
    };

    onMount(async () => {
        await checkShouldShow();
        if (shouldShow) {
            const clickGuiSettings = await getModuleSettings("Pendant");
            applyValues(clickGuiSettings);
        } else {
            isLoading = false;
        }
    });
</script>

{#if !isLoading && shouldShow}
    <div style="--opacity: {opacityValue}">
        <img class="rat"
             src={CustomURL || "img/rat.png"}
             alt={CustomURL ? "custom image" : "rat"}
             in:fade={{ duration: 200 }}
             out:fade={{ duration: 200 }}
             style="scale: {ScaleFactor}">
    </div>
{/if}

<style>
    .rat {
        position: absolute;
        bottom: 15px;
        right: 15px;
        width: 185px;
        will-change: opacity;
        opacity: var(--opacity);
    }
</style>
