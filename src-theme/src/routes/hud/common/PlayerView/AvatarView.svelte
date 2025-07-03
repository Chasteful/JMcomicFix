<script lang="ts">
    export let skinUrl: string;

</script>


<div class="scene" style={`--skin-url: url('${skinUrl}');`}>
    <div class="head">
        <div id="head-front"></div>
        <div id="head-back"></div>
        <div id="head-left"></div>
        <div id="head-right"></div>
        <div id="head-top"></div>
        <div id="head-bottom"></div>
    </div>
</div>

<style lang="scss">

  .scene {
    position: relative;
    transform-style: preserve-3d;
    height: fit-content;
    image-rendering: pixelated;

    > * {
      transform-style: preserve-3d;
    }
  }

  .scene div div {
    image-rendering: pixelated;
    position: absolute;
    height: 100%;
    width: 100%;
    transform-style: preserve-3d;

  }

  .scene div div::after {
    content: " ";
    position: absolute;
    align-self: center;
    height: 100%;
    width: 100%;
    scale: 1.125;
    background: #ffffff01;
    image-rendering: pixelated;
  }

  .scene div div,
  .scene div div:after {
    background-image: var(--skin-url);
  }

  .head {
    height: 8px;
    width: 8px;
    border-radius: 8px;
  }

  #head-front {
    background-position: 56px 56px;
    transform: translateZ(4px);
  }

  #head-front::after {
    background-position: 24px 56px;
    transform: translateZ(0.5px);
  }

  #head-back {
    background-position: 40px 56px;
    transform: rotateY(180deg) translateZ(4px);
  }

  #head-back::after {
    background-position: 8px 56px;
    transform: translateZ(0.5px);
  }

  #head-left {
    background-position: 48px 56px;
    transform: rotateY(90deg) translateZ(4px);
  }

  #head-left::after {
    background-position: 16px 56px;
    transform: translateZ(0.5px);
  }

  #head-right {
    background-position: 64px 56px;
    transform: rotateY(-90deg) translateZ(4px);
  }

  #head-right::after {
    background-position: 32px 56px;
    transform: translateZ(0.5px);
  }

  #head-top {
    background-position: 56px 64px;
    transform: translateY(-4px) rotateX(90deg);
  }

  #head-top::after {
    background-position: 24px 64px;
    transform: translateZ(0.5px);
  }

  #head-bottom {
    background-position: 48px 64px;
    transform: translateY(4px) rotateX(270deg) rotate(180deg);
  }

  #head-bottom::after {
    background-position: 16px 64px;
    transform: translateZ(0.5px);
  }
</style>

