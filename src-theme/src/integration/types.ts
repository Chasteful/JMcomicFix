export interface Module {
    name: string;
    category: string;
    enabled: boolean;
    description: string;
    hidden: boolean;
    aliases: string[];
    tag: string | null;

}

export interface GroupedModules {
    [category: string]: Module[]
}

export type ModuleSetting =
    SearchSetting
    | BooleanSetting
    | FloatSetting
    | FloatRangeSetting
    | IntSetting
    | IntRangeSetting
    | ChoiceSetting
    | ChooseSetting
    | MultiChooseSetting
    | ConfigurableSetting
    | TogglableSetting
    | ColorSetting
    | TextSetting
    | TextArraySetting
    | BindSetting
    | VectorSetting
    | KeySetting;

export interface SearchSetting {
    valueType: string;
    name: string;
    value: string[];
}

export interface KeySetting {
    valueType: string;
    name: string;
    value: string;
}

export interface BindSetting {
    valueType: string;
    name: string;
    value: {
        printableKeyName: string;
        boundKey: string;
        action: string;
    };
    defaultValue: {
        boundKey: string;
        action: "Toggle" | "Hold";
        printableKeyName: string;
    };
}

export interface TextSetting {
    valueType: string;
    name: string;
    value: string;
}

export interface VectorSetting {
    valueType: string;
    name: string;
    value: Vec3;
}

export interface TextArraySetting {
    valueType: string;
    name: string;
    value: string[];
}

export interface ColorSetting {
    valueType: string;
    name: string;
    value: number;
}

export interface BooleanSetting {
    valueType: string;
    name: string;
    value: boolean;
}

export interface FloatSetting {
    valueType: string;
    name: string;
    range: {
        from: number;
        to: number;
    };
    suffix: string;
    value: number;
}

export interface FloatRangeSetting {
    valueType: string;
    name: string;
    range: {
        from: number;
        to: number;
    };
    suffix: string;
    value: {
        from: number,
        to: number
    };
}

export interface IntSetting {
    valueType: string;
    name: string;
    range: {
        from: number;
        to: number;
    };
    suffix: string;
    value: number;
}

export interface IntRangeSetting {
    valueType: string;
    name: string;
    range: {
        from: number;
        to: number;
    };
    suffix: string;
    value: {
        from: number,
        to: number
    };
}

export interface ChoiceSetting {
    valueType: string;
    name: string;
    active: string;
    choices: { [name: string]: ModuleSetting }
    value: ModuleSetting[];
}

export interface ChooseSetting {
    valueType: string;
    name: string;
    choices: string[];
    value: string;
}

export interface MultiChooseSetting {
    valueType: string;
    name: string;
    choices: string[];
    value: string[];
    canBeNone: boolean;
}

export interface ConfigurableSetting {
    valueType: string;
    name: string;
    value: ModuleSetting[];
}

export interface TogglableSetting {
    valueType: string;
    name: string;
    value: ModuleSetting[];
}

export interface PersistentStorageItem {
    key: string;
    value: string;
}

export interface VirtualScreen {
    name: string;
}

export interface Scoreboard {
    header: TextComponent;
    entries: {
        name: TextComponent;
        score: TextComponent;
    }[];
}

export interface Vec3 {
    x: number;
    y: number;
    z: number;
}

export interface PlayerData {
    username: string;
    uuid: string;
    position: Vec3;
    blockPosition: Vec3;
    velocity: Vec3;
    yaw: number;
    pitch: number;
    dimension: string;
    selectedSlot: number;
    gameMode: string;
    health: number,
    actualHealth: number;
    maxHealth: number;
    absorption: number;
    armor: number;
    food: number;
    air: number;
    ping: number;
    maxAir: number;
    experienceLevel: number;
    experienceProgress: number;
    killsCount: number;
    deathCount: number;
    playTime: number;
    effects: StatusEffect[];
    mainHandStack: ItemStack;
    offHandStack: ItemStack;
    armorItems: ItemStack[];
    scoreboard: Scoreboard;
    serverAddress: String,
    isDead: boolean,
    isEating: boolean;
    eatingStartTime: number;
    eatingMaxDuration: number;
}

