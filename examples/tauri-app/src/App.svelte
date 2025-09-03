<script lang="ts">
  import { onMount } from 'svelte';
  import { requestPermission, checkPermission } from 'tauri-plugin-audio-permissions'
  import { trace, info, error, attachConsole } from '@tauri-apps/plugin-log'

	let permissionStatus = $state('unknown');
	let isLoading = $state(false);
	let logs = $state([]);
	let autoCheckComplete = $state(false);

	// Derived states using $derived
	let statusClass = $derived({
		'unknown': 'status-unknown',
		'granted': 'status-granted', 
		'denied': 'status-denied',
		'error': 'status-error'
	}[permissionStatus] || 'status-unknown');

	let statusText = $derived({
		'unknown': 'Unknown',
		'granted': 'Granted ✅',
		'denied': 'Denied ❌', 
		'error': 'Error ⚠️'
	}[permissionStatus] || 'Unknown');

	function addLog(message, type = 'info') {
		const timestamp = new Date().toLocaleTimeString();
		info(`[${timestamp}] ${message}`);
	}

	async function performPermissionCheck() {
		isLoading = true;
		addLog('Checking current permission status...', 'info');
		
		try {
			const result = await checkPermission();
			addLog(`Permission check result: ${JSON.stringify(result)}`, 'success');
			permissionStatus = result.granted ? 'granted' : 'denied';
			return result.granted;
		} catch (error) {
			addLog(`Permission check failed: ${error}`, 'error');
			permissionStatus = 'error';
			return false;
		} finally {
			isLoading = false;
		}
	}

	async function performPermissionRequest() {
		isLoading = true;
		addLog('Requesting audio permission...', 'info');
		
		try {
			const result = await requestPermission();
			addLog(`Permission request result: ${JSON.stringify(result)}`, 'success');
			permissionStatus = result.granted ? 'granted' : 'denied';
			return result.granted;
		} catch (error) {
			addLog(`Permission request failed: ${error}`, 'error');
			permissionStatus = 'error';
			return false;
		} finally {
			isLoading = false;
		}
	}

	async function autoInitializePermissions() {
		addLog('=== Auto-initializing audio permissions ===', 'info');
		
		// First, check current status
		const hasPermission = await performPermissionCheck();
		
		if (!hasPermission && permissionStatus !== 'error') {
			addLog('Permission not granted, automatically requesting...', 'info');
			await performPermissionRequest();
		} else if (hasPermission) {
			addLog('Permission already granted!', 'success');
		}
		
		autoCheckComplete = true;
		addLog('=== Auto-initialization complete ===', 'info');
	}

	// Manual check button handler
	async function manualCheckPermission() {
		await performPermissionCheck();
	}

	// Manual request button handler
	async function manualRequestPermission() {
		await performPermissionRequest();
	}

	// Auto-initialize on component mount
	onMount(() => {
		autoInitializePermissions();
	});
</script>

