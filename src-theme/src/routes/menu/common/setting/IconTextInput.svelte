<script lang="ts">
    export let icon: string;
    export let title: string;
    export let type = "text";
    export let value = "";
    export let maxLength: number | null = null;
    export let pattern: string | null = null;
</script>

<div class="icon-text-input">
    <div class="icon">
        <img src="img/menu/icon-{icon}.svg" alt={icon}>
    </div>
    {#if type === "text"}
        <input {pattern} maxlength={maxLength} class="input" spellcheck="false" type="text" placeholder={title} bind:value={value} autocomplete="off">
    {:else if type === "password"}
        <input {pattern} maxlength={maxLength} class="input" type="password" placeholder={title} bind:value={value} autocomplete="off">
    {/if}
    <div class="button-container">
        <slot />
    </div>
</div>

<style lang="scss">
@use "../../../../colors.scss" as *;

.icon-text-input {
  display: grid;
  grid-template-columns: 64px 1fr auto;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(14px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.icon {
  height: 64px;
  width: 64px;
  background: rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  img {
    width: 28px;
    height: 28px;
    opacity: 0.8;
  }
}

.input {
  color: #e2e6eb;
  font-size: 18px;
  font-weight: 500;
  background: transparent;
  border: none;
  padding: 0 18px;
  outline: none;

  &::placeholder {
    color: rgba(255, 255, 255, 0.4);
  }

  &:invalid {
    border: none;
    box-shadow: inset 0 0 0 2px $menu-error-color;
  }
}

.button-container {
  padding-right: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

</style>