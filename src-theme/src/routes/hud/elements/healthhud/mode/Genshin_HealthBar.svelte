<script lang="ts">
    import { listen } from "../../../../../integration/ws";
    import { getPlayerData } from "../../../../../integration/rest";
    import type { PlayerData } from "../../../../../integration/types";
    import type { ClientPlayerDataEvent } from "../../../../../integration/events";
    import { onDestroy, onMount, tick } from "svelte";
    import { cubicOut, expoInOut } from 'svelte/easing';
    import { fly } from "svelte/transition";
    import { hsvToRgba } from "../../../../../util/color_utils";
    import { Tween } from "svelte/motion";

    let blink = false;
    let showHealthBar = false;
    let rafId: number | null = null;
    let playerData: PlayerData | null = null;
    let canvas: HTMLCanvasElement | null = null;
    let ctx: CanvasRenderingContext2D | null = null;
    let iv: ReturnType<typeof setInterval> | null = null;

    let healthVal = 0;
    let absorptionVal = 0;
    let maxHealthVal = 1;
    let prevHealthVal = 0;
    let prevAbsorptionVal = 0;

    const BAR_WIDTH = 420;
    const BAR_HEIGHT = 14;
    const SHADOW_BLUR = 8;
    const SHADOW_OFFSET = 4;

    const healthTweened = new Tween(0, { duration: 300, easing: cubicOut });
    const absorptionTweened = new Tween(0, { duration: 300, easing: cubicOut });
    const maxHealthTweened = new Tween(1, { duration: 300, easing: cubicOut });
    const prevHealthTweened = new Tween(0, { duration: 800, easing: cubicOut });
    const prevAbsorptionTweened = new Tween(0, { duration: 1000, easing: cubicOut });

    function fmt(n: number): string {
        const rounded = Math.round(n);
        return Math.abs(n - rounded) < 0.05 ? `${rounded}` : n.toFixed(1);
    }

    function lerp(a: number, b: number, t: number) {
        return a + (b - a) * t;
    }

    function updatePlayerData(s: PlayerData) {
        playerData = s;
        healthTweened.target = s.health;
        absorptionTweened.target = s.absorption;
        maxHealthTweened.target = s.maxHealth;
        prevHealthTweened.target = s.health;
        prevAbsorptionTweened.target = s.absorption;
    }

    async function showDelayed() {
        await tick();
        await new Promise(res => setTimeout(res, 500));
        showHealthBar = true;
    }

    function drawDiamondPath(context: CanvasRenderingContext2D, x: number, y: number, w: number, h: number) {
        const midY = y + h / 2;
        const edgeWidth = w * 0.02;

        context.beginPath();
        context.moveTo(x + edgeWidth, y);
        context.lineTo(x + w - edgeWidth, y);
        context.lineTo(x + w, midY);
        context.lineTo(x + w - edgeWidth, y + h);
        context.lineTo(x + edgeWidth, y + h);
        context.lineTo(x, midY);
        context.closePath();
    }

    function drawHealthBar() {
        if (!canvas || !ctx) return;

        const dpr = window.devicePixelRatio || 1;
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        const x = SHADOW_OFFSET * dpr;
        const y = SHADOW_OFFSET * dpr;
        const w = BAR_WIDTH * dpr;
        const h = BAR_HEIGHT * dpr;
        const currentIsLowHealth = maxHealthVal > 0 && (healthVal / maxHealthVal <= 0.25);

        const pulse = currentIsLowHealth ? (Math.sin(performance.now() / 600) + 1) / 2 : 0;

        ctx.save();
        const shadowR = lerp(0, 255, pulse);
        const shadowA = lerp(0.4, 0.7, pulse);

        ctx.shadowColor = `rgba(${shadowR}, 0, 0, ${shadowA})`;
        ctx.shadowBlur = SHADOW_BLUR * dpr;
        ctx.shadowOffsetX = SHADOW_OFFSET * 0.5 * dpr;
        ctx.shadowOffsetY = SHADOW_OFFSET * 0.5 * dpr;

        drawDiamondPath(ctx, x, y, w, h);
        ctx.fillStyle = "rgba(0, 0, 0, 0.3)";
        ctx.fill();
        ctx.restore();

        ctx.save();
        drawDiamondPath(ctx, x, y, w, h);
        ctx.clip();

        const total = Math.max(healthVal + absorptionVal, maxHealthVal, 1);
        const healthPct = Math.min(Math.max(healthVal / total, 0), 1);
        const absorbPct = Math.min(Math.max(absorptionVal / total, 0), 1);
        const prevHealthPct = Math.min(Math.max(prevHealthVal / total, 0), 1);
        const prevAbsorbPct = Math.min(Math.max(prevAbsorptionVal / total, 0), 1);

        const curPct = healthPct + absorbPct;
        const prevPct = prevHealthPct + prevAbsorbPct;

        const bgGradient = ctx.createLinearGradient(x, y, x, y + h);
        if (currentIsLowHealth) {
            bgGradient.addColorStop(0.3, `rgba(255, 50, 50, ${lerp(0.05, 0.20, pulse)})`);
            bgGradient.addColorStop(1, `rgba(40, 0, 0, ${lerp(0.35, 0.65, pulse)})`);
        } else {
            bgGradient.addColorStop(0.3, "rgba(165,200,55,0.1)");
            bgGradient.addColorStop(1, "rgba(0,0,0,0.4)");
        }

        ctx.fillStyle = bgGradient;
        ctx.fillRect(x, y, w, h);

        const hpColor = currentIsLowHealth ? hsvToRgba(4, 60, 100, 0.7) : hsvToRgba(82, 68, 84, 0.7);
        const abColor = "rgb(212,175,55)";
        const tsColor =  "rgb(195,180,115)";

        if (prevPct > curPct && prevPct > 0) {
            ctx.fillStyle = prevAbsorptionVal > absorptionVal ? abColor : tsColor;
            ctx.globalAlpha = 1.0;
            ctx.fillRect(x + w * curPct, y, w * (prevPct - curPct), h);
            ctx.globalAlpha = 1.0;
        }

        if (curPct > 0) {
            const barGradient = ctx.createLinearGradient(x, y, x + w * curPct, y);
            barGradient.addColorStop(0, hpColor);

            if (healthPct > 0 && healthPct < curPct) {
                const midPoint = healthPct / curPct;
                const transitionWidth = 0.05;
                barGradient.addColorStop(Math.max(0, midPoint - transitionWidth / 2), hpColor);
                barGradient.addColorStop(Math.min(1, midPoint + transitionWidth / 2), abColor);
            }

            barGradient.addColorStop(1, curPct > healthPct ? abColor : hpColor);
            ctx.fillStyle = barGradient;
            ctx.fillRect(x, y, w * curPct, h);
        }
        ctx.restore();

        ctx.save();
        drawDiamondPath(ctx, x, y, w, h);
        ctx.strokeStyle = "rgba(0,0,0,0.35)";
        ctx.lineWidth = 1.5 * dpr;
        ctx.stroke();
        ctx.restore();

        ctx.save();
        drawDiamondPath(ctx, x, y, w, h);
        ctx.strokeStyle = "rgba(0,0,0,0.35)";
        ctx.lineWidth = 1.5 * dpr;
        ctx.stroke();
        ctx.restore();

        ctx.save();
        ctx.font = `bold ${14 * dpr}px "Genshin", sans-serif`;
        ctx.fillStyle = "#fff";
        ctx.textBaseline = "middle";
        ctx.shadowColor = "rgba(0, 0, 0, 0.9)";
        ctx.shadowBlur = 3 * dpr;

        const centerY = y + h / 2;
        const separatorX = x + w / 2;

        let currentText = fmt(healthVal);
        if (absorptionVal > 0) currentText += ` +${fmt(absorptionVal)}`;

        const fullText = `${currentText} / ${fmt(maxHealthVal)}`;

        ctx.textAlign = "center";
        ctx.fillText(fullText, separatorX, centerY);

        ctx.restore();
    }

    function animate() {
        const changed =
            healthVal !== healthTweened.current ||
            absorptionVal !== absorptionTweened.current ||
            maxHealthVal !== maxHealthTweened.current ||
            prevHealthVal !== prevHealthTweened.current ||
            prevAbsorptionVal !== prevAbsorptionTweened.current;

        const currentIsLowHealth = maxHealthVal > 0 && (healthVal / maxHealthVal <= 0.25);

        healthVal = healthTweened.current;
        absorptionVal = absorptionTweened.current;
        maxHealthVal = maxHealthTweened.current;
        prevHealthVal = prevHealthTweened.current;
        prevAbsorptionVal = prevAbsorptionTweened.current;

        if (changed || currentIsLowHealth) {
            drawHealthBar();
        }

        rafId = requestAnimationFrame(animate);
    }

    $: if (canvas) {
        ctx = canvas.getContext('2d');
        const dpr = window.devicePixelRatio || 1;
        const cssWidth = BAR_WIDTH + SHADOW_OFFSET * 2;
        const cssHeight = BAR_HEIGHT + SHADOW_OFFSET * 2;

        canvas.style.width = `${cssWidth}px`;
        canvas.style.height = `${cssHeight}px`;
        canvas.width = cssWidth * dpr;
        canvas.height = cssHeight * dpr;

        drawHealthBar();
    }

    $: isLowHealth = playerData ? (playerData.health / playerData.maxHealth <= 0.25) : false;

    $: {
        if (isLowHealth && !iv) {
            iv = setInterval(() => (blink = !blink), 500);
        } else if (!isLowHealth && iv) {
            clearInterval(iv);
            iv = null;
            blink = false;
        }
    }

    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        updatePlayerData(e.playerData);
    });

    onMount(async () => {
        updatePlayerData(await getPlayerData());
        await showDelayed();
        animate();
    });

    onDestroy(() => {
        if (iv) clearInterval(iv);
        if (rafId) cancelAnimationFrame(rafId);
    });
