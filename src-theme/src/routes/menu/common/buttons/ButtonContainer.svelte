<script>
</script>

<div class="button-container">
    <slot />
</div>

<style lang="scss">

    .button-container {
      background-color: var(--menu-button-container-background-color);
      box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
      padding: 10px 24px;
      width: max-content;
      border-radius: 16px;
      display: flex;
      column-gap: 20px;
    }
</style>
