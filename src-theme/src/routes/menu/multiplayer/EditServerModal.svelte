<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import SingleSelect from "../common/setting/select/SingleSelect.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import {editServer as editServerRest} from "../../../integration/rest";
    import {createEventDispatcher} from "svelte";

    export let visible: boolean;
    export let name: string;
    export let address: string;
    export let resourcePackPolicy: string;
    export let id: number;

    const dispatch = createEventDispatcher();

    $: disabled = validateInput(address, name);

    function validateInput(address: string, name: string): boolean {
        return name.length === 0 || address.length === 0;
    }

    async function editServer() {
        if (disabled) {
            return;
        }
        await editServerRest(id, name, address, resourcePackPolicy);
        dispatch("serverEdit");
        visible = false;
    }
</script>

<Modal bind:visible={visible} title="Edit Server">
    <IconTextInput bind:value={name} icon="info" title="Name"/>
    <IconTextInput bind:value={address} icon="server" title="Address"/>
    <SingleSelect bind:value={resourcePackPolicy} options={["Prompt", "Enabled", "Disabled"]}
                  title="Server Resource Packs"/>
    <ButtonSetting {disabled} inset={true} listenForEnter={true} on:click={editServer} title="Edit Server"/>
</Modal>