</script>

{#if showHealthBar && playerData && playerData.gameMode !== "spectator"}
    <div class="health-bar" transition:fly|global={{ duration: 500, y: 50, easing: expoInOut }}>
        {#if playerData.gameMode !== "creative"}
            <div class="status-container">
                <div class="status-wrapper">
                    <div class="level-stat">Lv. {playerData.experienceLevel}</div>
                    <canvas bind:this={canvas} class="health-canvas"></canvas>
                </div>
            </div>
        {/if}
    </div>
{/if}

<style lang="scss">
  .health-bar {
    display: flex;
    justify-content: center;
    align-items: center;
    margin-bottom: 6px;
    font-family: "Genshin", sans-serif;
  }

  .status-container {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
  }

  .status-wrapper {
    position: relative;
    width: 420px;
    height: 14px;
    display: flex;
    align-items: center;
  }

  .level-stat {
    position: absolute;
    right: calc(100% + 12px);
    font-size: 14px;
    color: rgba(255, 255, 255, 0.85);
    text-shadow: 0 0 2px rgba(0, 0, 0, 0.9),
    0 0 4px rgba(0, 0, 0, 0.7),
    1px 1px 2px rgba(0, 0, 0, 0.6),
    -1px -1px 2px rgba(0, 0, 0, 0.6);
    white-space: nowrap;
    line-height: 1;
    top: 50%;
    transform: translateY(-50%);
  }

  .health-canvas {
    position: absolute;
    left: -4px;
    top: -4px;
    display: block;
    image-rendering: crisp-edges;
    pointer-events: none;
  }
</style>
