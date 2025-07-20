<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import {fade} from "svelte/transition";
    import type {ProgressEvent} from "../../../../integration/events";
    import {WindowSize} from "../../../../util/WindowSize";
    import {onMount} from "svelte";

    let progressEvent: ProgressEvent | null = null;
    const {width, destroy} = WindowSize();
    let progressWidth = 0;

    $: progressWidth = $width * 0.4;

    listen("progress", (e: ProgressEvent) => {
        progressEvent = e;
        // Auto-hide after 3 seconds if progress completes
        if (e.progress >= e.maxProgress) {
            setTimeout(() => {
                progressEvent = null;
            }, 100);
        }
    });

    onMount(() => {
        return destroy;
    });
</script>

{#if progressEvent}
    <div
            class="progress-container"
            style="width: {progressWidth}px;"
            transition:fade={{ duration: 200 }}
    >
        <div class="progress-header">
            <span class="progress-title">{progressEvent.title}</span>
            <span class="progress-percent">
                {Math.round((progressEvent.progress / progressEvent.maxProgress) * 100)}%
            </span>
        </div>
        <div class="progress-bar">
            <div
                    class="progress-fill"
                    style="width: {(progressEvent.progress / progressEvent.maxProgress) * 100}%;"
            />
        </div>
    </div>
{/if}

<style lang="scss">
  @import "../../../../colors.scss";

  .progress-container {
    position: fixed;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%) scale(1);
    height: 50px;
    min-height: 50px;
    background: color-mix(
                    in srgb,
                    rgba(20, 20, 20, 0.5) 0%,
                    rgba(darken($base, 5%), 0.5) 100%
    );
    border-radius: 14px;
    padding: 12px 16px;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.03) inset;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    z-index: 1000;
    transition: all 0.3s ease;
  }

  .progress-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }

  .progress-title {
    font-size: 14px;
    font-weight: 600;
    color: $text;
  }

  .progress-percent {
    font-size: 14px;
    font-weight: 600;
    color: var(--primary-color);
  }

  .progress-bar {
    height: 8px;
    flex-shrink: 0;
    width: 100%;
    background: color-mix(in srgb, rgba(20, 20, 20) 30%, transparent);
    border-radius: 4px;
    overflow: hidden;
  }

  .progress-fill {
    height: 8px !important;
    border-radius: 4px;
    background: color-mix(
                    in srgb,
                    var(--primary-color) 70%,
                    transparent 30%
    );
    transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  }
</style>
