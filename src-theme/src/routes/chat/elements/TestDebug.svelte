<script lang="ts">
    import {onDestroy} from 'svelte';
    import {chatEvents} from "./chatEventStore";
    import {get} from 'svelte/store';

    let lastWheel: WheelEvent | null = null;
    let lastKey: KeyboardEvent | null = null;
    let storeUpdates = 0;
    let debugContainer: HTMLDivElement;

    const unsubWheel = chatEvents.wheel.subscribe(e => {
        lastWheel = e;
        storeUpdates++;
        if (debugContainer && e) {
            debugContainer.scrollTop += e.deltaY;
        }
    });

    const unsubKey = chatEvents.keydown.subscribe(e => {
        lastKey = e;
        storeUpdates++;
    });

    function checkStore() {
        console.log('当前Store状态:');
        console.log('Wheel:', get(chatEvents.wheel));
        console.log('Keydown:', get(chatEvents.keydown));
    }

    onDestroy(() => {
        unsubWheel();
        unsubKey();
    });
</script>

<style>
    .debug {
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: rgba(0, 0, 0, 0.85);
        color: white;
        padding: 1rem;
        border-radius: 8px;
        font-size: 1rem;
        z-index: 9999;
        text-align: center;
        width: 300px;
        height: 200px;
        overflow-y: auto;
        box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
        font-family: monospace;

        &:hover {
            color: red;
        }
    }

    .status {
        margin-top: 10px;
        padding-top: 10px;
        border-top: 1px solid #444;
    }
</style>

<div bind:this={debugContainer} class="debug">
    <h3>事件调试器</h3>

    <div class="event">
        <strong>滚轮事件:</strong>
        <div>deltaY: {lastWheel ? lastWheel.deltaY : '无'}</div>
        <div>时间: {lastWheel ? lastWheel.timeStamp : ''}</div>
    </div>

    <div class="event">
        <strong>按键事件:</strong>
        <div>按键: {lastKey ? lastKey.key : '无'}</div>
        <div>代码: {lastKey ? lastKey.code : ''}</div>
    </div>

    <div class="status">
        <div>Store更新次数: {storeUpdates}</div>
        <button on:click={checkStore}>检查Store</button>
    </div>

    <!-- 模拟很多行用于测试滚动效果 -->
    {#each Array(30) as _, i}
        <div>模拟行 {i + 1}</div>
    {/each}
</div>
