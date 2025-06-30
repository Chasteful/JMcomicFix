<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ModuleSetting, TextSetting,} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;

    const cSetting = setting as TextSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <input bind:value={cSetting.value} class="value" on:input={handleChange}
           placeholder={$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}
           spellcheck="false" type="text">
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0px;
  }

  .name {
    font-weight: 500;
    color: $text;
    font-size: var(--font-size);
    margin-bottom: 5px;
  }

  .value {
    width: 100%;
    background-color: rgba($base, .36);

    font-size: var(--font-size);
    color: $text;
    border: none;
    border-bottom: solid 2px $text;
    padding: 5px;
    border-radius: 3px;
    transition: ease border-color .2s;

    &::-webkit-scrollbar {
      background-color: transparent;
    }
  }
</style>
