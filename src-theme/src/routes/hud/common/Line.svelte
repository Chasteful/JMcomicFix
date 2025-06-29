 <div class="line">
  <svg width="100%" height="2" viewBox="0 0 100 2" preserveAspectRatio="none">
  <defs>
    <linearGradient id="flow-gradient" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="var(--primary-color)" stop-opacity="0.5" />
      <stop offset="50%" stop-color="var(--secondary-color)" stop-opacity="0.5" />
      <stop offset="100%" stop-color="var(--primary-color)" stop-opacity="0.5" />
      <animateTransform
        attributeName="gradientTransform"
        type="translate"
        from="1 0"
        to="-1 0"
        dur="4s"
        repeatCount="indefinite"
      />
    </linearGradient>
  </defs>
  <rect x="0" y="0" width="100%" height="2" fill="url(#flow-gradient)" />
</svg>
</div>
<style lang="scss">
.line {
  margin: 0.5em 0;

  svg {
    display: block;
  }
}
</style>
