<script lang="ts">
    import {fade, fly} from "svelte/transition";
    import {createEventDispatcher} from "svelte";
    import {portal} from "../../../../integration/util";

    export let title: string;
    export let visible: boolean;

    const dispatch = createEventDispatcher();

    function handleClick() {
        dispatch("close");
        visible = false;
    }
</script>

{#if visible}
    <div class="modal-wrapper" transition:fade|global={{duration: 200}} use:portal>
        <div class="modal" in:fly|global={{duration: 300, y: -100}} out:fly|global={{duration: 300, y: -100}}>
            <div class="titlebar">
            <button class="button-modal-close" on:click={handleClick}>
                <img src="img/menu/icon-close.svg" alt="close">
            </button>
            </div>
            <div class="title">{title}</div>

            <div class="content">
                <slot/>
            </div>
        </div>
    </div>
{/if}

<style lang="scss">

  .modal-wrapper {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(255, 255, 255, 0.02);
    z-index: 999;
  }

  .modal {
    background: rgba(255, 255, 255, 0.07);
    border-radius: 16px;
    backdrop-filter: blur(8px);
    border: 1.5px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 10px 16px rgba(0, 0, 0, 0.15);
    min-width: 500px;
    max-width: 90vw;
    max-height: 90vh;
    position: fixed;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%) scale(1);
    padding: 20px;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    overflow: hidden;
  }

  .titlebar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 14px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    background: transparent;
    backdrop-filter: blur(6px);
    color: #d0d0e0;
    font-size: 16px;
    font-weight: 500;
    border-radius: 12px 12px 0 0;
    cursor: move;
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    box-sizing: border-box;
  }

  .title {
    color: #e0e0f0;
    font-size: 18px;
    font-weight: 600;
  }

  .button-modal-close {
    font-size: 18px;
    color: #bbb;
    background: none;
    border: none;
    cursor: pointer;
    transition: color 0.2s ease-in-out;
    display: flex;
    height: 40px;
    width: 40px;
    align-items: center;
    justify-content: center;

    &:hover {
      color: #8ab4f8;
    }
  }


  .content {
    margin-top: 52px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    color: #ddddf0;
    font-size: 15px;
    padding-right: 8px;
    row-gap: 40px;
  }

  @media screen and (max-width: 1366px) {
    .modal {
      zoom: 0.8;
    }
  }

  @media screen and (max-width: 1200px) {
    .modal {
      zoom: 0.5;
    }
  }

  @media screen and (max-height: 1100px) {
    .modal {
      zoom: 0.8;
    }
  }

  @media screen and (max-height: 700px) {
    .modal {
      zoom: 0.5;
    }
  }

  @media screen and (max-height: 540px) {
    .modal {
      zoom: 0.4;
    }
  }
</style>
