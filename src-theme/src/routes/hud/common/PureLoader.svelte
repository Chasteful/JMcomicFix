<style lang="scss">
  @use "sass:math";
  @import "../../../colors.scss";
  $r: 6.5em;
  $b: 1em;
  $t: 0.625s;
  $a: 2 * $b / $r * 180 / math.$pi * 1deg;

  $c: #569fff, #00d2d3, #54a0ff, #5f27cd;

  @property --a0 {
    syntax: '<angle>';
    initial-value: 0deg;
    inherits: true;
  }

  @property --da {
    syntax: '<angle>';
    initial-value: 0deg;
    inherits: true;
  }

  @property --dy {
    syntax: '<length>';
    initial-value: 0px;
    inherits: false;
  }

  @property --fy {
    syntax: '<number>';
    initial-value: 1;
    inherits: false;
  }

  :global(body) {
    display: grid;
    overflow: hidden;
    margin: 0;

    &::before {
      background:
              Radial-Gradient(closest-side,
                      nth($c, 1) 50%, rgba(nth($c, 1), 0) 90%) 50% / #{2 * $r} #{2 * $r} no-repeat,
              conic-gradient($c, nth($c, 1));
      content: '';
    }
  }

  div, ::before, ::after {
    display: grid;
    grid-column: 1;
    grid-row: 1;
    border-radius: 50%;
  }

  .loader {
    border-radius: 0;
    background: #000;
    filter: contrast(9);
    mix-blend-mode: multiply;

    &__comp {
      place-self: center;
      filter: blur(9px);

      &--arc {
        --da: #{$a};
        transform: rotate(calc(var(--a0) - #{0.5 * $a}));
        animation:
                rot 2 * $t linear infinite,
                exp $t ease-out infinite alternate;

        &::before, &::after {
          place-self: center;
          content: '';
        }

        &::before {
          border: solid $b transparent;
          padding: $r;
          background:
                  radial-gradient(circle at 50% #{0.5 * $b},
                          #fff #{0.5 * $b}, transparent 0),
                  conic-gradient(#fff var(--da, 30deg), transparent 0%);
          background-origin: border-box;
          --full: linear-gradient(red, red);
          mask: var(--full) padding-box exclude, var(--full);
        }

        &::after {
          width: $b;
          height: $b;
          border-radius: 50%/90% 90% 10% 10%;
          transform: rotate(var(--da)) translateY(-($r + 0.5 * $b));
          background: #fff;
        }
      }

      &--drop {
        padding: 0.8125 * $b;
        transform-origin: 50% 100%;
        transform:
                translateY(calc(var(--dy) - #{$r}))
                scaleY(var(--fy));
        background: #fff;
        animation:
                mov 2 * $t ease-in infinite,
                vis 2 * $t ease-in infinite;
      }
    }
  }

  @keyframes rot {
    to {
      --a0: 360deg;
    }
  }

  @keyframes exp {
    to {
      --da: 90deg;
    }
  }

  @keyframes mov {
    35%, 100% {
      --dy: #{2 * $r - 0.5 * $b};
    }
  }

  @keyframes vis {
    35% {
      --fy: 1.5;
    }
    50%, 100% {
      --fy: 0;
    }
  }
</style>

<div class="loader">
    <div class="loader__comp loader__comp--arc"></div>
    <div class="loader__comp loader__comp--drop"></div>
</div>
