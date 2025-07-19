<script lang="ts">
    import {getClientInfo, getModuleSettings, getSession} from "../../../../integration/rest";
    import type {
        ClientInfo,
        Session,
        PlayerData,
        ConfigurableSetting, TextSetting,
    } from "../../../../integration/types";
    import {onMount} from "svelte";
    import {listen} from "../../../../integration/ws";
    import {getModules} from "../../../../integration/rest";
    import type {ClientPlayerDataEvent} from "../../../../integration/events";
    import {fly} from 'svelte/transition';
    import {expoInOut} from "svelte/easing";
    import ClientName from "./ClientName.svelte";

    let clientInfo: ClientInfo | null = null;
    let session: Session | null = null;
    let playerData: PlayerData | null = null;
    let showUsername = false;
    let nameProtect: string = ""


    let dataPromise = Promise.all([
        getClientInfo().then(info => clientInfo = info),
        getSession().then(s => session = s),
        CheckShowUsername().then(show => showUsername = show)
    ]);

    function NameProtectSetting(configurable: ConfigurableSetting) {
        const Replacement = configurable.value.find(v => v.name === "Replacement") as TextSetting;
        nameProtect = Replacement?.value ?? "";

    }

    async function CheckShowUsername() {
        const modules = await getModules();
        return modules.some(module => module.name === "NameProtect" && !module.enabled)
    }

    async function updateClientInfo() {
        clientInfo = await getClientInfo();
    }

    async function updateSession() {
        session = await getSession();
    }

    onMount(async () => {
        await dataPromise;
        const settings = await getModuleSettings("NameProtect");
        NameProtectSetting(settings);

        setInterval(async () => {
            await updateClientInfo();
            await updateSession();
            showUsername = await CheckShowUsername();
            const settings = await getModuleSettings("NameProtect");
            NameProtectSetting(settings);
        }, 1000);
    });
    listen("clientPlayerData", ((event: ClientPlayerDataEvent) => {
        playerData = event.playerData;
    }));


    listen("session", async () => {
        await updateSession();
    });
</script>


<div class="watermark hud-container" transition:fly|global={{ duration: 500, y: -50, easing: expoInOut }}>
    <div class="watermark-content">
        {#if clientInfo}
            <ClientName {clientInfo}
            />

            {#if session}
                <div class="separator"></div>
                <div class="info">
                    {#if showUsername}
                        {session.username}
                    {:else}
                        {nameProtect}
                    {/if}
                </div>
            {/if}

            {#if playerData}
                <div class="separator"></div>
                <div class="info">{playerData.ping} Ping</div>
                <div class="separator"></div>
                <div class="info">{playerData.serverAddress}</div>
            {/if}
        {/if}
    </div>
</div>

<style lang="scss">
  @import "../../../../colors";

  .watermark {
    display: flex;
    white-space: nowrap;
    flex-direction: row;
    align-items: stretch;
    padding: 10px;
    color: hsl(0, 0%, 90%);
    text-shadow: 0 0 3px rgba(255, 255, 255, 0.9);
    font-size: 20px;
    min-width: 150px;
  }

  .watermark-content {
    display: flex;
    align-items: center;
    padding: 0 8px;
  }

  .separator {
    width: 2px;
    height: 16px;
    background: rgba($text, 0.25);
    margin: 0 5px;
  }

  .info {
    margin: 0 6px;
  }
</style>
