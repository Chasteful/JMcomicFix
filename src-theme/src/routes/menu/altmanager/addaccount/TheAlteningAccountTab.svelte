<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {addAlteningAccount, browse} from "../../../../integration/rest";

    let token = "";
    let loading = false;
    $: disabled = validateToken(token);

    function validateToken(token: string) {
        return token.length === 0;
    }

    async function addAccount() {
        if (disabled) {
            return;
        }
        loading = true;
        await addAlteningAccount(token);
    }
</script>

<Tab>
    <IconTextInput bind:value={token} icon="user" title="Token"/>
    <ButtonSetting {disabled} inset={true} listenForEnter={true} {loading} on:click={addAccount} title="Add Account"/>
    <ButtonSetting on:click={() => browse("ALTENING_FREE")} secondary={true} title="Get Account Token"/>
</Tab>
