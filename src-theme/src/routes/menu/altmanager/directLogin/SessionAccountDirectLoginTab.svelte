<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {directLoginToSessionAccount} from "../../../../integration/rest";

    let token = "";
    $: disabled = validateSessionId(token);

    function validateSessionId(token: string): boolean {
        return token.length === 0;
    }

    async function login() {
        if (disabled) {
            return;
        }
        await directLoginToSessionAccount(token);
    }
</script>

<Tab>
    <IconTextInput bind:value={token} icon="user" title="Session ID"/>
    <ButtonSetting {disabled} inset={true} listenForEnter={true} on:click={login} title="Login"/>
</Tab>
