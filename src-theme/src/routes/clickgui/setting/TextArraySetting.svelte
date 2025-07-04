<script lang="ts">
    import type {ModuleSetting, TextArraySetting} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {createEventDispatcher} from "svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as TextArraySetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    function removeValueIndex(index: number) {
        cSetting.value.splice(index, 1);
        cSetting.value = cSetting.value;
        handleChange();
    }

    function addValueIndex() {
        cSetting.value = ["", ...cSetting.value];
        handleChange();
    }

</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <button class="button-add" on:click={addValueIndex}>Add value</button>
    {#if cSetting.value.length > 0}
        <div class="inputs">
            {#each cSetting.value as _, index}
                <div class="input-wrapper">
                    <input type="text" class="value" spellcheck="false" placeholder={setting.name}
                           bind:value={cSetting.value[index]}
                           on:input={handleChange}>
                    <button class="button-remove" title="Remove" on:click={() => removeValueIndex(index)}>
                        <img src="img/clickgui/icon-cross.svg" alt="remove">
                    </button>
                </div>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../colors.scss" as *;

  .input-wrapper {
    display: grid;
    grid-template-columns: 1fr max-content;
    column-gap: 5px;
    align-items: center;
  }

  .button-remove {
    background-color: transparent;
    border: none;
    cursor: pointer;
  }

  .setting {
    padding: 7px 0;
  }

  .inputs {
    display: flex;
    flex-direction: column;
    row-gap: 10px;
    margin-top: 5px;
  }

  .name {
    font-weight: 500;
    color: $text;
    font-size: var(--font-size);
    margin-bottom: 5px;
  }

  .button-add {

    font-size: var(--font-size);
    color: $text;
    background: linear-gradient(to right,
            color-mix(in srgb, var(--primary-color) 30%, transparent) 0%,
            color-mix(in srgb, var(--secondary-color) 30%, transparent) 51%,
            color-mix(in srgb, var(--primary-color) 30%, transparent) 100%
    );
    border: none;
    padding: 6px 10px;
    transition: 0.5s;
    text-align: center;
    background-size: 200% auto;
    width: 100%;
    cursor: pointer;
    border-radius: 3px;

    &:hover {
      background-position: right center;
    }
  }

  .value {
    width: 100%;
    background: rgba($base, 0.3);
    backdrop-filter: blur(6px);
    -webkit-backdrop-filter: blur(6px);
    font-size: var(--font-size);
    color: $text;
    padding: 6px;
    border-radius: 3px;

    &::-webkit-scrollbar {
      background-color: transparent;
    }
  }
</style>
