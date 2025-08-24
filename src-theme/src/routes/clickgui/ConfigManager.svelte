<script lang="ts">
    import { onMount } from "svelte";
    import { getConfigs, loadConfig, saveConfig, deleteConfig } from "../../integration/rest";
    import { fade } from "svelte/transition";


    let configs: string[] = [];
    let selectedConfig: string | null = null;

    onMount(async () => {
        try {
            configs = await getConfigs();
            console.log("Configs loaded:", configs);
        } catch (error) {
            console.error("Failed to fetch configs:", error);
        }
    });

    const handleLoad = async (name: string) => {
        await loadConfig(name);
    };

    const handleSave = async () => {
        if (!selectedConfig) return;
        await saveConfig(selectedConfig);
        configs = await getConfigs();
    };

    const handleDelete = async (name: string) => {
        await deleteConfig(name);
        configs = await getConfigs();
    };
</script>

<div class="config-manager" transition:fade>
    <h2>Local Configs</h2>
    {#if configs.length > 0}
        <ul>
            {#each configs as item (item)}
                <li>
                    <span>{item}</span>
                    <button on:click={() => handleLoad(item)}>Load</button>
                    <button on:click={() => handleDelete(item)}>Delete</button>
                </li>
            {/each}
        </ul>
    {:else}
        <p>No configs available</p>
    {/if}

    <input bind:value={selectedConfig} placeholder="Config name" />
    <button on:click={handleSave}>Save</button>
</div>

<style lang="scss">
  .config-manager {
    padding: 10px;
    right: 20px;
    bottom: 10px;
    background: #1a1a1a;
    border-radius: 5px;
    color: #ccc;
    width: 250px;

    h2 {
      margin-bottom: 10px;
      font-size: 16px;
    }

    ul {
      list-style: none;
      padding: 0;
    }

    li {
      display: flex;
      align-items: center;
      margin-bottom: 5px;

      span {
        flex-grow: 1;
        padding: 5px;
      }

      button {
        padding: 5px 10px;
        margin-left: 5px;
        background: #333;
        border: none;
        color: #fff;
        cursor: pointer;

        &:hover {
          background: #444;
        }
      }
    }

    input {
      padding: 5px;
      margin-top: 10px;
      width: 100%;
      box-sizing: border-box;
    }

    button {
      padding: 5px 10px;
      margin-top: 5px;
      background: #333;
      border: none;
      color: #fff;
      cursor: pointer;

      &:hover {
        background: #444;
      }
    }
  }
</style>
