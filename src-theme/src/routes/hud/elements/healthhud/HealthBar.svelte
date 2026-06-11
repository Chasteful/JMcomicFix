<script lang="ts">
    import Genshin_HealthBar from "./mode/Genshin_HealthBar.svelte";

    export let settings: { [name: string]: any };

    const modes: Record<string, any> = {
        Genshin: Genshin_HealthBar
    };

    let ModeComponent: any;

    $: ModeComponent = modes[settings?.mode] ?? Genshin_HealthBar;
</script>

<div style="transform: scale({settings.scale});">
    <svelte:component this={ModeComponent} {settings}/>
</div>
