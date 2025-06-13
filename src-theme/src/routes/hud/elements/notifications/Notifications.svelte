<script lang="ts">
    import { listen } from "../../../../integration/ws";
    import Notification from "./Notification.svelte";
    import type { NotificationEvent } from "../../../../integration/events";
    import { Howl } from "howler";
    import { elasticOut } from "svelte/easing";


    interface TNotification {
        animationKey: number;
        id: number;
        severity: string;
        message: string;
        remaining?: number;
        intervalId?: NodeJS.Timeout;
        timeoutId?: NodeJS.Timeout;
        leaving?: boolean;
    }
    let notifications: TNotification[] = [];
    const error = new Howl({ src: ['audio/notifications/error.mp3'], preload: true });
    const info = new Howl({ src: ['audio/notifications/info.mp3'], preload: true });
    const success = new Howl({ src: ['audio/notifications/success.ogg'], preload: true });
    const disable = new Howl({ src: ['audio/notifications/disable.ogg'], preload: true , volume: 0.5 });
    const enable = new Howl({ src: ['audio/notifications/enable.ogg'], preload: true, volume: 0.5 });


    function addNotification(title: string, message: string, severity: string) {
        const animationKey = Date.now();
        let id = animationKey;

        if (severity === "ENABLED" || severity === "DISABLED") {
            const existingIndex = notifications.findIndex(
                n => n.message === message && (n.severity === "ENABLED" || n.severity === "DISABLED")
            );

            if (existingIndex !== -1) {
                // 复用现有通知的ID和DOM元素
                const existing = notifications[existingIndex];
                id = existing.id;  // 关键点：保持相同ID

                // 清除旧定时器
                clearInterval(existing.intervalId);
                clearTimeout(existing.timeoutId);

                // 创建新定时器（保持倒计时重置）
                let remaining = 3.0;
                const intervalId = setInterval(() => {
                    remaining = +(remaining - 0.1).toFixed(1);
                    notifications = notifications.map(n =>
                        n.id === id ? { ...n, remaining } : n
                    );
                }, 100);

                const timeoutId = setTimeout(() => {
                    clearInterval(intervalId);
                    notifications = notifications.map(n =>
                        n.id === id ? { ...n, leaving: true } : n
                    );
                    setTimeout(() => {
                        notifications = notifications.filter(n => n.id !== id);
                    }, 10);
                }, 3000);

                // 更新通知属性（保持相同ID和animationKey）
                notifications[existingIndex] = {
                    ...existing,
                    severity,
                    remaining: 3.0,
                    intervalId,
                    timeoutId,
                    leaving: false
                };

                notifications = notifications; // 触发响应式更新
                return;
            }
        }

        // 新通知创建逻辑
        let remaining = 3.0;
        const intervalId = setInterval(() => {
            remaining = +(remaining - 0.1).toFixed(1);
            notifications = notifications.map(n =>
                n.id === id ? { ...n, remaining } : n
            );
        }, 100);

        const timeoutId = setTimeout(() => {
            clearInterval(intervalId);
            notifications = notifications.map(n =>
                n.id === id ? { ...n, leaving: true } : n
            );
            setTimeout(() => {
                notifications = notifications.filter(n => n.id !== id);
            }, 10);
        }, 3000);

        notifications = [
            {
                animationKey,
                id,
                message,
                severity,
                remaining,
                intervalId,
                timeoutId
            },
            ...notifications
        ];
    }

    listen("notification", (e: NotificationEvent) => {
      addNotification(e.title, e.message, e.severity);
      switch (e.severity) {
          case "ERROR": error.play(); break;
          case "INFO": info.play(); break;
          case "SUCCESS": success.play(); break;
          case "ENABLED": enable.play(); break;
          case "DISABLED": disable.play();break;
      }
    });
    function notificationFly(node: Element, { delay = 0, duration = 600 } = {}) {
      return {
        delay,
        duration,
        easing: elasticOut,
        css: (t: number, u: number) => `
          transform:
            scale(${0.5 + 0.5 * t * t})
            translateY(${Math.sin(u * Math.PI) * 30}px)
            rotate(${(1 - t) * 8}deg);
          opacity: ${t * t};
        `
      };
    }
    function notificationOut(node: Element, { delay = 0, duration = 300 } = {}) {
    return {
      delay,
      duration,
      css: (t: number) => {
               const eased = easeInBack(1 - t);
        return `
          transform:
            translateY(${eased * 100}px)
            scale(${1 - eased * 0.5});
          opacity: ${1 - eased};
          transition-timing-function: cubic-bezier(0.68, -0.55, 0.27, 1.55);
          transform-origin: top center;
        `;
      }
    };
  }
   function easeInBack(t: number): number {
    const c1 = 1.5;    const c3 = c1 + 1;
    return c3 * t * t * t - c1 * t * t;
  }

  </script>
<div class="notifications" class:draggable={notifications.length === 0}>
    {#each notifications as notification (notification.id)}
        <div
                in:notificationFly
                out:notificationOut
        >
            <Notification {...notification} />
        </div>
    {:else}
        <!-- 空状态下的占位元素 -->
        <div class="empty-placeholder" />
    {/each}
</div>

<style lang="scss">
  @import "../../../../colors.scss";
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
    transition: background-color,border-color 0.3s ease;

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
