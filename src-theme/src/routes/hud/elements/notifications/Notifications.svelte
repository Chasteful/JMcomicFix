<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import { popScale } from "../../../../util/animate_utils";
    import { fly } from "svelte/transition";
    import Notification from "./Notification.svelte";
    import type {NotificationEvent, NotificationSeverity} from "../../../../integration/events";
    import { Howl } from "howler";
    import { onDestroy } from "svelte";
    import type { HudNotificationsSettings } from "../../components";

    interface TNotification {
        animationKey: number;
        id: number;
        severity: NotificationSeverity;
        message: string;
        remaining: number;
        leaving?: boolean;
    }

    export let settings: { [name: string]: any };

    let cSettings: HudNotificationsSettings;

    $: cSettings = settings as HudNotificationsSettings;

    let notifications: TNotification[] = [];

    let globalTimer: ReturnType<typeof setTimeout> | null = null;

    function startGlobalTimer() {
        if (globalTimer) return;
        globalTimer = setInterval(() => {
            if (notifications.length === 0) {
                stopGlobalTimer();
                return;
            }

            notifications = notifications.map(n => {
                if (n.leaving) return n;
                const nextRemaining = +(n.remaining - 0.1).toFixed(1);

                if (nextRemaining <= 0) {

                    triggerLeave(n.id);
                    return { ...n, remaining: 0, leaving: true };
                }
                return { ...n, remaining: nextRemaining };
            });
        }, 100);
    }

    function stopGlobalTimer() {
        if (globalTimer) {
            clearInterval(globalTimer);
            globalTimer = null;
        }
    }

    function triggerLeave(id: number) {
        setTimeout(() => {
            notifications = notifications.filter(n => n.id !== id);
        }, 300);
    }

    const error = new Howl({src: ['audio/notifications/error.ogg'], preload: true});
    const info = new Howl({src: ['audio/notifications/info.ogg'], preload: true});
    const success = new Howl({src: ['audio/notifications/success.ogg'], preload: true});
    const disable = new Howl({src: ['audio/notifications/disable.ogg'], preload: true, volume: 0.5});
    const enable = new Howl({src: ['audio/notifications/enable.ogg'], preload: true, volume: 0.5});
    const blink = new Howl({src: ['audio/notifications/blink.ogg'], preload: true})
    const blinked = new Howl({src: ['audio/notifications/blinked.ogg'], preload: true})

    function addNotification(title: string, message: string, severity: NotificationSeverity) {
        if (!cSettings.severities.includes(severity)) return;

        let animationKey = Date.now();
        const id = animationKey;

        if (message.startsWith("Currently storing")) {
            const existingIndex = notifications.findIndex(n =>
                n.severity === "BLINKING" || n.severity === "BLINKED"
            );
            if (severity === "BLINK") return;
            if (existingIndex !== -1) {
                updateExistingNotification(existingIndex, message, severity);
                return;
            }
        }

        if (severity === "ENABLED" || severity === "DISABLED") {
            const existingIndex = notifications.findIndex(
                n => n.message === message && (n.severity === "ENABLED" || n.severity === "DISABLED")
            );
            if (existingIndex !== -1) {
                updateExistingNotification(existingIndex, message, severity);
                return;
            }
        }

        if (notifications.length > 16) {

            notifications = notifications.slice(0, 16);
        }

        notifications = [
            {
                animationKey,
                id,
                message,
                severity,
                remaining: 3.0
            },
            ...notifications
        ];

        startGlobalTimer();
    }

    function updateExistingNotification(index: number, message: string, severity: NotificationSeverity) {

        notifications[index] = {
            ...notifications[index],
            message,
            severity,
            remaining: 3.0,
            leaving: false
        };
        notifications = notifications;
        startGlobalTimer();
    }

    listen("notification", (e: NotificationEvent) => {
        addNotification(e.title, e.message, e.severity);

        switch (e.severity) {
            case "ERROR": error.play(); break;
            case "INFO": info.play(); break;
            case "SUCCESS": success.play(); break;
            case "ENABLED": enable.play(); break;
            case "DISABLED": disable.play(); break;
            case "BLINK": blink.play(); break;
            case "BLINKED": blinked.play(); break;
        }
    });

    onDestroy(() => {
        stopGlobalTimer();
    });
</script>
<div class="notifications" class:draggable={notifications.length === 0} style="transform: scale({settings.scale});">
    {#each notifications as notification (notification.id)}
        <div
                in:fly={{ x: 50, duration: 200 }}
                out:popScale
        >
            <Notification {...notification}/>
        </div>
    {:else}
        <!-- 空状态下的占位元素 -->
        <div class="empty-placeholder"></div>
    {/each}
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .notifications {
    will-change: transform, opacity;
    transform: translateZ(0);
    backface-visibility: hidden;
    perspective: 1000px;
    display: flex;
    bottom: 25px;
    right: 25px;
    flex-direction: column;
    align-items: flex-end;
    position: absolute;
    min-height: 80px;
    min-width: 400px;
    border-radius: 14px;
    border: 6px dashed transparent;
    transition: background-color, border-color 0.3s ease;

    &:hover {
      background: rgba(204, 204, 204, 0.2);
      border-color: #ccc;


    }

    .empty-placeholder {
      display: none;
    }


    &.draggable {
      cursor: move;

      &:hover {
        border-color: rgba(255, 255, 255, 0.8) !important;
        background: rgba(204, 204, 204, 0.3);
      }
    }
  }
</style>
