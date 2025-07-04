<script lang="ts">
    import {
        addProxyFromClipboard,
        checkProxy,
        connectToProxy as connectToProxyRest,
        disconnectFromProxy as disconnectFromProxyRest,
        getCurrentProxy,
        getProxies,
        openScreen,
        removeProxy as removeProxyRest,
        setProxyFavorite,
    } from "../../../integration/rest.js";
    import BottomButtonWrapper from "../common/buttons/BottomButtonWrapper.svelte";
    import OptionBar from "../common/optionbar/OptionBar.svelte";
    import MenuListItem from "../common/menulist/MenuListItem.svelte";
    import Menu from "../common/Menu.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import MenuListItemTag from "../common/menulist/MenuListItemTag.svelte";
    import MenuList from "../common/menulist/MenuList.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import Search from "../common/Search.svelte";
    import MenuListItemButton from "../common/menulist/MenuListItemButton.svelte";
    import type {Proxy} from "../../../integration/types";
    import {onMount} from "svelte";
    import AddProxyModal from "./AddProxyModal.svelte";
    import EditProxyModal from "./EditProxyModal.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import MultiSelect from "../common/setting/select/MultiSelect.svelte";
    import {notification} from "../common/header/notification_store";
    import lookup from "country-code-lookup";
    import {listen} from "../../../integration/ws";
    import type {
        ProxyAdditionResultEvent,
        ProxyCheckResultEvent,
        ProxyEditResultEvent
    } from "../../../integration/events.js";

    $: {
        let filteredProxies = proxies;

        filteredProxies = filteredProxies.filter(p => countries.includes(convertCountryCode(p.ipInfo?.country)));
        if (favoritesOnly) {
            filteredProxies = filteredProxies.filter(a => a.favorite);
        }
        if (searchQuery) {
            filteredProxies = filteredProxies.filter(p => p.host.toLowerCase().includes(searchQuery.toLowerCase()));
        }

        renderedProxies = filteredProxies;
    }

    let addProxyModalVisible = false;
    let editProxyModalVisible = false;
    let allCountries: string[] = [];

    let searchQuery = "";
    let favoritesOnly = false;
    let countries: string[] = [];

    let proxies: Proxy[] = [];
    let renderedProxies = proxies;
    let isConnectedToProxy = false;

    let currentEditProxy: Proxy | null = null;

    onMount(async () => {
        await refreshProxies();
        renderedProxies = proxies;
        await updateIsConnectedToProxy();
    });

    async function updateIsConnectedToProxy() {
        isConnectedToProxy = Object.keys(await getCurrentProxy()).length > 0;
    }

    function convertCountryCode(code: string | undefined): string {
        if (code === undefined) {
            return "Unknown";
        }
        return lookup.byIso(code)?.country ?? "Unknown";
    }

    async function refreshProxies() {
        proxies = await getProxies();

        let c = new Set();
        for (const p of proxies) {
            c.add(convertCountryCode(p.ipInfo?.country));
        }
        allCountries = Array.from(c) as string[];
        countries = allCountries;
    }

    function handleSearch(e: CustomEvent<{ query: string }>) {
        searchQuery = e.detail.query;
    }

    function handleProxySort() {

    }

    async function removeProxy(id: number) {
        await removeProxyRest(id);
        await refreshProxies();
    }

    async function connectToProxy(id: number) {
        await connectToProxyRest(id);
        notification.set({
            title: "ProxyManager",
            message: "Connected to proxy",
            error: false
        });
        await updateIsConnectedToProxy();
    }

    async function connectToRandomProxy() {
        const proxy = renderedProxies[Math.floor(Math.random() * renderedProxies.length)];
        if (proxy) {
            await connectToProxy(proxy.id);
        }
    }

    async function toggleFavorite(index: number, favorite: boolean) {
        await setProxyFavorite(index, favorite);
        await refreshProxies();
    }

    listen("proxyAdditionResult", async (e: ProxyAdditionResultEvent) => {
        if (e.error) {
            notification.set({
                title: "ProxyManager",
                message: "Couldn't connect to proxy",
                error: true
            });
        } else {
            notification.set({
                title: "ProxyManager",
                message: "Successfully added proxy",
                error: false
            });

            await refreshProxies();
        }
    });

    listen("proxyEditResult", async (e: ProxyEditResultEvent) => {
        if (e.error) {
            notification.set({
                title: "ProxyManager",
                message: "Couldn't connect to proxy",
                error: true
            });
        } else {
            notification.set({
                title: "ProxyManager",
                message: "Successfully edited proxy",
                error: false
            });

            await refreshProxies();
        }
    });

    listen("proxyCheckResult", async (e: ProxyCheckResultEvent) => {
        if (e.error) {
            notification.set({
                title: "ProxyManager",
                message: "Could not connect to proxy",
                error: true
            });
        } else {
            notification.set({
                title: "ProxyManager",
                message: "Proxy is working",
                error: false
            });

            await refreshProxies();
        }
    });

    async function disconnectFromProxy() {
        await disconnectFromProxyRest();
        await updateIsConnectedToProxy();
        notification.set({
            title: "ProxyManager",
            message: "Disconnected from proxy",
            error: false
        });
    }

    function editProxy(proxy: Proxy) {
        currentEditProxy = proxy;
        editProxyModalVisible = true;
    }
