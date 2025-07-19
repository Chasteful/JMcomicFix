<script lang="ts">
    import {fade} from "svelte/transition";
    import {listen} from "../../../integration/ws";
    import type {ClickGuiValueChangeEvent, OverlayTitleEvent} from "../../../integration/events";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import {TimeoutManager} from "../../../util/Theme/TimeoutManager";
    import type {
        ConfigurableSetting,
        TextArraySetting,
        TextComponent as TTextComponent
    } from "../../../integration/types";
    import {getModules, getModuleSettings} from "../../../integration/rest";
    import {onMount} from "svelte";

    let OverlayTitle: OverlayTitleEvent | null = null;
    const timeouts = new TimeoutManager();
    const OVERLAY_TIMEOUT = 3000;
    let shouldFilter = false;
    let filterKeywords: string[] = [];

    async function checkShouldFilter(): Promise<void> {
        const modules = await getModules();
        shouldFilter = modules.some(module =>
            module.name === "TitleControl" && module.enabled
        );
    }

    function isTextArraySetting(configurable: ConfigurableSetting) {
        const keywordsSetting = configurable.value.find(v => v.name === "Keywords") as TextArraySetting;
        filterKeywords = keywordsSetting?.value ?? [];
    }

    function FilterText(component: TTextComponent | string | undefined): boolean {
        if (!component || !shouldFilter) return false;

        const extractText = (c: TTextComponent | string): string => {
            if (typeof c === "string") return c;
            let result = c.text ?? "";
            if (Array.isArray(c.extra)) {
                for (const child of c.extra) {
                    result += extractText(child);
                }
            }
            return result;
        };

        const flatText = extractText(component).toLowerCase();
        return filterKeywords.some(keyword =>
            flatText.includes(keyword.toLowerCase())
        );
    }

    listen("overlayTitle", (event: OverlayTitleEvent) => {
        if (FilterText(event.title) || FilterText(event.subtitle)) return;

        OverlayTitle = event;
        timeouts.set("overlay", () => OverlayTitle = null, OVERLAY_TIMEOUT);
    });

    onMount(async () => {
        await checkShouldFilter();
        const TitleControlSettings = await getModuleSettings("TitleControl");
        isTextArraySetting(TitleControlSettings);
    });
    listen("titleControlValueChange", (e: ClickGuiValueChangeEvent) => {
        isTextArraySetting(e.configurable);
    });
</script>
<div class="overlay-container">
    {#if OverlayTitle}
        <div class="overlay-message" in:fade={{ duration: 100 }} out:fade={{ duration: 100 }}>
            <div class="title-wrapper">
                {#if OverlayTitle.title}
                    <TextComponent fontSize={50} textComponent={OverlayTitle.title}/>
                {/if}
            </div>

            <div class="subtitle-wrapper">
                {#if OverlayTitle.subtitle}
                    <TextComponent fontSize={36} textComponent={OverlayTitle.subtitle}/>
                {/if}
            </div>
        </div>
    {/if}
</div>


<style lang="scss">
  @import "../../../colors.scss";

  .overlay-container {
    position: fixed;
    top: 0;
    left: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    pointer-events: none;
    width: 100%;
  }

  .overlay-message {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    text-shadow: 1px 1px 2px rgba($base, 0.7);
  }

  .title-wrapper {
    white-space: nowrap;
    padding: 5px;
    font-variant-numeric: tabular-nums;
    min-height: calc(50px + 5px * 2);
    margin-bottom: 30px;
  }

  .subtitle-wrapper {
    white-space: nowrap;
    padding: 5px;
    font-variant-numeric: tabular-nums;
    min-height: calc(36px + 5px * 2);
  }
</style>
