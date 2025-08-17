<script lang="ts">
    import ModernHotBar from "./mode/Modern_HotBar.svelte";
    import RiseHotBar from "./mode/Rise_HotBar.svelte";
    import DefaultHotBar from "./mode/Default_HotBar.svelte";
    export let settings: { [name: string]: any };

    const modes: Record<string, any> = {
        Modern: ModernHotBar,
        Rise:RiseHotBar,
        Default:DefaultHotBar
    };

    let ModeComponent: any;

    $: ModeComponent = modes[settings?.mode] ?? ModernHotBar;
</script>

<svelte:component this={ModeComponent} {settings} />
