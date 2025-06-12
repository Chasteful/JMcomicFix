<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { primaryRgb, secondaryRgb } from '../../../components/ThemeManager';
    export let src = 'img/LiquidBounce.svg';
    export let size = 225;
    // svelte-ignore export_let_unused
    export let baseColor = '#ffffff';
    let redLayer: HTMLImageElement;
    let blueLayer: HTMLImageElement;
    let intervalId: ReturnType<typeof setInterval>;
    let timeoutId: ReturnType<typeof setTimeout>;

    function startGlitchLoop() {
      const layers = [redLayer, blueLayer].filter(Boolean) as HTMLImageElement[];
      intervalId = setInterval(() => {
        layers.forEach((layer) => {
          const tx = Math.random() * 20 - 10;
          const ty = Math.random() * 20 - 10;
          layer.style.transform = `translate(${tx}px, ${ty}px)`;
          const x = Math.random() * 100;
          const y = Math.random() * 100;
          const w = Math.random() * 20 + 20;
          const h = Math.random() * 20 + 20;
          layer.style.clipPath = `polygon(${x}% ${y}%, ${x + w}% ${y}%, ${x + w}% ${y + h}%, ${x}% ${y + h}%)`;
        });
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
          layers.forEach((layer) => {
            layer.style.transform = '';
            layer.style.clipPath = '';
          });
        }, 150);
      }, 450);
    }
    onMount(() => {
      startGlitchLoop();
    });
    onDestroy(() => {
      clearInterval(intervalId);
      clearTimeout(timeoutId);
    });
    const delays = Array(8).fill(0).map(() => Math.random() * 100);
    const durations = Array(8).fill(0).map(() => 100 + Math.random() * 300);

    const randomClipPaths = Array(3).fill(0).map(() => {
      const top = Math.random() * 60;
      const bottom = top + 20 + Math.random() * 30;
      return `polygon(0 ${top}%, 100% ${top}%, 100% ${bottom}%, 0 ${bottom}%)`;
    });
    let mounted = false;
  onMount(() => {

    requestAnimationFrame(() => mounted = true);
  });
  </script>
  <div 
   class="cyberpunk-watermark {mounted ? 'animate' : ''}"
    style={`--size: ${size}px; --accent1: ${primaryRgb}; --accent2: ${secondaryRgb}; --base: ${baseColor};`}
  >
    <img class="layer base" src={src} alt="logo-base" />
    <img class="layer neon-effect" src={src} alt="logo-neon"
         style={`animation-delay: ${delays[0]}ms; animation-duration: ${durations[0]}ms`} />
    <img class="layer glitch" src={src} alt="logo-glitch" 
         style={`animation-delay: ${delays[1]}ms; animation-duration: ${durations[1]}ms`} />
    <img class="layer glitch-partial" src={src} alt="logo-glitch-partial" 
         style={`animation-delay: ${delays[2]}ms; animation-duration: ${durations[2]}ms`} />
    <!-- Additional random localized glitch layers -->
    <img class="layer glitch-localized-1" src={src} alt="logo-glitch-local-1" 
         style={`animation-delay: ${delays[3]}ms; animation-duration: ${durations[3]}ms; clip-path: ${randomClipPaths[0]}`} />
    <img class="layer glitch-localized-2" src={src} alt="logo-glitch-local-2" 
         style={`animation-delay: ${delays[4]}ms; animation-duration: ${durations[4]}ms; clip-path: ${randomClipPaths[1]}`} />
    <img class="layer glitch-localized-3" src={src} alt="logo-glitch-local-3" 
         style={`animation-delay: ${delays[5]}ms; animation-duration: ${durations[5]}ms; clip-path: ${randomClipPaths[2]}`} />
    <div class="holographic-effect" 
         style={`animation-delay: ${delays[6]}ms; animation-duration: ${durations[6]}ms`}></div>
    <img bind:this={redLayer} class="layer glitch-dynamic red" src={src} alt="glitch-red" />
    <img bind:this={blueLayer} class="layer glitch-dynamic blue" src={src} alt="glitch-blue" />
  </div>
  <style>
    .cyberpunk-watermark {
      --size: 165px;
      position: relative;
      left: 10px;
      width: var(--size);
      height: var(--size);
      pointer-events: none;
      filter: contrast(1.2);
      animation: zoomInDown 0.5s ease-out both;
    }
    .layer {
      position: absolute;
      width: 100%;
      height: 100%;
      top: 0;
      left: 0;
      object-fit: contain;
    }
    .base {
      z-index: 1;
      opacity: 1;
      animation: base-flicker 2s infinite, random-disappear 2s infinite;
    }
    .neon-effect {
      z-index: 2;
      mix-blend-mode: screen;
      opacity: 0;
      animation: neon-pulse 0.3s infinite, color-shift 1s infinite;
    }
    .glitch {
      z-index: 4;
      opacity: 0;
      animation: glitch-effect 0.1s infinite;
      clip-path: polygon(0 0, 100% 0, 100% 60%, 0 60%);
    }
    .glitch-dynamic.red {
      filter: drop-shadow(-2px 0 0 var(--accent1));
      mix-blend-mode: lighten;
    }
    .glitch-dynamic.blue {
      filter: drop-shadow(2px 0 0 var(--accent2));
      mix-blend-mode: lighten;
    }
    .glitch-partial {
      z-index: 5;
      opacity: 0;
      animation: glitch-partial 0.1s infinite;
      clip-path: polygon(0 20%, 100% 20%, 100% 80%, 0 80%);
    }

    .glitch-localized-1, .glitch-localized-2, .glitch-localized-3 {
      z-index: 6;
      opacity: 0;
      animation: glitch-localized 0.2s infinite;
      mix-blend-mode: lighten;
    }
    .glitch-localized-1 {
      filter: hue-rotate(30deg) brightness(1.5);
    }
    .glitch-localized-2 {
      filter: hue-rotate(180deg) brightness(1.2);
    }
    .glitch-localized-3 {
      filter: hue-rotate(300deg) brightness(1.3);
    }
    .holographic-effect {
      position: absolute;
      width: 100%;
      height: 100%;
      z-index: 7;
      background: 
        radial-gradient(
          circle at center,
          rgba(255, 255, 255, 0.1) 0%,
          transparent 70%
        );
      opacity: 0;
      animation: holographic-pulse 0.1s infinite;
      mix-blend-mode: hard-light;
    }
    @keyframes zoomInDown {
  from {
    opacity: 0;
    transform: scale3d(0.1, 0.1, 0.1) translate3d(0, -1000px, 0);
    animation-timing-function: cubic-bezier(0.55, 0.055, 0.675, 0.19);
  }

  60% {
    opacity: 1;
    transform: scale3d(0.475, 0.475, 0.475) translate3d(0, 60px, 0);
    animation-timing-function: cubic-bezier(0.175, 0.885, 0.32, 1);
  }
}
    @keyframes base-flicker {
      0%, 80%, 100% { opacity: 1; }
      85%, 90%, 95% { opacity: 0.1; }
      97% { opacity: 0; }
    }
    @keyframes random-disappear {
      0%, 100% { opacity: 1; }
      20%, 60% { opacity: 0; }
    }
    @keyframes neon-pulse {
      0%, 100% { opacity: 0.2; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(1.05); }
    }
    @keyframes color-shift {
      0%, 100% { filter: hue-rotate(0deg); }
      50% { filter: hue-rotate(90deg); }
    }
    @keyframes glitch-effect {
      0%, 100% { opacity: 0; transform: translate(0); }
      5% { opacity: 0.6; transform: translate(-10px, 5px); }
      10% { opacity: 0.8; transform: translate(8px, -4px); }
      20% { opacity: 0.5; transform: translate(-5px, 3px); }
      30% { opacity: 0.9; transform: translate(7px, 2px); }
      40% { opacity: 0; transform: translate(0); }
    }
    @keyframes glitch-partial {
      0%, 100% { opacity: 0; transform: translateY(0); }
      10% { opacity: 0.9; transform: translateY(-10px); clip-path: polygon(0 25%, 100% 25%, 100% 75%, 0 75%); }
      20% { opacity: 0.7; transform: translateY(10px); clip-path: polygon(0 30%, 100% 30%, 100% 70%, 0 70%); }
      30% { opacity: 1; transform: translateY(0); clip-path: polygon(0 40%, 100% 40%, 100% 60%, 0 60%); }
    }
    @keyframes glitch-localized {
      0%, 70% { opacity: 0; transform: translate(0, 0); }
      5% { opacity: 0.8; transform: translate(-5px, 3px); }
      10% { opacity: 0.6; transform: translate(5px, -3px); }
      15% { opacity: 0.9; transform: translate(-3px, 2px); }
      20% { opacity: 0.7; transform: translate(3px, -2px); }
      25% { opacity: 0.5; transform: translate(-2px, 1px); }
      30% { opacity: 0; transform: translate(0, 0); }
    }

    @keyframes holographic-pulse {
      0%, 100% { opacity: 0; transform: scale(1); }
      50% { opacity: 0.2; transform: scale(1.05); }
    }
  </style>
