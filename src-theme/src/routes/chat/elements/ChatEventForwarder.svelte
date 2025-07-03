<script lang="ts">
    import {onMount} from 'svelte';
    import {chatEvents} from "./chatEventStore";

    function handleKeyDown(e: KeyboardEvent) {

        if (e.key === 'Escape' || e.key === 'Enter') {
            e.preventDefault();
        }
        chatEvents.keydown.set(e);
    }

    function handleMouseWheel(e: WheelEvent) {

        if (document.querySelector('.chat-hud.focused')) {
            e.preventDefault();
        }
        chatEvents.wheel.set(e);
    }

    onMount(() => {

        window.addEventListener('keydown', handleKeyDown);
        window.addEventListener('wheel', handleMouseWheel, {passive: false});

        return () => {
            window.removeEventListener('keydown', handleKeyDown);
            window.removeEventListener('wheel', handleMouseWheel);
        };
    });
</script>
