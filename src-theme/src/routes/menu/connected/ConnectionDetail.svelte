<script lang="ts">
    import type {ConnectionDetailsEvent} from "../../../integration/events"
    import {listen} from "../../../integration/ws"
    import {fade} from "svelte/transition"
    import TextComponent from "../common/TextComponent.svelte"

    let connectionDetails: ConnectionDetailsEvent;


    listen("connectionDetails", (event: ConnectionDetailsEvent) => {
        connectionDetails = event

    })


</script>


<div class="overlay-message" transition:fade={{duration:600}}>
    {#if connectionDetails}
        <TextComponent fontSize={28} textComponent={connectionDetails.result}/>
    {/if}

</div>


<style lang="scss">
  .overlay-message {
    position: fixed;
    left: 50%;
    top: 50%;
    -webkit-font-smoothing: none;
    -moz-osx-font-smoothing: grayscale;
    font-smooth: never;
    transform: translate(-50%, -50%);
    display: flex;
    font-family: 'Minecraftia', serif;
    flex-direction: column;
    align-items: center;
    text-align: center;
    white-space: nowrap;
    width: 100%;
    gap: 6px;
    z-index: 1000;
    image-rendering: pixelated;
    text-shadow: 2px 2px #000A;
  }

  @media screen and (max-width: 1600px) {
    .overlay-message {
      transform: translate(-50%, -50%) scale(0.9);
    }
  }

  @media screen and (max-width: 1366px) {
    .overlay-message {
      transform: translate(-50%, -50%) scale(0.8);
    }
  }

  @media screen and (max-width: 1200px) {
    .overlay-message {
      transform: translate(-50%, -50%) scale(0.7);
    }
  }

  @media screen and (max-height: 1100px) {
    .overlay-message {
      transform: translate(-50%, -50%) scale(0.6);
    }
  }

  @media screen and (max-height: 700px) {
    .overlay-message {
      transform: translate(-50%, -50%) scale(0.5);
    }
  }
</style>
