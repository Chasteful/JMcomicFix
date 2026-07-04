<script lang="ts">
    import Header from "./header/Header.svelte";
    import {fly} from "svelte/transition";
    import {onMount} from "svelte";
    import {location} from "svelte-spa-router";
    import Background from "./Background.svelte";


    const transitionDuration = 700;
    let ready = false;

    const noAccountPaths = ["/altmanager", "/disconnected"];
    const noHeaderPaths = ["/lockscreen", "/disconnected"];
    const BackgroundPaths = ["lockscreen",];


    onMount(() => {
        setTimeout(() => {
            ready = true;
        }, transitionDuration);
    });
    $: showAccount = !noAccountPaths.includes($location);
    $: showHeader = !noHeaderPaths.includes($location);
    $: showBackground = BackgroundPaths.includes($location);

</script>

<div class="menu">
    {#if ready}
        <div transition:fly|global={{duration: 700, y: -100}}>
            <Header showAccount={showAccount}
                    showHeader={showHeader}
            />
        </div>
    {/if}

    <div class="menu-wrapper">
        <slot/>
    </div>
</div>
<Background showBackground={showBackground}/>
<style lang="scss">
  .menu {
    padding: 50px;
    display: flex;
    flex-direction: column;
    height: 100vh;
  }

  .menu-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    will-change: transform;
  }

  @media screen and (max-width: 1366px) {
    .menu {
      zoom: 0.8;
      height: 125vh;
    }
  }

  @media screen and (max-width: 1200px) {
    .menu {
      zoom: 0.5;
      height: 200vh;
    }
  }

  @media screen and (max-height: 1100px) {
    .menu {
      zoom: 0.8;
      height: 125vh;
    }
  }

  @media screen and (max-height: 700px) {
    .menu {
      zoom: 0.5;
      height: 200vh;
    }
  }

  @media screen and (max-height: 540px) {
    .menu {
      zoom: 0.4;
      height: 250vh;
    }
  }
</style>
