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
    <div class="body">
        <div id="body-front"></div>
        <div id="body-back"></div>
        <div id="body-left"></div>
        <div id="body-right"></div>
        <div id="body-bottom"></div>
    </div>
    <div class="slim left-slim-arm">
        <div id="left-slim-arm-front"></div>
        <div id="left-slim-arm-back"></div>
        <div id="left-slim-arm-left"></div>
        <div id="left-slim-arm-top"></div>
        <div id="left-slim-arm-bottom"></div>
    </div>
    <div class="slim right-slim-arm">
        <div id="right-slim-arm-front"></div>
        <div id="right-slim-arm-back"></div>
        <div id="right-slim-arm-right"></div>
        <div id="right-slim-arm-top"></div>
        <div id="right-slim-arm-bottom"></div>
    </div>
    <div class="left-leg">
        <div id="left-leg-front"></div>
        <div id="left-leg-back"></div>
        <div id="left-leg-left"></div>
        <div id="left-leg-top"></div>
        <div id="left-leg-bottom"></div>
    </div>
    <div class="right-leg">
        <div id="right-leg-front"></div>
        <div id="right-leg-back"></div>
        <div id="right-leg-right"></div>
        <div id="right-leg-top"></div>
        <div id="right-leg-bottom"></div>
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

  .body {
    width: 8px;
    height: 12px;
  }

  #body-front {
    background-position: 44px 44px;
    transform: translateZ(2px);
  }

  #body-front::after {
    background-position: 44px 28px;
    transform: translateZ(0.5px);
  }

  #body-back {
    background-position: 32px 44px;
    transform: rotateY(180deg) translateZ(2px);
  }

  #body-back::after {
    background-position: 32px 28px;
    transform: translateZ(0.5px);
  }

  #body-left {
    width: 4px;
    background-position: 36px 44px;
    transform: rotateY(90deg) translateZ(6px);
  }

  #body-left::after {
    content: none;
  }

  #body-right {
    width: 4px;
    background-position: 48px 44px;
    transform: rotateY(-90deg) translateZ(2px);
  }

  #body-right::after {
    content: none;
  }

  #body-bottom {
    width: 8px;
    height: 4px;
    background-position: 36px 48px;
    transform: translateY(10px) rotateX(270deg);
  }

  #body-bottom::after {
    display: none;
  }

  .left-slim-arm {
    position: absolute;
    width: 3px;
    height: 12px;
    transform: translateX(8px) translateY(-12px);
  }

  #left-slim-arm-front {
    background-position: 28px 12px;
    transform: translateZ(2px);
  }

  #left-slim-arm-front::after {
    background-position: 12px 12px;
    transform: translateZ(0.3px);
  }

  #left-slim-arm-back {
    background-position: 21px 12px;
    transform: rotateY(180deg) translateZ(2px);
  }

  #left-slim-arm-back::after {
    background-position: 8px 12px;
    transform: translateZ(0.3px);
  }

  #left-slim-arm-left {
    width: 4px;
    background-position: 25px 12px;
    transform: rotateY(90deg) translateZ(1px);
  }

  #left-slim-arm-left::after {
    background-position: 9px 12px;
    transform: translateZ(0.15px);
  }

  #left-slim-arm-top {
    width: 3px;
    height: 4px;
    background-position: 28px 16px;
    transform: translateY(-2px) rotateX(90deg);
  }

  #left-slim-arm-top::after {
    background-position: 12px 16px;
    transform: translateZ(0.7px);
  }

  #left-slim-arm-bottom {
    width: 3px;
    height: 4px;
    background-position: 25px 16px;
    transform: translateY(10px) rotateX(270deg);
  }

  #left-slim-arm-bottom::after {
    background-position: 14px 16px;
    transform: translateZ(0.7px);
  }

  .right-slim-arm {
    transform-style: preserve-3d;
    position: absolute;
    width: 3px;
    height: 12px;
    transform: translateX(-3px) translateY(-12px);
  }

  #right-slim-arm-front {
    background-position: 20px 44px;
    transform: translateZ(2px);
  }

  #right-slim-arm-front::after {
    background-position: 20px 28px;
    transform: translateZ(0.3px);
  }

  #right-slim-arm-back {
    background-position: 13px 44px;
    transform: rotateY(180deg) translateZ(2px);
  }

  #right-slim-arm-back::after {
    background-position: 13px 28px;
    transform: translateZ(0.3px);
  }

  #right-slim-arm-right {
    width: 4px;
    background-position: 17px 44px;
    transform: rotateY(270deg) translateZ(2px);
  }

  #right-slim-arm-right::after {
    background-position: 17px 28px;
    transform: translateZ(0.15px);
  }

  #right-slim-arm-top {
    width: 3px;
    height: 4px;
    background-position: 20px 48px;
    transform: translateY(-2px) rotateX(90deg);
  }

  #right-slim-arm-top::after {
    background-position: 20px 32px;
    transform: translateZ(0.7px);
  }

  #right-slim-arm-bottom {
    width: 3px;
    height: 4px;
    background-position: 17px 48px;
    transform: translateY(10px) rotateX(270deg);
  }

  #right-slim-arm-bottom::after {
    background-position: 17px 32px;
    transform: translateZ(0.7px);
  }

  .left-leg {
    position: absolute;
    width: 4px;
    height: 12px;
    transform: translateX(4px) translateY(0px);
  }

  #left-leg-front {
    background-position: 44px 12px;
    transform: translateZ(2px);
  }

  #left-leg-front::after {
    background-position: 60px 12px;
    transform: translateZ(0.3px);
  }

  #left-leg-back {
    background-position: 36px 12px;
    transform: rotateY(180deg) translateZ(2px);
  }

  #left-leg-back::after {
    background-position: 52px 12px;
    transform: translateZ(0.3px);
  }

  #left-leg-left {
    width: 4px;
    background-position: 40px 12px;
    transform: rotateY(90deg) translateZ(2px);
  }

  #left-leg-left::after {
    background-position: 56px 12px;
    transform: translateZ(0.15px);
  }

  #left-leg-top {
    width: 4px;
    height: 4px;
    background-position: 44px 16px;
    transform: translateY(-2px) rotateX(90deg);
  }

  #left-leg-top::after {
    display: none;
  }

  #left-leg-bottom {
    width: 4px;
    height: 4px;
    background-position: 40px 16px;
    transform: translateY(10px) rotateX(270deg);
  }

  #left-leg-bottom::after {
    background-position: 56px 16px;
    transform: translateZ(0.7px);
  }

  .right-leg {
    width: 4px;
    height: 12px;
    transform: translateX(0px) translateY(0px);
  }

  #right-leg-front {
    background-position: 60px 44px;
    transform: translateZ(2px);
  }

  #right-leg-front::after {
    background-position: 60px 28px;
    transform: translateZ(0.3px);
  }

  #right-leg-back {
    background-position: 52px 44px;
    transform: rotateY(180deg) translateZ(2px);
  }

  #right-leg-back::after {
    background-position: 52px 28px;
    transform: translateZ(0.3px);
  }

  #right-leg-right {
    width: 4px;
    background-position: 64px 44px;
    transform: rotateY(270deg) translateZ(2px);
  }

  #right-leg-right::after {
    background-position: 64px 28px;
    transform: translateZ(0.15px);
  }

  #right-leg-top {
    width: 4px;
    height: 4px;
    background-position: 60px 48px;
    transform: translateY(-2px) rotateX(90deg);
  }

  #right-leg-top::after {
    display: none;
  }

  #right-leg-bottom {
    width: 4px;
    height: 4px;
    background-position: 56px 48px;
    transform: translateY(10px) rotateX(270deg);
  }

  #left-leg-bottom::after {
    background-position: 56px 32px;
    transform: translateZ(0.7px);
  }
</style>

