<script lang="ts">
    import Router, {push} from "svelte-spa-router";
    import ClickGui from "./routes/clickgui/ClickGui.svelte";
    import Hud from "./routes/hud/Hud.svelte";
    import {getVirtualScreen, openScreen} from "./integration/rest";
    import {cleanupListeners, listenAlways} from "./integration/ws";
    import {onMount} from "svelte";
    import {locked} from "./routes/menu/LoginMenu/locked_store";
    import {insertPersistentData} from "./integration/persistent_storage";
    import Rat from "./routes/none/rat.svelte";
    import Title from "./routes/menu/title/Title.svelte";
    import Multiplayer from "./routes/menu/multiplayer/Multiplayer.svelte";
    import AltManager from "./routes/menu/altmanager/AltManager.svelte";
    import Singleplayer from "./routes/menu/singleplayer/Singleplayer.svelte";
    import ProxyManager from "./routes/menu/proxymanager/ProxyManager.svelte";
    import Disconnected from "./routes/menu/disconnected/Disconnected.svelte";
    import Browser from "./routes/browser/Browser.svelte";
    import LoginMenu from "./routes/menu/LoginMenu/LoginMenu.svelte";
    import LockScreen from "./routes/menu/LoginMenu/LockScreen.svelte";
    import ConnectionScreen from "./routes/menu/connected/ConnectionScreen.svelte";
    import ChatHud from "./routes/chat/ChatHud.svelte"
    import HudLayoutEditor from "./routes/layouteditor/HudLayoutEditor.svelte";

    const routes = {
        "/loginmenu":LoginMenu,
        "/lockscreen":LockScreen,
        "/clickgui": ClickGui,
        "/hud": Hud,
        "/inventory": Rat,
        "/title": Title,
        "/multiplayer": Multiplayer,
        "/altmanager": AltManager,
        "/singleplayer": Singleplayer,
        "/proxymanager": ProxyManager,
        "/disconnected": Disconnected,
        "/browser": Browser,
        "/connecting":ConnectionScreen,
        "/chat":ChatHud,
        "/layouteditor":Rat,
    };

    const url = window.location.href;
    const staticTag = url.split("?")[1];
    const isStatic = staticTag === "static";

    async function changeRoute(name: string) {
        cleanupListeners();
        console.log(`[Router] Redirecting to ${name}`);
        await push(`/${name}`);
    }
    $: if ($locked) {
        openScreen("lockscreen");
    }

    onMount(async () => {
        await insertPersistentData();

        if (isStatic) {
            return;
        }

        listenAlways("socketReady", async () => {
            const virtualScreen = await getVirtualScreen();
            await changeRoute(virtualScreen.name || "none");
        });

        listenAlways("virtualScreen", async (event: any) => {
            console.log(`[Router] Virtual screen change to ${event.screenName}`);
            const action = event.action;

            switch (action) {
                case "close":
                    await changeRoute("none");
                    break;
                case "open":
                    await changeRoute(event.screenName || "none");
                    break;
            }
        });

        const virtualScreen = await getVirtualScreen();
        await changeRoute(virtualScreen.name || "none");
    });
</script>

<main>
    <Router {routes}/>
</main>
