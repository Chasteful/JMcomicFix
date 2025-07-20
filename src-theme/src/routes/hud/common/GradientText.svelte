<script>
    export let text = "Music";
    export let imageUrl = "https://68.media.tumblr.com/f6d55c4c3981fd7409a41f9e9cfa71bf/tumblr_oqy2keV1LD1uha1xeo1_400.gif";


    export let fontSize = "100w px";
    export let fontFamily = "Alibaba";
    export let textTransform = "uppercase";
    export let gradientColors = [
        { color: "hsla(50, 100%, 70%, 0.5)", offset: "10%" },
        { color: "hsla(200, 100%, 60%, 0.5)", offset: "50%" },
        { color: "hsla(320, 100%, 50%, 0.5)", offset: "90%" }
    ];
</script>

<div class="text-wrap" style="
  --font-size: {fontSize};
  --font-family: {fontFamily};
  --text-transform: {textTransform};
">
    <svg viewBox="0 0 500 80">
        <pattern
                id="p-img"
                viewBox="0 0 300 100"
                patternUnits="userSpaceOnUse"
                width="200%"
                height="200%"
                x="-60%"
                y="-10%"
        >
            <image xlink:href={imageUrl} width="300" height="200" />
        </pattern>
        <text
                text-anchor="middle"
                x="50%"
                y="50%"
                dy=".35em"
                class="img-layer"
        >
            {text}
        </text>
        <linearGradient id="gr-overlay" x1="0" y1="0" x2="100%" y2="100%">
            {#each gradientColors as stop}
                <stop stop-color={stop.color} offset={stop.offset} />
            {/each}
        </linearGradient>
        <text
                text-anchor="middle"
                x="50%"
                y="50%"
                dy=".35em"
                class="gradient-layer"
        >
            {text}
        </text>
    </svg>
</div>

<style>
    .text-wrap {
        margin: 0;
        position: relative;
        width: 100%;
    }

    svg {
        position: absolute;
        width: 100%;
        height: auto;
        font: bold var(--font-size) var(--font-family);
        text-transform: var(--text-transform);
    }

    .img-layer {
        fill: url(#p-img);
    }

    .gradient-layer {
        fill: url(#gr-overlay);
    }
</style>
