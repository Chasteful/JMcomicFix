<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {BlockHitResult, ModuleSetting, VectorSetting} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {getCrosshairData, getPlayerData} from "../../../integration/rest";

    export let setting: ModuleSetting;
    const cSetting = setting as VectorSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    async function locate() {
        const hitResult = await getCrosshairData();

        if (hitResult.type === "block") {
            const blockHitResult = hitResult as BlockHitResult;

            cSetting.value = blockHitResult.blockPos;
        } else {
            const playerData = await getPlayerData();
            cSetting.value = playerData.blockPosition;
        }
        handleChange();
    }
</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <div class="input-group">
        <input bind:value={cSetting.value.x} class="value" on:input={handleChange} placeholder="X" spellcheck="false"
               type="number"/>
        <input bind:value={cSetting.value.y} class="value" on:input={handleChange} placeholder="Y" spellcheck="false"
               type="number"/>
        <input bind:value={cSetting.value.z} class="value" on:input={handleChange} placeholder="Z" spellcheck="false"
               type="number"/>
        <button class="locate-btn" on:click={locate} title="Locate">&#x2299;</button>
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
  }

  .name {
    font-weight: 500;
    color: $text;
    font-size: var(--font-size);
    margin-bottom: 5px;
  }

  .input-group {
    display: grid;
    grid-template-columns: repeat(3, 1fr) 20px;
    column-gap: 5px;

    input.value {
      width: 100%;
      background-color: rgba($base, .4);

      font-size: var(--font-size);
      color: $text;
      border: none;
      border-bottom: solid 2px $text;
      padding: 5px;
      border-radius: 3px;
      transition: ease border-color .2s;
      appearance: textfield;

      &::-webkit-scrollbar {
        background-color: transparent;
      }


      &::-webkit-outer-spin-button,
      &::-webkit-inner-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }
    }

    .locate-btn {
      display: block;
      background-color: transparent;
      border: none;
      cursor: pointer;
      color: $text;
      font-size: var(--font-size);
      text-align: right;
    }
  }
</style>
