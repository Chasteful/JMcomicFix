<script lang="ts">
    import {onMount} from "svelte";
    import {listen} from "../../../../integration/ws";
    import {getPlayerData} from "../../../../integration/rest";
    import type {ClientPlayerDataEvent} from "../../../../integration/events";
    import type {PlayerData} from "../../../../integration/types";
    import Status from "./Status.svelte";
    import {fade} from "svelte/transition";
    import {calcArmorValue} from "../../../../util/Client/calcArmorValue";

    let playerData: PlayerData | null = null;
    type BarKey = 'armor' | 'air' | 'food';
    const barColors: Record<BarKey, string> = {
        armor: "#49EAD6",
        air: "#AAC1E3",
        food: "#B88458"
    };

    function updatePlayerData(newData: PlayerData) {

        const updatedData = {...newData};

        if (updatedData.armorItems) {
            updatedData.armor = calcArmorValue(updatedData.armorItems);
        }

        playerData = updatedData;
    }

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        updatePlayerData(event.playerData);
    });

    onMount(async () => {
        updatePlayerData(await getPlayerData());
    });

    type StatusBarConfig = {
        key: BarKey;
        condition: () => boolean;
        max: number | (() => number);
        value: () => number | undefined;
        color: string;
        icon?: string;
        label?: () => string | undefined;
        alignRight?: boolean;
        disableAutoColor?: boolean;
    };
    const statusBars: StatusBarConfig[] = [
        {
            key: "food",
            condition: () => playerData?.food !== undefined && playerData.air === playerData.maxAir,
            max: 20,
            value: () => playerData?.food,
            color: barColors.food,
            icon: "food",
            alignRight: false
        },
        {
            key: "armor",
            condition: () => playerData?.armor !== undefined && playerData.air === playerData.maxAir,
            max: 20,
            value: () => playerData?.armor,
            color: barColors.armor,
            icon: "shield",
            disableAutoColor: true,
            alignRight: false
        },
        {
            key: "air",
            condition: () => playerData?.air !== undefined && playerData?.maxAir !== undefined && playerData.air < playerData.maxAir,
            max: () => playerData?.maxAir ?? 0,
            value: () => playerData?.air,
            color: barColors.air,
            alignRight: false
        },
    ];

</script>

{#if playerData && playerData.gameMode !== "spectator"}
    <div class="hotbar" transition:fade={{ duration: 300 }}>
        {#each statusBars as bar (bar.key)}
            {#if bar.condition()}
                <Status
                        max={typeof bar.max === "function" ? bar.max() : bar.max}
                        value={bar.value() ?? 0}
                        color={bar.color}
                        icon={bar.icon}
                        height="24px"
                        borderRadius="12px"
                        label={bar.label?.()}
                        disableAutoColor={bar.disableAutoColor}
                        animate={true}

                />
            {/if}
        {/each}
    </div>
{/if}

<style lang="scss">
  @import "../../../../colors";

  .hotbar {
    display: flex;
    gap: 4px;
    width: 400px;

  }
</style>