</script>

<AddProxyModal bind:visible={addProxyModalVisible}/>
{#if currentEditProxy}
    <EditProxyModal bind:visible={editProxyModalVisible} id={currentEditProxy.id}
                    host={currentEditProxy.host}
                    port={currentEditProxy.port}
                    forwardAuthentication={currentEditProxy.forwardAuthentication}
                    username={currentEditProxy.credentials?.username ?? ""}
                    password={currentEditProxy.credentials?.password ?? ""}
                    requiresAuthentication={currentEditProxy.credentials !== undefined}/>
{/if}
<Menu>
    <OptionBar>
        <Search on:search={handleSearch}/>
        <SwitchSetting bind:value={favoritesOnly} title="Favorites Only"/>
        <MultiSelect bind:values={countries} options={allCountries} title="Country"/>
    </OptionBar>

    <MenuList on:sort={handleProxySort} sortable={false}>
        {#each renderedProxies as proxy}
            <MenuListItem
                    image="img/flags/{(proxy.ipInfo?.country ?? 'unknown').toLowerCase()}.svg"
                    title="{proxy.host}:{proxy.port}"
                    favorite={proxy.favorite}
                    on:dblclick={() => connectToProxy(proxy.id)}>
                <svelte:fragment slot="subtitle">
                    <span class="subtitle">{proxy.ipInfo?.org ?? "Unknown"}</span>
                </svelte:fragment>

                <svelte:fragment slot="tag">
                    <MenuListItemTag text={convertCountryCode(proxy.ipInfo?.country)}/>
                </svelte:fragment>

                <svelte:fragment slot="active-visible">
                    <MenuListItemButton title="Delete" icon="trash" on:click={() => removeProxy(proxy.id)}/>
                    <MenuListItemButton title="Check" icon="check" on:click={() => checkProxy(proxy.id)}/>
                    <MenuListItemButton title="Favorite" icon={proxy.favorite ? "favorite-filled" : "favorite" }
                                        on:click={() => toggleFavorite(proxy.id, !proxy.favorite)}/>
                    <MenuListItemButton title="Edit" icon="pen-2" on:click={() => editProxy(proxy)}/>
                </svelte:fragment>

                <svelte:fragment slot="always-visible">
                    <MenuListItemButton title="Connect" icon="play" on:click={() => connectToProxy(proxy.id)}/>
                </svelte:fragment>
            </MenuListItem>
        {/each}
    </MenuList>

    <BottomButtonWrapper>
        <ButtonContainer>
            <IconTextButton icon="icon-plus-circle.svg" on:click={() => addProxyModalVisible = true} title="Add"/>
            <IconTextButton icon="icon-clipboard.svg" on:click={() => addProxyFromClipboard()} title="Add Clipboard"/>
            <IconTextButton disabled={renderedProxies.length === 0} icon="icon-random.svg" on:click={connectToRandomProxy}
                            title="Random"/>
            <IconTextButton disabled={!isConnectedToProxy} icon="icon-disconnect.svg" on:click={disconnectFromProxy}
                            title="Disconnect"/>
        </ButtonContainer>

        <ButtonContainer>
            <IconTextButton icon="icon-back.svg" on:click={() => openScreen("title")} title="Back"/>
        </ButtonContainer>
    </BottomButtonWrapper>
</Menu>
