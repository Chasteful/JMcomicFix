<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let value: number;
    export let valueType: "int" | "float";

    let inputElement: HTMLElement;
    let inputValue = "";

    $: {
        if (document.activeElement !== inputElement) {
            inputValue = value.toString();
        }
    }

    const dispatch = createEventDispatcher<{
        change: { value: number }
    }>();

    function handleInput() {
        let parsed: number;
        if (valueType === "float") {
            parsed = parseFloat(inputValue);
        } else {
            parsed = parseInt(inputValue);
        }

        if (!isNaN(parsed)) {
            dispatch("change", {value: parsed});
        }
    }

    function handleKeyDown(e: KeyboardEvent) {
        if (e.key === "Enter") {
            e.preventDefault();
        }
    }
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<span bind:innerText={inputValue} bind:this={inputElement} class="value" contenteditable="true" on:input={handleInput}
      on:keydown={handleKeyDown}></span>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .value {
    color: $text;
    font-weight: 500;
    font-size: var(--font-size);
    background-color: transparent;
    border: none;
    min-width: 5px;
    display: inline-block;
  }
</style>
