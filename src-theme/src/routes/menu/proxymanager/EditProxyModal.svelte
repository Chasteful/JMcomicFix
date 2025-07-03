<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import {editProxy as editProxyRest} from "../../../integration/rest";
    import {afterUpdate} from "svelte";
    import {listen} from "../../../integration/ws";

    export let visible: boolean;
    export let id: number;
    export let host: string;
    export let port: number;
    export let username: string;
    export let password: string;
    export let requiresAuthentication: boolean;
    export let forwardAuthentication: boolean;

    let hostPort = "";

    let loading = false;

    $: disabled = validateInput(requiresAuthentication, hostPort, username, password);
    $: {
        if (!requiresAuthentication) {
            username = "";
            password = "";
        }
    }

    afterUpdate(() => {
        hostPort = `${host}:${port}`;
    });

    function validateInput(requiresAuthentication: boolean, hostPort: string, username: string, password: string): boolean {
        let valid = /.+:[0-9]+/.test(hostPort);

        if (requiresAuthentication) {
            valid &&= username.length > 0 && password.length > 0;
        }

        return !valid;
    }

    async function editProxy() {
        if (disabled) {
            return;
        }

        const [host, port] = hostPort.split(":");

        loading = true;
        await editProxyRest(id, host, parseInt(port), username, password, forwardAuthentication);
    }

    listen("proxyEditResult", () => {
        visible = false;
        loading = false;
    });
</script>

<Modal bind:visible={visible} title="Edit Proxy">
    <IconTextInput bind:value={hostPort} icon="server" pattern=".+:[0-9]+" title="Host:Port"/>
    <SwitchSetting bind:value={requiresAuthentication} title="Requires Authentication"/>
    {#if requiresAuthentication}
        <IconTextInput title="Username" icon="user" bind:value={username}/>
        <IconTextInput title="Password" icon="lock" type="password" bind:value={password}/>
    {/if}
    <SwitchSetting bind:value={forwardAuthentication} title="Forward Microsoft Authentication"/>
    <ButtonSetting {disabled} listenForEnter={true} {loading} on:click={editProxy} title="Edit Proxy"/>
</Modal>
