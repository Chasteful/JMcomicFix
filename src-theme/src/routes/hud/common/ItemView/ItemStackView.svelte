<script>
    import ItemStackDisplay from "./ItemStackDisplay.svelte";
    export let stack;
</script>

<ItemStackDisplay
        {stack}
        showDurability={stack.damage > 0}
        durabilityColorMode={true}
/>
