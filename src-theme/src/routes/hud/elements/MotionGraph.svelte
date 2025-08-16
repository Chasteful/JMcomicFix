<script lang="ts">
    import {onDestroy} from 'svelte';
    import {getBPS} from "../../../util/movement_utils";

    export let width: number = 250;
    export let amplitude: number = 50;
    export let yOffset: number = -210;

    let speeds: number[] = [];
    let maxSpeed: number = 0.1;
    let pathD: string = '';

    const updateGraph = () => {
        const MAX_POINTS = 100;
        if (speeds.length > MAX_POINTS) {
            speeds = speeds.slice(-MAX_POINTS);
        }
        maxSpeed = Math.max(maxSpeed * 0.95, ...speeds);

        if (speeds.length >= 2) {
            updatePath();
        }
    };

    function updatePath() {
        const points = speeds.map((speed, i) => ({
            x: (i / (speeds.length - 1)) * width,
            y: (1 - Math.min(speed / maxSpeed, 1)) * amplitude
        }));

        let d = `M ${points[0].x},${points[0].y}`;
        points.slice(1).forEach((p, i) => {
            const prev = points[i];
            const cp1 = {x: prev.x + (p.x - prev.x) * 0.5, y: prev.y};
            const cp2 = {x: prev.x + (p.x - prev.x) * 0.5, y: p.y};
            d += ` C ${cp1.x},${cp1.y} ${cp2.x},${cp2.y} ${p.x},${p.y}`;
        });

        pathD = d;
    }

    const interval = setInterval(() => {
        speeds = [...speeds, getBPS.current];
        updateGraph();
    }, 50);

    onDestroy(() => {
        clearInterval(interval);
    });
</script>

<div class="graph-container" style={`--yOffset: ${yOffset}px`}>
    <svg class="motion-graph" height={amplitude} width={width}>

        <defs>
            <mask height={amplitude} id="fade-mask" maskUnits="userSpaceOnUse" width={width} x="0" y="0">

                <linearGradient id="fade-gradient" x1="0%" x2="100%" y1="0%" y2="0%">
                    <stop offset="0%" stop-color="white" stop-opacity="0"/>
                    <stop offset="10%" stop-color="white" stop-opacity="1"/>
                    <stop offset="90%" stop-color="white" stop-opacity="1"/>
                    <stop offset="100%" stop-color="white" stop-opacity="0"/>
                </linearGradient>

                <rect fill="url(#fade-gradient)" height={amplitude} width={width} x="0" y="0"/>
            </mask>
        </defs>

        <g mask="url(#fade-mask)">
            <path class="base-path" d={pathD}/>
            <path class="highlight-path" d={pathD}/>
        </g>
    </svg>
</div>

<style>
    .graph-container {
        display: flex;
        flex-direction: column;
        left: 50%;
        bottom: var(--yOffset);
        transform: translateX(-50%);
        pointer-events: none;
    }

    .motion-graph {
        filter: drop-shadow(0 2px 10px rgba(255, 255, 255, 0.7));
    }

    .base-path {
        fill: none;
        stroke: rgba(255, 255, 255, 0.6);
        stroke-width: 4;
        stroke-linecap: round;
    }

    .highlight-path {
        fill: none;
        stroke: rgba(255, 255, 255, 1);
        stroke-width: 2;
        stroke-linecap: round;
    }
</style>
