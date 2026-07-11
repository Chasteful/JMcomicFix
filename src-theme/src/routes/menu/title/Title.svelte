<script lang="ts">
    import MainButton from "./buttons/MainButton.svelte";
    import ChildButton from "./buttons/ChildButton.svelte";
    import ConfettiBackground from "./ConfettiBackground.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import IconButton from "../common/buttons/IconButton.svelte";
    import {
        browse,
        exitClient,
        getClientUpdate,
        openScreen,
        toggleBackgroundShaderEnabled
    } from "../../../integration/rest";
    import {fly} from "svelte/transition";
    import {onMount} from "svelte";
    import {notification} from "../common/header/notification_store";
    import {isAnniversary} from "../../../util/utils";

    let regularButtonsShown = true;
    let clientButtonsShown = false;

    onMount(() => {
        setTimeout(async () => {
            const release = await getClientRelease();

            if (release && !release.error) {
                notification.set({
                    title: `JMcomicFix ${release.tagName} version has been released!`,
                    message: `Click to copy the download link.`,
                    url: release.downloadUrl,
                    error: false,
                    delay: 50
                });
            }
        }, 2000);
    });

    function toggleButtons() {
        if (clientButtonsShown) {
            clientButtonsShown = false;
            setTimeout(() => {
                regularButtonsShown = true;
            }, 750);
        } else {
            regularButtonsShown = false;
            setTimeout(() => {
                clientButtonsShown = true;
            }, 750);
        }
    }
</script>

<div class="title-screen">
    {#if isAnniversary()}
        <ConfettiBackground/>
    {/if}

    <div class="content">
        <div class="main-buttons">
            {#if regularButtonsShown}
                <MainButton title="Singleplayer" icon="singleplayer" index={0}
                            on:click={() => openScreen("singleplayer")}/>

                    <MainButton title="Multiplayer" icon="multiplayer" let:parentHovered
                                on:click={() => openScreen("multiplayer")} index={1}>
                        <ChildButton title="Realms" icon="realms" {parentHovered}
                                     on:click={() => openScreen("multiplayer_realms")}/>
                    </MainButton>
                    <MainButton title="LiquidBounce" icon="liquidbounce" on:click={toggleButtons} index={2}/>
                    <MainButton title="Options" icon="options" on:click={() => openScreen("options")} index={3}/>
                {:else if clientButtonsShown}
                    <MainButton title="Proxy Manager" icon="proxymanager" on:click={() => openScreen("proxymanager")}
                                index={0}/>
                    <MainButton title="Click GUI" icon="clickgui" on:click={() => openScreen("clickgui")} index={1}/>
                    <MainButton title="HUD" icon="layout" on:click={() => openScreen("hud")} index={2}/>
                    <!-- <MainButton title="Scripts" icon="scripts" index={2}/> -->
                    <MainButton title="Back" icon="back-large" on:click={toggleButtons} index={3}/>
                {/if}
            </div>

            <div class="social-buttons" transition:fly|global={{duration:300, y:100}}>
                <ButtonContainer>
                    <IconButton icon="nodebb" on:click={() => browse("MAINTAINER_FORUM")} title="Forum"/>
                    <IconButton icon="github" on:click={() => browse("MAINTAINER_GITHUB")} title="GitHub"/>
                    <IconButton icon="discord" on:click={() => browse("MAINTAINER_DISCORD")} title="Discord"/>
                    <IconButton icon="twitter" on:click={() => browse("MAINTAINER_TWITTER")} title="Twitter"/>
                    <IconButton icon="youtube" on:click={() => browse("MAINTAINER_YOUTUBE")} title="YouTube"/>
                    <IconButton icon="bili" on:click={() => browse("MAINTAINER_BILIBILI")} title="BiliBili"/>
                    <IconTextButton icon="icon-liquidbounce.net.svg" on:click={() => browse("CLIENT_WEBSITE")}
                                    title="Releases"/>
                </ButtonContainer>
            </div>
        </div>
</div>

<style>
    .title-screen {
        position: relative;
        isolation: isolate;
        display: flex;
        flex: 1;
        flex-direction: column;
    }

    .content {
        flex: 1;
        display: grid;
        grid-template-areas:
            "a ."
            "b c";
        grid-template-rows: 1fr max-content;
        grid-template-columns: 1fr max-content;
    }

    .main-buttons {
        display: flex;
        flex-direction: column;
        row-gap: 25px;
        grid-area: a;
    }


    .social-buttons {
        grid-area: c;
    }
</style>
