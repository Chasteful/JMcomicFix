<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {
        ModuleSetting,
        ChooseSetting,
    } from "../../../integration/types";
    import Dropdown from "./common/Dropdown.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;

    const cSetting = setting as ChooseSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <Dropdown
            bind:value={cSetting.value}
            name={$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}
            on:change={handleChange}
            options={cSetting.choices}
    />
</div>

<style lang="scss">
  .setting {
    padding: 7px 0;
  }
</style>
