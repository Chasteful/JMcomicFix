<script lang="ts">
    import type {
        OverlayDisconnectionEvent,
    } from "../../../integration/events";
    import {listen} from "../../../integration/ws";
    import {fade} from "svelte/transition";
    import type {TextComponent as TTextComponent} from "../../../integration/types";
    import TextComponent from "../common/TextComponent.svelte";
    import {writable} from "svelte/store";

    const overlayDisconnection = writable<OverlayDisconnectionEvent | null>(null);

    let showMessage = true;

    function getLines(input: TTextComponent | string): (TTextComponent | string)[] {
        if (typeof input === "string") {
            return input.split("\n");
        } else if (true && input.text.includes("\n")) {

            return input.text.split("\n").map(line => ({
                ...input,
                text: line
            }));
        } else {
            return [input];
        }
    }

    listen("overlayDisconnection", async (event: OverlayDisconnectionEvent) => {
        const evtClone = {...event, info: event.info};
        overlayDisconnection.set(evtClone);
        showMessage = true;
    });
</script>

{#if $overlayDisconnection && showMessage}
    <div class="overlay-message" transition:fade={{duration:200}}>
        {#if $overlayDisconnection?.info}
            {#each getLines($overlayDisconnection.info) as line}
                <TextComponent fontSize={24} textComponent={line}/>
            {/each}
        {/if}
    </div>
{/if}

<style lang="scss">
  .overlay-message {
    position: fixed;
    left: 50vw;
    top: 50vh;
    -webkit-font-smoothing: none;
    -moz-osx-font-smoothing: grayscale;
    font-smooth: never;
    transform: translate(-50%, -50%);
    display: flex;
    font-family: 'Minecraftia', serif;
    flex-direction: column;
    align-items: center;
    text-align: center;
    white-space: pre-wrap;
    width: 100%;
    gap: 6px;
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
