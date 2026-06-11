<script lang="ts">
    import {onDestroy, onMount} from 'svelte';

    export let skinUrl: string;

    let canvas: HTMLCanvasElement | null = null;
    let skinImage: HTMLImageElement | null = null;

    interface SourceRect {
        sx: number;
        sy: number;
        sw: number;
        sh: number;
    }

    let mounted = false;

    $: if (mounted && skinUrl) {
        load();
    }

    function load() {
        if (!canvas || !skinUrl) return;
        skinImage = new Image();
        skinImage.crossOrigin = 'anonymous';
        skinImage.onload = () => draw();
        skinImage.onerror = () => console.error('Failed to load skin image', skinUrl);
        skinImage.src = skinUrl;
    }

    function draw() {
        if (!canvas || !skinImage) return;
        const ctx = canvas.getContext('2d')!;
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.imageSmoothingEnabled = false;

        const tileSize = 8;
        const scale = 8;
        const tileW = tileSize * scale;
        const tileH = tileW;

        const outerScale = 1.125;
        const outerW = Math.ceil(tileW * outerScale);
        const outerH = outerW;
        const offset = (outerW - tileW) / 2;

        const innerFront: SourceRect = {sx: 8, sy: 8, sw: tileSize, sh: tileSize};
        ctx.drawImage(
            skinImage,
            innerFront.sx,
            innerFront.sy,
            innerFront.sw,
            innerFront.sh,
            offset,
            offset,
            tileW,
            tileH
        );

        const outerFront: SourceRect = {sx: 40, sy: 8, sw: tileSize, sh: tileSize};
        ctx.drawImage(
            skinImage,
            outerFront.sx,
            outerFront.sy,
            outerFront.sw,
            outerFront.sh,
            0,
            0,
            outerW,
            outerH
        );
    }

    onMount(() => {
        mounted = true;
        load();
    });

    onDestroy(() => {
        if (skinImage) {
            skinImage.onload = null;
            skinImage.onerror = null;
        }
    });
</script>

<canvas width="72" height="72" bind:this={canvas} class="scene"></canvas>

<style lang="scss">
  .scene {
    image-rendering: pixelated;
    display: block;
  }
</style>
