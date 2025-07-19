<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {addCrackedAccount, randomUsername} from "../../../../integration/rest";
    import IconButton from "../../common/buttons/IconButton.svelte";
    import SwitchSetting from "../../common/setting/SwitchSetting.svelte";

    let username = "";
    let online = false;

    async function addAccount() {
        await addCrackedAccount(username, online);
    }

    async function generateRandomUsername() {
        username = await randomUsername();
    }
</script>

<Tab>
    <IconTextInput bind:value={username} icon="user" maxLength={16} title="Username">
        <IconButton icon="random" on:click={generateRandomUsername} title="Random"/>
    </IconTextInput>
    <SwitchSetting bind:value={online} title="Use online UUID"/>
    <ButtonSetting inset={true} listenForEnter={true} on:click={addAccount} title="Add Account"/>
</Tab>
