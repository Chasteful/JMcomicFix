<script lang="ts">
    import SimpleButton from "../common/setting/SimpleButton.svelte";
    import {
        directLoginToCrackedAccount,
        getAccounts,
        loginToAccount, openScreen,
        randomUsername,
        reconnectToServer
    } from "../../../integration/rest";
    import type {
        AccountManagerLoginEvent,
    } from "../../../integration/events";
    import {listen} from "../../../integration/ws";
    import {onMount} from "svelte";
    import type {Account} from "../../../integration/types";
    import {restoreSession,} from "../../../integration/rest.js";
    import {fly} from "svelte/transition";
    import Menu from "../common/Menu.svelte";

    import ReasonInfo from "./ReasonInfo.svelte";

    let premiumAccounts: Account[] = [];

    function handleBackClick(screen: string) {

        setTimeout(() => openScreen(screen), 10);
    }

    async function reconnectWithRandomUsername() {
        const username = await randomUsername();
        await directLoginToCrackedAccount(username, false);
    }

    async function reconnectWithRandomAccount() {
        const account = premiumAccounts[Math.floor(Math.random() * premiumAccounts.length)];
        await loginToAccount(account.id);
    }

    onMount(async () => {
        premiumAccounts = (await getAccounts()).filter(a => a.type !== "Cracked" && !a.favorite);
        setTimeout(() => {
            listen("accountManagerLogin", async (e: AccountManagerLoginEvent) => {
                await reconnectToServer();
            });
        }, 1000);
    });

</script>


<Menu>
    <div class="reconnect" transition:fly|global={{duration:300, y:100}}>
        <SimpleButton on:click={() => reconnectToServer()} title="Reconnect"/>
        <SimpleButton on:click={restoreSession} title="Restore initial session"/>
        <SimpleButton disabled={premiumAccounts.length === 0} on:click={reconnectWithRandomAccount}
                      title="Reconnect with random account"/>
        <SimpleButton on:click={reconnectWithRandomUsername} title="Reconnect with random username"/>
    </div>
    <div class="back" transition:fly|global={{duration:300, y:100}}>
        <SimpleButton on:click={() => handleBackClick("altmanager")} title="Back to AltManager"/>
        <SimpleButton on:click={() => handleBackClick("multiplayer")} title="Back to Server List"/>
        <SimpleButton on:click={() => handleBackClick("title")} title="Back to Title Screen"/>
    </div>
</Menu>
<ReasonInfo/>
<div class="background-image"
     style="background-image: url('img/menu/disconnected/xibao.png')"
></div>
<style lang="scss">
  .background-image {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-image: url('/img/menu/disconnected/xibao.png');
    background-size: 100% 100%;
    background-position: center;
    z-index: -1;
    transform: scale(1);

  }

  .back {
    position: fixed;
    bottom: 20px;
    right: 5px;
    display: flex;
    flex-direction: column;
    row-gap: 20px;
    align-items: flex-end;
  }

  .reconnect {
    position: fixed;
    bottom: 20px;
    left: 5px;
    display: flex;
    flex-direction: column;
    row-gap: 20px;
    align-items: flex-start;
  }
</style>
