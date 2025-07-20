<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import SingleSelect from "../common/setting/select/SingleSelect.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import {addServer as restAddServer} from "../../../integration/rest";
    import {createEventDispatcher} from "svelte";

    export let visible: boolean;

    const dispatch = createEventDispatcher();

    let name = "Minecraft Server";
    let address = "";
    let resourcePackPolicy = "Prompt";

    $: disabled = validateInput(address, name);

    function validateInput(address: string, name: string): boolean {
        return address.length === 0 || name.length === 0;
    }

    async function addServer() {
        if (disabled) {
            return;
        }
        await restAddServer(name, address, resourcePackPolicy);
        dispatch("serverAdd");
        cleanUp();
        visible = false;
    }

    function cleanUp() {
        name = "Minecraft Server";
        address = "";
        resourcePackPolicy = "";
    }
</script>

<Modal bind:visible={visible} on:close={cleanUp} title="Add Server">
    <IconTextInput bind:value={name} icon="info" title="Name"/>
    <IconTextInput bind:value={address} icon="server" title="Address"/>
    <SingleSelect bind:value={resourcePackPolicy} options={["Prompt", "Enabled", "Disabled"]}
                  title="Server Resource Packs"/>
    <ButtonSetting {disabled} inset={true} listenForEnter={true} on:click={addServer} title="Add Server"/>
</Modal>
