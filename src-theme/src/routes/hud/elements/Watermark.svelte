<script lang="ts">
    import {getClientInfo, getModuleSettings, getSession} from "../../../integration/rest";
    import type {
        ClientInfo,
        Session,
        PlayerData,
        ConfigurableSetting, TextSetting,
    } from "../../../integration/types";
    import { onMount } from "svelte";
    import { listen } from "../../../integration/ws";
    import {getModules} from "../../../integration/rest";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import { fade, fly } from 'svelte/transition';
    import {clientName} from "../../../components/ThemeManager";
    import {expoInOut} from "svelte/easing";

    let clientInfo: ClientInfo | null = null;
    let session: Session | null = null;
    let playerData: PlayerData | null = null;
    let showUsername = false;
    let nameProtect : string = ""


    let dataPromise = Promise.all([
        getClientInfo().then(info => clientInfo = info),
        getSession().then(s => session = s),
        CheckShowUsername().then(show => showUsername = show)
    ]);
    function NameProtectSetting(configurable: ConfigurableSetting) {
        const Replacement= configurable.value.find(v => v.name === "Replacement") as TextSetting;
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
<svg width="0" height="0" aria-hidden="true">
    <filter id="glow" x="-50%" y="-200%" width="200%" height="500%" primitiveUnits="objectBoundingBox">
        <feGaussianBlur in="SourceGraphic" stdDeviation=".025 .2" result="blurred" />
        <feColorMatrix in="blurred" type="saturate" values="1.3" result="saturated" />
        <feBlend in="SourceGraphic" in2="saturated" mode="normal" />
    </filter>
</svg>

<div class="watermark"  transition:fly|global={{duration: 500, y: -50, easing: expoInOut}}>
    <div class="watermark-content">
        {#if clientInfo}
            <div  class="client client-glow" in:fade>
                { $clientName || "禁漫修复" }&nbsp;{clientInfo.clientVersion}
            </div>
            {#if session }
                <div class="separator"></div>
            {#if showUsername }
                <div class="info">{session.username}</div>
                {:else }
                <div class="info">{nameProtect}</div>
            {/if}
            {/if}
            {#if playerData}
                <div class="separator"></div>
                <div class="info">{playerData.ping}&nbsp;Ping</div>
                <div class="separator"></div>
                <div class="info">{playerData.serverAddress}</div>
            {/if}
        {/if}
    </div>
</div>


<style lang="scss">
  @import "../../../colors.scss";
  @property --k {
    syntax: '<number>';
    initial-value: 0;
    inherits: false;
  }

  @keyframes k {
    to {
      --k: 1;
    }
  }

  .client-glow {
    animation: k 4s linear infinite;
    filter: url(#glow);
    background-image: linear-gradient(
                    90deg,
                    hsl(calc(var(--k) * 1turn), 95%, 65%),
                    hsl(calc(var(--k) * 1turn + 90deg), 95%, 65%)
    );
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    color: transparent;
    font-weight: 900;
  }



  .watermark {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    padding: 10px;
    color: hsl(0, 0%, 90%);
    text-shadow: 0 0 3px rgba(255, 255, 255, 0.9);
    background: rgba($base, 0.5);
    border-radius:  8px;
    font-size: 20px;
    min-width: 150px;
    box-shadow:
            0 4px 16px rgba($base, 0.6),
            inset 0 0 10px rgba(255, 255, 255, 0.05);
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
  .client {
    font-size: 22px;
    font-family: 'Alibaba', sans-serif;
    font-feature-settings: "tnum";
    font-variant-numeric: tabular-nums;
    font-weight: 700;
    text-transform: uppercase;
  }

</style>