export interface StatusEffect {
    id: any;
    effect: string;
    localizedName: string;
    duration: number;
    amplifier: number;
    ambient: boolean;
    infinite: boolean;
    visible: boolean;
    showIcon: boolean;
    color: number;
}


export interface ItemStack {
    identifier: string;
    count: number;
    damage: number;
    maxDamage: number;
    displayName: TextComponent | string;
    /**
     * @deprecated use {@link enchantments} instead.
     */
    hasEnchantment: boolean;
    enchantments?: Record<string, number>;
    hasDyedColor: boolean;
    dyedColor?: number;
}

export interface PrintableKey {
    translationKey: string;
    localized: string;
}

export interface MinecraftKeybind {
    bindName: string;
    key: PrintableKey;
}

export interface Registries {
    blocks: {
        identifier: string;
        name: string;
    }[] | undefined;
    items: {
        identifier: string;
        name: string;
    }[] | undefined;
}

export interface Session {
    username: string;
    accountType: string;
    avatar: string;
    premium: boolean;
    uuid: string;
}

export interface Verification {
    isDev: boolean;
    isOwner: boolean;
    hwid: string;
    developer: string;
    avatar: string;
}

export interface Server {
    id: number;
    address: string;
    icon: string;
    label: TextComponent | string;
    players: {
        max: number;
        online: number;
    };
    name: string;
    online: boolean;
    playerCountLabel: string;
    protocolVersion: number;
    version: string;
    ping: number;
    resourcePackPolicy: string;
}

export interface TextComponent {
    type?: string;
    extra?: (TextComponent | string)[];
    color: string;
    bold?: boolean;
    italic?: boolean;
    underlined?: boolean;
    strikethrough?: boolean;
    obfuscated?: boolean;
    font?: string;
    text: string;
}

export interface Protocol {
    name: string;
    version: number;
}

export interface Account {
    avatar: string;
    favorite: boolean;
    id: number;
    type: string;
    username: string;
    uuid: string;
}

export interface World {
    id: number;
    name: string;
    displayName: string;
    lastPlayed: number;
    gameMode: string;
    difficulty: string;
    icon: string | undefined;
    hardcore: boolean;
    commandsAllowed: boolean;
    version: string;
}

export interface Proxy {
    id: number;
    host: string;
    port: number;
    forwardAuthentication: boolean;
    favorite: boolean;
    credentials: {
        username: string;
        password: string;
    } | undefined;
    ipInfo: {
        city?: string;
        country?: string;
        ip: string;
        loc?: string;
        org?: string;
        postal?: string;
        region?: string;
        timezone?: string;
    } | undefined;
}

export interface GameWindow {
    width: number;
    height: number;
    scaledWidth: number;
    scaledHeight: number;
    scaleFactor: number;
    guiScale: number;
}

export interface Component {
    name: string;
    mode: string;
    settings: { [name: string]: any };
}

export interface ClientInfo {
    gameVersion: string;
    clientVersion: string;
    clientName: string;
    development: boolean;
    fps: number;
    gameDir: string;
    inGame: boolean;
    viaFabricPlus: boolean;
    hasProtocolHack: boolean;
}

export interface ClientUpdate {
    development: boolean;
    commit: string;
    update: {
        buildId: number | undefined;
        commitId: string | undefined;
        branch: string | undefined;
        clientVersion: string | undefined;
        minecraftVersion: string | undefined;
        release: boolean;
        date: string;
        message: string;
        url: string;
    } | undefined;
}

export interface Browser {
    url: string
}

export interface HitResult {
    type: "block" | "entity" | "miss";
    pos: Vec3;
}

export interface BlockHitResult extends HitResult {
    blockPos: Vec3;
    side: string;
    isInsideBlock: boolean;
}

export interface EntityHitResult extends HitResult {
    entityName: string;
    entityType: string;
    entityPos: Vec3;
}

export interface GeneratorResult {
    name: string;
}

export interface Screen {
    class: string,
    title: string,
}
