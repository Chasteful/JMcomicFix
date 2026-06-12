import type {
    BedState,
    ConfigurableSetting,
    HudComponent,
    ItemStack,
    MinecraftKey,
    MinecraftKeyboardKey,
    MinecraftMouseKey,
    PlayerData,
    Proxy,
    Screen,
    Server,
    Session,
    Setting,
    StatusEffect,
    TextComponent,
} from "./types";


export interface EventMap {
    socketReady: void;
    themeColorChange: ThemeColorChangeEvent;
    hudValueChange:HudValueChangeEvent;
    nameProtectValueChange: NameProtectEvent;
    clickGuiScaleChange: ClickGuiScaleChangeEvent;
    clickGuiValueChange: ClickGuiValueChangeEvent;
    spaceSeperatedNamesChange: SpaceSeperatedNamesChangeEvent;
    clientLanguageChanged: void;
    valueChanged: ValueChangedEvent;
    moduleActivation: ModuleActivationEvent;
    moduleToggle: ModuleToggleEvent;
    refreshArrayList: void;
    notification: NotificationEvent;
    gameModeChange: GameModeChangeEvent;
    targetChange: TargetChangeEvent;
    blockCountChange: BlockCountChangeEvent;
    bedStateChange: BedStateChangeEvent;
    clientChatStateChange: ClientChatStateChangeEvent;
    clientChatMessage: ClientChatMessageEvent;
    clientChatError: ClientChatErrorEvent;
    accountManagerMessage: AccountManagerMessageEvent;
    accountManagerLogin: AccountManagerLoginEvent;
    accountManagerAddition: AccountManagerAdditionEvent;
    accountManagerRemoval: AccountManagerRemovalEvent;
    proxyCheckResult: ProxyCheckResultEvent;
    virtualScreen: VirtualScreenEvent;
    serverPinged: ServerPingedEvent;
    componentsUpdate: ComponentsUpdateEvent;
    scaleFactorChange: ScaleFactorChangeEvent;
    browserUrlChange: BrowserUrlChangeEvent;
    userLoggedIn: void;
    userLoggedOut: void;

    //WindowEvents.kt
    mouseButton: MouseButtonEvent;
    keyboardKey: KeyboardKeyEvent;
    keyboardChar: KeyboardCharEvent;

    //UserInterfaceEvents.kt
    fps: FpsChangeEvent;
    clientPlayerData: ClientPlayerDataEvent;
    clientPlayerEffect: ClientPlayerEffectEvent;
    clientPlayerInventory: ClientPlayerInventoryEvent;
    title: TitleEventTitle;
    subtitle: TitleEventSubtitle;
    titleFade: TitleEventFade;
    clearTitle: TitleEventClear;
    connectionDetails: ConnectionDetailsEvent;

    //GameEvents.kt
    key: KeyEvent;
    keybindChange: void;
    session: SessionEvent;
    chatSend: ChatSendEvent;
    chatReceive: ChatReceiveEvent;
    disconnect: void;
    progress: ProgressEvent;
    keyBindingCPS:KeyBindingCPSEvent;
    overlayTitle: OverlayTitleEvent;
    overlayPlayList: OverlayPlayListEvent;
    overlayChatMessage: OverlayChatEvent;
    overlayDisconnection: OverlayDisconnectionEvent;
    overlayMessage: OverlayMessageEvent;

    //PlayerEvents.kt
    death: void;
}

export interface ThemeColorChangeEvent {
    themeId: string;
    name: string;
    value: number;
}

export interface ClickGuiValueChangeEvent {
    configurable: ConfigurableSetting;
}

export interface HudValueChangeEvent{
    configurable: ConfigurableSetting;
}


export interface NameProtectEvent {
    value: boolean;
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
    key: MinecraftKeyboardKey;
    screen: Screen | undefined;
}

export interface MouseButtonEvent {
    key: MinecraftMouseKey;
    button: number;
    action: number;
    mods: number;
    screen: Screen | undefined;
}

export interface KeyboardCharEvent {
    codePoint: number;
}

export interface ScaleFactorChangeEvent {
    scaleFactor: number;
}

export interface ComponentsUpdateEvent {
    id: string | null;
    components: HudComponent[];
}

export interface ClientPlayerDataEvent {
    playerData: PlayerData;
}

export interface ClientPlayerEffectEvent {
    effects: StatusEffect[];
}

export interface OverlayMessageEvent {
    text: TextComponent | string;
    tinted: boolean;
}

export interface NotificationEvent {
    title: string;
    message: string;
    severity: "INFO" | "SUCCESS" | "ERROR" | "ENABLED" | "DISABLED" | "BLINK" | "BLINKED" | "BLINKING";
}

export interface ProgressEvent {
    title: string;
    progress: number;
    maxProgress: number;
    timeRemaining: number;
}

export interface KeyEvent {
    key: MinecraftKey;
    action: number;
}

export interface KeyBindingCPSEvent {
    key: string;
    cps: number;
}

export interface TargetChangeEvent {
    target: PlayerData | null;
    distant:number;
}

export interface BlockCountChangeEvent {
    nextBlock?: string;
    count?: number;
}

export interface BedStateChangeEvent {
    bedStates: BedState[];
}

export interface AccountManagerAdditionEvent {
    username: string | null;
    error: string | null;
}

export interface AccountManagerRemovalEvent {
    username: string | null;
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

export interface ClientPlayerInventoryEvent {
    inventory: PlayerInventory;
}

export interface PlayerInventory {
    armor: ItemStack[];
    main: ItemStack[];
    crafting: ItemStack[];
    enderChest: ItemStack[];
    openChest: ItemStack[];
}

export interface ProxyCheckResultEvent {
    proxy: Proxy | null;
    error: string | null;
}

export interface SpaceSeperatedNamesChangeEvent {
    value: boolean;
}

export interface BrowserUrlChangeEvent {
    index: number;
    url: string;
}

export interface ValueChangedEvent {
    value: Setting<any>;
}

export interface ClickGuiScaleChangeEvent {
    value: number;
}

export interface ModuleActivationEvent {
    moduleName: string;
}

export interface GameModeChangeEvent {
    gameMode: "survival" | "creative" | "adventure" | "spectator";
}

export interface ClientChatStateChangeEvent {
    state: "connecting" | "connected" | "logon" | "loggedIn" | "disconnected" | "authenticationFailed";
}

export interface ClientChatMessageEvent {
    user: {
        name: string;
        uuid: string;
    };
    message: string;
    chatGroup: "PublicChat" | "PrivateChat";
    // Not "public"/"private" because the EnumChoiceSerializer in Kotlin ignores @SerializedName annotations, bug?
}

export interface ClientChatErrorEvent {
    error: string;
}

export interface SessionEvent {
    session: Session;
}

export interface ChatSendEvent {
    message: string;
}

export interface ChatReceiveEvent {
    message: string;
    textData: TextComponent;
    type: "ChatMessage" | "DisguisedChatMessage" | "GameMessage";
}

export interface FpsChangeEvent {
    fps: number;
}

export interface TitleEventTitle {
    text: TextComponent | string | null;
}

export interface TitleEventSubtitle {
    text: TextComponent | string | null;
}

export interface TitleEventFade {
    fadeInTicks: number;
    stayTicks: number;
    fadeOutTicks: number;
}

export interface TitleEventClear {
    reset: boolean;
}

export interface VirtualScreenEvent {
    type: string;
    action: "open" | "close";
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
    isSelf: boolean;
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
    fadeTimeout?: number;
}

export interface OverlayDisconnectionEvent  {
    parent: "title" | "menu" | "custom";
    info: TextComponent | string;
}
