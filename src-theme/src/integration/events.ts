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
export interface Event {
}
export interface ClickGuiValueChangeEvent extends Event {
    configurable: ConfigurableSetting;
}

export interface ModuleToggleEvent extends Event  {
    moduleName: string;
    hidden: boolean;
    enabled: boolean;
}

export interface KeyboardKeyEvent extends Event {
    keyCode: number;
    scanCode: number;
    action: number;
    mods: number;
    key: string;
    screen: Screen | undefined;
}

export interface MouseButtonEvent extends Event  {
    key: string;
    button: number;
    action: number;
    mods: number;
    screen: Screen | undefined;
}

export interface ScaleFactorChangeEvent extends Event  {
    scaleFactor: number;
}

export interface ComponentsUpdateEvent extends Event  {
    components: Component[];
}

export interface ClientPlayerDataEvent extends Event  {
    playerData: PlayerData;
}

export interface OverlayMessageEvent extends Event  {
    text: TextComponent | string;
    tinted: boolean;
}

export interface ConnectionDetailsEvent extends Event  {

    result: TextComponent | string;

}

export interface OverlayTitleEvent extends Event  {
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

export interface OverlayPlayListEvent  extends Event {
    header: TextComponent | string;
    footer: TextComponent | string;
    players: PlayerEntry[];

}

export interface OverlayChatEvent  extends Event {
    content: TextComponent | string,
    timestamp: number;
    isSystem: Boolean;
    id: number
    visible: boolean
    fadeTimeout?: number;
}

export interface OverlayDisconnectionEvent  extends Event  {
    parent: "title" | "menu" | "custom";
    info: TextComponent | string;
}
export interface ChatReceiveEvent  extends Event {
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



export interface NotificationEvent  extends Event  {
    title: string;
    message: string;
    severity: "INFO" | "SUCCESS" | "ERROR" | "ENABLED" | "DISABLED" | "BLINK" | "BLINKED" | "BLINKING";
}

export interface ProgressEvent  extends Event  {
    title: string;
    progress: number;
    maxProgress: number;
    timeRemaining: number;
}

export interface KeyEvent  extends Event {
    key: string;
    action: number;
    mods: number;

}

export interface TargetChangeEvent  extends Event {
    target: PlayerData | null;
}

export interface BlockCountChangeEvent  extends Event {
    count?: number;
}

export interface AccountManagerAdditionEvent  extends Event {
    username: string | null;
    error: string | null;
}

export interface AccountManagerMessageEvent extends Event  {
    message: string;
}

export interface AccountManagerLoginEvent  extends Event {
    username: string | null;
    error: string | null;
}

export interface ServerPingedEvent extends Event  {
    server: Server;
}

export interface PlayerInventoryEvent  extends Event {
    inventory: PlayerInventory;
}

export interface PlayerInventory  extends Event {
    armor: ItemStack[];
    main: ItemStack[];
    crafting: ItemStack[];
    enderChest: ItemStack[];
    openChest: ItemStack[];
}

export interface ProxyAdditionResultEvent extends Event  {
    proxy: Proxy | null;
    error: string | null;
}

export interface ProxyEditResultEvent  extends Event {
    proxy: Proxy | null;
    error: string | null;
}

export interface ProxyCheckResultEvent  extends Event {
    proxy: Proxy;
    error: string | null;
}

export interface SpaceSeperatedNamesChangeEvent  extends Event {
    value: boolean;
}

export interface BrowserUrlChangeEvent  extends Event {
    url: string;
}

export interface SelectHotbarSlotSilentlyEvent extends Event {
    requester: any;
    slot: number;
}
