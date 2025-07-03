<script lang="ts">
    import {fly} from "svelte/transition";
    import {notification, type TNotification} from "./notification_store";
    import {onMount} from "svelte";
    import {get} from "svelte/store";

    let currentNotification: TNotification | null = null;
    let showNotification = false;
    let timeoutHandle: ReturnType<typeof setTimeout> | null = null;

    onMount(() => {
        if (get(notification)) notification.set(null);

        const unsubscribe = notification.subscribe((n) => {
            if (n) {

                if (timeoutHandle) {
                    clearTimeout(timeoutHandle);
                    timeoutHandle = null;
                }

                currentNotification = n;
                showNotification = true;

                timeoutHandle = setTimeout(() => {
                    showNotification = false;
                    timeoutHandle = null;
                }, (n.delay ?? 3) * 1000);
            } else {
                showNotification = false;
            }
        });

        return () => {
            unsubscribe();
            if (timeoutHandle) clearTimeout(timeoutHandle);
        };
    });
</script>


<div class="notifications">
    {#if showNotification && currentNotification}
        {#key currentNotification.id || currentNotification.message}
            <div class="notification"
                 transition:fly|global={{duration: 500, y: -100}}
                 on:outroend={() => {
                        if (!showNotification) currentNotification = null;
                    }}
            >
                <div class="icon" class:error={currentNotification.error}>
                    <img src="img/hud/notification/icon-info.svg" alt="info">
                </div>
                <div class="title">{currentNotification.title}</div>
                <div class="message">{currentNotification.message}</div>
            </div>
        {/key}
    {/if}
</div>
<style lang="scss">
  @use "../../../../colors.scss" as *;

  .notifications {
    display: grid;
    grid-template-columns: 1fr;
  }

  .notification {
    grid-row-start: 1;
    grid-column-start: 1;
    background: rgba(255, 255, 255, 0.05);
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
    border-radius: 5px;
    display: grid;
    grid-template-areas:
        "a b"
        "a c";
    grid-template-columns: max-content 1fr;
    overflow: hidden;
    padding-right: 10px;
    min-width: 350px;

    .title {
      color: $text;
      font-weight: 600;
      font-size: 18px;
      grid-area: b;
      align-self: flex-end;
    }

    .message {
      color: $text-color;
      font-weight: 500;
      grid-area: c;
    }

    .icon {
      grid-area: a;
      height: 65px;
      width: 65px;
      background-color: rgba(255, 255, 255, 0.1);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 10px;
    }
  }
</style>
