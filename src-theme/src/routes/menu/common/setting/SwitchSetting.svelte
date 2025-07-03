<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let value: boolean;
    export let title: string;

    const dispatch = createEventDispatcher();
</script>

<div class="switch-setting">
    <label class="switch">
        <input bind:checked={value} on:change={() => dispatch("change")} type="checkbox"/>
        <span class="checkbox"></span>
    </label>
    <div class="title">{title}</div>
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../colors.scss" as *;

  .switch-setting {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    margin: 10px 0;
    transition: all 0.3s ease;
  }

  .title {
    color: $text;
    font-size: 22px;
    margin-left: 12px;
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
    width: 32px;
    height: 32px;
    background-color: rgba($base, 0.5);
    border-radius: 10px;
    border: 2px solid rgba(white, 0.1);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    transition: background-color 0.3s ease, box-shadow 0.3s ease, border 0.3s ease;

  }

  input {
    display: none;
  }

  input:checked + .checkbox {
    background-color: $text;
    box-shadow: 0 0 12px 3px rgba($text, 0.4),
    inset 0 1px 2px rgba(255, 255, 255, 0.1);
    animation: pulse 0.5s ease-out;
  }

  .switch:hover .checkbox {
    box-shadow: 0 0 12px 3px rgba($text, 0.4),
    inset 0 1px 2px rgba(255, 255, 255, 0.1);
  }

  .switch:focus-within .checkbox {
    outline: 2px solid $text;
  }

  @keyframes pulse {
    0% {
      box-shadow: 0 0 12px 3px rgba($text, 0.4),
      inset 0 1px 2px rgba(255, 255, 255, 0.1),
      0 0 0 0 rgba($text, 0.7);
    }
    70% {
      box-shadow: 0 0 12px 3px rgba($text, 0.4),
      inset 0 1px 2px rgba(255, 255, 255, 0.1),
      0 0 0 8px rgba($text, 0);
    }
    100% {
      box-shadow: 0 0 12px 3px rgba($text, 0.4),
      inset 0 1px 2px rgba(255, 255, 255, 0.1);
    }
  }
</style>
