<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {addMicrosoftAccount, addMicrosoftAccountCopyUrl} from "../../../../integration/rest.js";

    let loading = false;

    async function addAccount() {
        loading = true;
        await addMicrosoftAccount();
    }

    async function copyLoginUrl() {
        await addMicrosoftAccountCopyUrl();
    }
</script>

<Tab>
    <ButtonSetting {loading} on:click={addAccount} title="Link Account"/>
    <ButtonSetting inset={true} on:click={copyLoginUrl} secondary={true} title="Copy URL"/>
</Tab>
