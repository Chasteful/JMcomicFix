<script lang="ts">
    import { onMount } from "svelte"
    import type {
        BlockCountChangeEvent,
        ClientPlayerDataEvent,
        SelectHotbarSlotSilentlyEvent
    } from "../../../integration/events"
    import type { PlayerData, SilentHand } from "../../../integration/types"
    import { listen } from "../../../integration/ws"
    import { getPlayerData, getModules } from "../../../integration/rest"
    import { FadeOut } from "../../../util/animate_utils"
    import { REST_BASE } from "../../../integration/host"

    let silentHand: SilentHand | null = null
    let silentHotbarEnabled = false
    let count: number | undefined
    let playerData: PlayerData | null = null

    listen("blockCountChange", (e: BlockCountChangeEvent) => count = e.count)
    listen("clientPlayerData", (e: ClientPlayerDataEvent) => playerData = e.playerData)
    listen("selectHotbarSlotSilently", (e: SelectHotbarSlotSilentlyEvent) => silentHand = { requester: e.requester, slot: e.slot })
    listen("resetHotbarSlotSilently", () => silentHand = null)
    listen("moduleToggle", () => checkSilentHotbar())

    async function checkSilentHotbar() {
        const modules = await getModules()
        silentHotbarEnabled = modules.some(m => m.name === "SilentHotbar" && m.enabled)
    }

    onMount(async () => {
        playerData = await getPlayerData()
        await checkSilentHotbar()
    })
</script>

{#if silentHotbarEnabled && silentHand}
    {#if count === undefined && playerData?.mainHandStack && playerData.mainHandStack.identifier !== "minecraft:air"}
        <div class="silent-hand-container" out:FadeOut|global={{ duration: 150 }}>
            <div class="item-icon hud-container">
                <img
                        class="icon"
                        src={`${REST_BASE}/api/v1/client/resource/itemTexture?id=${playerData.mainHandStack.identifier}`}
                        alt={playerData.mainHandStack.identifier}
                />
            </div>
        </div>
    {/if}
{/if}


<style lang="scss">
  @import "../../../colors";

  .silent-hand-container {
    display: flex;
    justify-content: center;
    align-items: center;
    position: absolute;
  }

  .item-icon {
    width: 64px;
    height: 64px;
    display: flex;
    justify-content: center;
    align-items: center;

    .icon {
      width: 100%;
      height: 100%;
      object-fit: contain;
      display: block;
    }
  }

</style>
