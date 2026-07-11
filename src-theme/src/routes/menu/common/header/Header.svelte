<script lang="ts">
    import Account from "./account/Account.svelte";
    import Notifications from "./Notifications.svelte";
    import {listen} from "../../../../integration/ws";
    import type {
        AccountManagerAdditionEvent,
        AccountManagerLoginEvent,
        AccountManagerMessageEvent
    } from "../../../../integration/events";
    import {notification} from "./notification_store";
    import Logo from "./Logo.svelte";
    import {location} from "svelte-spa-router";
    import {isAnniversary} from "../../../../util/utils";
    import AnimatedLogo from "./AnimatedLogo.svelte";

    export let showAccount: boolean;
    export let showHeader: boolean;

    $: showAnniversaryLogo = $location === "/title" && isAnniversary();

    listen("accountManagerAddition", (e: AccountManagerAdditionEvent) => {
        if (!e.error) {
            notification.set({
                title: "AltManager",
                message: `Successfully added account ${e.username}`,
                error: false
            });
        } else {
            notification.set({
                title: "AltManager",
                message: e.error,
                error: true
            });
        }
    });

    listen("accountManagerMessage", (e: AccountManagerMessageEvent) => {
        notification.set({
            title: "AltManager",
            message: e.message,
            error: false
        });
    });

    listen("accountManagerLogin", (e: AccountManagerLoginEvent) => {
        if (!e.error) {
            notification.set({
                title: "AltManager",
                message: `Successfully logged in to account ${e.username}`,
                error: false
            });
        } else {
            notification.set({
                title: "AltManager",
                message: e.error,
                error: true
            });
        }
    });
</script>

{#if showHeader}
<div class="header">
    <div class="logo-wrapper">
        <div class="logo" class:visible={showAnniversaryLogo} aria-hidden={!showAnniversaryLogo}>
            <AnimatedLogo/>
        </div>
        <div class="logo" class:visible={!showAnniversaryLogo} aria-hidden={showAnniversaryLogo}>
            <Logo/>
        </div>
    </div>

    <div class="notifications">
        <Notifications/>
    </div>
    {#if showAccount}
        <Account/>
    {/if}
</div>
{/if}
<style lang="scss">
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 60px;
  }

  .notifications {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
  }

  .logo-wrapper {
    display: grid;
  }

  .logo {
    grid-area: 1 / 1;
    opacity: 0;
    pointer-events: none;
    transition: opacity .5s ease;

    &.visible {
      opacity: 1;
    }
  }
</style>
