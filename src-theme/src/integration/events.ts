import type {
    Component,
    ConfigurableSetting,
    ItemStack,
    PlayerData,
    Proxy,
    Screen,
    Server,
    TextComponent
} from "./types";
import {types} from "sass";
import List = types.List;

export interface ClickGuiValueChangeEvent {
    configurable: ConfigurableSetting;
}

export interface ModuleToggleEvent {
    moduleName: string;
    hidden: boolean;
    enabled: boolean;
}

export interface KeyboardKeyEvent {
    keyCode: number;
    scanCode: number;
    action: number;
    mods: number;
    key: string;
    screen: Screen | undefined;
}

export interface MouseButtonEvent {
    key: string;
    button: number;
    action: number;
    mods: number;
    screen: Screen | undefined;
}

export interface ScaleFactorChangeEvent {
    scaleFactor: number;
}

export interface ComponentsUpdateEvent {
    components: Component[];
}

export interface ClientPlayerDataEvent {
    playerData: PlayerData;
}

export interface OverlayMessageEvent {
    text: TextComponent | string;
    tinted: boolean;
}

export interface ConnectionDetailsEvent {

    result: TextComponent | string;

}

export interface OverlayTitleEvent {
    title: TextComponent | string;
    subtitle: TextComponent | string;
}

export interface PlayerEntry {
    name: TextComponent | string;
    uuid: string;
    latency: TextComponent | string;
    isFriend: boolean;
    isStaff: boolean;
}

export interface OverlayPlayListEvent {
    header: TextComponent | string;
    footer: TextComponent | string;
    players: PlayerEntry[];

}

export interface OverlayChatEvent {
    content: TextComponent | string,
    timestamp: number;
    isSystem: Boolean;
    id: number
    visible: boolean
    fadeTimeout?: number;
}

export interface OverlayDisconnectionEvent {
    parent: "title" | "menu" | "custom";
    info: TextComponent | string;
}
export interface ChatReceiveEvent {
    message: string;
    textData: TextComponent | string;
    type: ChatType;
    applyChatDecoration: (text: TextComponent | string) => TextComponent | string;
    cancelled?: boolean;
}

export enum ChatType {
    CHAT_MESSAGE = "ChatMessage",
    DISGUISED_CHAT_MESSAGE = "DisguisedChatMessage",
    GAME_MESSAGE = "GameMessage"
}



export interface NotificationEvent {
    title: string;
    message: string;
    severity: "INFO" | "SUCCESS" | "ERROR" | "ENABLED" | "DISABLED" | "BLINK" | "BLINKED" | "BLINKING";
}

export interface KeyEvent {
    key: string;
    action: number;
    mods: number;

}

export interface TargetChangeEvent {
    target: PlayerData | null;
}

export interface BlockCountChangeEvent {
    count?: number;
}

export interface AccountManagerAdditionEvent {
    username: string | null;
    error: string | null;
}

export interface AccountManagerMessageEvent {
    message: string;
}

export interface AccountManagerLoginEvent {
    username: string | null;
    error: string | null;
}

export interface ServerPingedEvent {
    server: Server;
}

export interface PlayerInventoryEvent {
    inventory: PlayerInventory;
}

export interface PlayerInventory {
    armor: ItemStack[];
    main: ItemStack[];
    crafting: ItemStack[];
}

export interface ProxyAdditionResultEvent {
    proxy: Proxy | null;
    error: string | null;
}

export interface ProxyEditResultEvent {
    proxy: Proxy | null;
    error: string | null;
}

export interface ProxyCheckResultEvent {
    proxy: Proxy;
    error: string | null;
}

export interface SpaceSeperatedNamesChangeEvent {
    value: boolean;
}

export interface BrowserUrlChangeEvent {
    url: string;
}
