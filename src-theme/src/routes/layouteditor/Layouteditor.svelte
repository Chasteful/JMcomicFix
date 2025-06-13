<script lang="ts">
    import { onMount } from "svelte";

    export let componentId: string;
    export let defaultPosition = { x: 0, y: 0 };
    export let snapToGrid = false;
    export let gridSize = 10;
    export let disabled = false;
    export let hudZoom = 100;

    let position = { ...defaultPosition };
    let moving = false;
    let dragStart = { mouseX: 0, mouseY: 0, elemX: 0, elemY: 0 };

    onMount(() => {
        const saved = localStorage.getItem(`hud-pos-${componentId}`);
        if (saved) position = JSON.parse(saved);
    });

    function savePosition() {
        localStorage.setItem(`hud-pos-${componentId}`, JSON.stringify(position));
    }

    function startDrag(e: MouseEvent) {
        if (disabled || e.button !== 0) return;
        dragStart.mouseX = e.clientX;
        dragStart.mouseY = e.clientY;
        dragStart.elemX = position.x;
        dragStart.elemY = position.y;

        moving = true;
        window.addEventListener("mousemove", handleDrag);
        window.addEventListener("mouseup", stopDrag);
    }

    function handleDrag(e: MouseEvent) {
        if (!moving) return;
        const scale = hudZoom / 100;

        let dx = (e.clientX - dragStart.mouseX) / scale;
        let dy = (e.clientY - dragStart.mouseY) / scale;

        let newX = dragStart.elemX + dx;
        let newY = dragStart.elemY + dy;

        if (snapToGrid) {
            newX = Math.round(newX / gridSize) * gridSize;
            newY = Math.round(newY / gridSize) * gridSize;
        }

        position = { x: newX, y: newY };
    }

    function stopDrag() {
        moving = false;
        savePosition();
        window.removeEventListener("mousemove", handleDrag);
        window.removeEventListener("mouseup", stopDrag);
    }
</script>

<div
        class="draggable"
        class:draggable={!disabled}
        on:mousedown={startDrag}
        style="
        position: absolute;
        left: {position.x}px;
        top: {position.y}px;
        {disabled ? 'pointer-events: none;' : ''}
    "
>

    <div class="hud-content-wrapper">
        <slot />
    </div>
</div>

<style>
    .draggable {
        cursor: move;
        user-select: none;
    }

    .hud-content-wrapper {
        display: inline-block;
        padding: 8px;
    }
</style>
