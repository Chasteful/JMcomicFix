<script lang="ts">
    import GlitchedLayers from '../common/VFX/GlitchedLayers.svelte';
    import ClientLogo from '../common/ClientLogo.svelte';

    export let settings: { [name: string]: any };
</script>
{#if settings?.glitched}
    <GlitchedLayers size={262}>
        <ClientLogo gradient={settings?.gradient} current={true}/>
    </GlitchedLayers>
{:else}
    <ClientLogo gradient={settings?.gradient} current={true}/>
{/if}
