<script>
    import { fade } from "svelte/transition";

</script>

<img class="rat" src="img/rat.png" alt="rat" in:fade out:fade={{duration: 100}}>

<style>
    .rat {
        position: absolute;
        bottom: 15px;
        right: 15px;
        width: 185px;
    }
</style>