<main class="container">
  <h1>Audio Permissions Plugin Demo</h1>

  <div class="row">
    <a href="https://vite.dev" target="_blank">
      <img src="/vite.svg" class="logo vite" alt="Vite Logo" />
    </a>
    <a href="https://tauri.app" target="_blank">
      <img src="/tauri.svg" class="logo tauri" alt="Tauri Logo" />
    </a>
    <a href="https://svelte.dev" target="_blank">
      <img src="/svelte.svg" class="logo svelte" alt="Svelte Logo" />
    </a>
  </div>

  <div class="status-section">
    <h2>Audio Permission Status</h2>
    <div class="status-indicator {statusClass}">
      {statusText}
      {#if isLoading}
        <span class="spinner">🔄</span>
      {/if}
    </div>
    
    {#if autoCheckComplete}
      <p class="status-description">
        {#if permissionStatus === 'granted'}
          Your app has permission to access the microphone for audio recording.
        {:else if permissionStatus === 'denied'}
          Audio recording permission was denied. Some features may not work.
        {:else if permissionStatus === 'error'}
          An error occurred while checking permissions. Check the logs below.
        {/if}
      </p>
    {:else}
      <p class="status-description">Initializing permissions...</p>
    {/if}
  </div>

  <div class="controls-section">
    <h3>Manual Controls</h3>
    <button 
      onclick="{manualCheckPermission}" 
      disabled={isLoading}
      class="btn btn-secondary"
    >
      {isLoading ? 'Checking...' : 'Check Permission'}
    </button>
    <button 
      onclick="{manualRequestPermission}" 
      disabled={isLoading}
      class="btn btn-primary"
    >
      {isLoading ? 'Requesting...' : 'Request Permission'}
    </button>
  </div>

  <div class="logs-section">
    <h3>Technical Logs</h3>
    <div class="logs-container">
      {#each logs as log}
        <div class="log-entry log-{log.type}">
          <span class="log-timestamp">[{log.timestamp}]</span>
          <span class="log-message">{log.message}</span>
        </div>
      {/each}
      {#if logs.length === 0}
        <div class="log-entry log-info">
          <span class="log-message">No logs yet...</span>
        </div>
      {/if}
    </div>
  </div>

</main>

<style>
  .logo.vite:hover {
    filter: drop-shadow(0 0 2em #747bff);
  }

  .logo.svelte:hover {
    filter: drop-shadow(0 0 2em #ff3e00);
  }

  .status-section {
    margin: 30px 0;
    padding: 20px;
    border: 2px solid #e0e0e0;
    border-radius: 10px;
    background-color: #f9f9f9;
  }

  .status-indicator {
    font-size: 24px;
    font-weight: bold;
    padding: 15px;
    border-radius: 8px;
    text-align: center;
    margin: 10px 0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
  }

  .status-unknown {
    background-color: #f0f0f0;
    color: #666;
    border: 2px solid #ccc;
  }

  .status-granted {
    background-color: #d4edda;
    color: #155724;
    border: 2px solid #c3e6cb;
  }

  .status-denied {
    background-color: #f8d7da;
    color: #721c24;
    border: 2px solid #f5c6cb;
  }

  .status-error {
    background-color: #fff3cd;
    color: #856404;
    border: 2px solid #ffeaa7;
  }

  .spinner {
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }

  .status-description {
    text-align: center;
    color: #666;
    font-style: italic;
    margin-top: 10px;
  }

  .controls-section {
    margin: 20px 0;
  }

  .controls-section h3 {
    margin-bottom: 15px;
  }

  .btn {
    padding: 10px 20px;
    margin: 5px;
    border: none;
    border-radius: 5px;
    cursor: pointer;
    font-size: 14px;
    transition: background-color 0.2s;
  }

  .btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-primary {
    background-color: #007bff;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background-color: #0056b3;
  }

  .btn-secondary {
    background-color: #6c757d;
    color: white;
  }

  .btn-secondary:hover:not(:disabled) {
    background-color: #545b62;
  }

  .logs-section {
    margin-top: 30px;
  }

  .logs-section h3 {
    margin-bottom: 15px;
  }

  .logs-container {
    background-color: #1e1e1e;
    color: #f0f0f0;
    padding: 15px;
    border-radius: 8px;
    font-family: 'Courier New', monospace;
    font-size: 12px;
    max-height: 300px;
    overflow-y: auto;
    border: 1px solid #444;
  }

  .log-entry {
    margin: 2px 0;
    display: flex;
    gap: 10px;
  }

  .log-timestamp {
    color: #888;
    min-width: 80px;
  }

  .log-message {
    flex: 1;
  }

  .log-info .log-message {
    color: #f0f0f0;
  }

  .log-success .log-message {
    color: #4caf50;
  }

  .log-error .log-message {
    color: #f44336;
  }

  .container {
    max-width: 800px;
    margin: 0 auto;
    padding: 20px;
  }

  .row {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 20px;
    margin: 20px 0;
  }
</style>
