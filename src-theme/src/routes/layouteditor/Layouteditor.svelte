<script lang="ts">
    export let showBackground: boolean;
    import Hud from "../hud/Hud.svelte";
    import Background from "../menu/common/Background.svelte";

</script>

    <Background showBackground={showBackground}/>
    <Hud/>



