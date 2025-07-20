<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let value: boolean;
    export let name: string;

    const dispatch = createEventDispatcher();
</script>

<div class="switch-container">
    <label class="switch">
        <input bind:checked={value} on:change={() => dispatch("change")} type="checkbox"/>
        <span class="checkbox"></span>
    </label>
    <div class="title">{name}</div>
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../colors.scss" as *;

  .switch-container {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    transition: all 0.3s ease;
  }

  .title {
    color: $text;
    font-size: var(--font-size);
    margin-left: 6px;
    font-weight: 500;
    transition: color 0.2s ease-in-out;
  }

  .switch {
    position: relative;
    display: flex;
    align-items: center;
    cursor: pointer;
  }

  .checkbox {
    display: block;
    position: relative;
    width: 20px;
    height: 20px;
    background-color: rgba($mantle, 0.6);
    border-radius: 6px;
    border: 1px solid rgba(white, 0.15);
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3),
    inset 0 1px 2px rgba(255, 255, 255, 0.05);
    transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
    will-change: transform, background-color, box-shadow;
    cursor: pointer;


    &:hover {
      background-color: rgba($mantle, 0.8);
      border-color: rgba(white, 0.25);
      box-shadow: 0 3px 8px rgba(0, 0, 0, 0.4),
      inset 0 1px 2px rgba(255, 255, 255, 0.1);
    }

    &:active {
      transform: translateY(1px);
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3),
      inset 0 1px 3px rgba(0, 0, 0, 0.2);
    }
  }

  input {
    display: none;
  }

  input:checked + .checkbox {
    background-color: color-mix(in srgb, var(--primary-color) 50%, transparent);
    border-color: color-mix(in srgb, var(--primary-color) 50%, transparent);
    box-shadow: 0 0 12px 3px color-mix(in srgb, var(--primary-color) 40%, transparent),
    inset 0 1px 2px rgba(255, 255, 255, 0.1);

    &:hover {
      background-color: color-mix(in srgb, var(--primary-color) 95%, white 5%);
      box-shadow: 0 0 16px 4px color-mix(in srgb, var(--primary-color) 50%, transparent),
      inset 0 1px 2px rgba(255, 255, 255, 0.15);
    }
  }

  .switch:focus-within .checkbox {
    outline: 2px solid var(--primary-color);
    outline-offset: 2px;
  }


</style>
