<script lang="ts">
    import {fly} from 'svelte/transition';
    import {expoInOut} from 'svelte/easing';
    import Modules from "./Modules.svelte";

</script>

<div class="arraylist" id="arraylist"
        transition:fly|global={{ duration: 500, y: -50, easing: expoInOut }}
>
    <Modules/>
</div>

<style lang="scss">
  @import '../../../../colors';

  :root {
    --primary-color-rgb: var(--primary-color-rgb);
    --secondary-color-rgb: var(--secondary-color-rgb);
  }

  .arraylist {
    display: flex;
    top: 0;
    right: 0;
    flex-direction: column;
    align-items: flex-end;
    position: absolute;
    transform: translateZ(0);

  }

</style>
