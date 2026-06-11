<script lang="ts">

    import Ambient_SessionInfo from "./mode/Ambient_SessionInfo.svelte";
    import Novoline_SeesionInfo from "./mode/Novoline_SeesionInfo.svelte";

    export let settings: { [name: string]: any };

    const modes: Record<string, any> = {
        Ambient: Ambient_SessionInfo,
        Simple: Novoline_SeesionInfo,
    };

    let ModeComponent: any;
    $: ModeComponent = modes[settings?.mode] ?? Ambient_SessionInfo;
</script>
<div style="transform: scale({settings.scale});">
    <svelte:component this={ModeComponent} {settings}/>
</div>
