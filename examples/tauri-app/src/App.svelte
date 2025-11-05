<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import {
    requestPermission,
    checkPermission,
    startForegroundService,
    stopForegroundService,
    updateNotification,
    isServiceRunning
  } from 'tauri-plugin-audio-permissions'
  import { info, error } from '@tauri-apps/plugin-log'

	let permissionStatus = $state('unknown');
	let isLoading = $state(false);
	let logs = $state([]);
	let autoCheckComplete = $state(false);
	let isRecording = $state(false);
	let elapsedSeconds = $state(0);
	let timerInterval = $state(null);

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

	// Recording functions
	async function startRecording() {
		isLoading = true;
		addLog('=== Starting recording session ===', 'info');

		try {
			// Check permission first
			const permission = await checkPermission();
			if (!permission.granted) {
				addLog('Permission not granted, requesting...', 'info');
				const result = await requestPermission();
				if (!result.granted) {
					addLog('Permission denied, cannot start recording', 'error');
					permissionStatus = 'denied';
					return;
				}
				permissionStatus = 'granted';
			}

			// Start foreground service
			addLog('Starting foreground service...', 'info');
			const serviceResult = await startForegroundService();

			if (serviceResult.started) {
				addLog('Foreground service started successfully', 'success');
				isRecording = true;
				elapsedSeconds = 0;
				startTimer();

				// In real app: start actual audio recording here
				addLog('🎤 Recording started (simulated)', 'success');
			} else {
				addLog('Failed to start foreground service', 'error');
			}
		} catch (err) {
			addLog(`Error starting recording: ${err}`, 'error');
			error(`Failed to start recording: ${err}`);
		} finally {
			isLoading = false;
		}
	}

	async function stopRecording() {
		isLoading = true;
		addLog('=== Stopping recording session ===', 'info');

		try {
			stopTimer();

			// In real app: stop actual audio recording here
			addLog('🎤 Stopping audio recording...', 'info');

			// Stop foreground service
			addLog('Stopping foreground service...', 'info');
			const result = await stopForegroundService();

			if (result.stopped) {
				addLog('Foreground service stopped successfully', 'success');
				isRecording = false;
				elapsedSeconds = 0;
				addLog('=== Recording session ended ===', 'success');
			} else {
				addLog('Failed to stop foreground service', 'error');
			}
		} catch (err) {
			addLog(`Error stopping recording: ${err}`, 'error');
			error(`Failed to stop recording: ${err}`);
		} finally {
			isLoading = false;
		}
	}

	async function checkServiceStatus() {
		try {
			const status = await isServiceRunning();
			addLog(`Service status check: ${status.running ? 'Running' : 'Not running'}`, 'info');
			return status.running;
		} catch (err) {
			addLog(`Error checking service status: ${err}`, 'error');
			return false;
		}
	}

	function startTimer() {
		timerInterval = setInterval(async () => {
			elapsedSeconds++;

			// Update notification every second (Android only)
			try {
				await updateNotification({
					title: 'Recording Audio',
					message: `Recording time: ${formatTime(elapsedSeconds)}`
				});
			} catch (err) {
				// Silently fail - not critical
			}
		}, 1000);
	}

	function stopTimer() {
		if (timerInterval) {
			clearInterval(timerInterval);
			timerInterval = null;
		}
	}

	function formatTime(seconds) {
		const mins = Math.floor(seconds / 60);
		const secs = seconds % 60;
		return `${mins}:${secs.toString().padStart(2, '0')}`;
	}

	// Auto-initialize on component mount
	onMount(() => {
		autoInitializePermissions();
	});

	// Cleanup on unmount
	onDestroy(() => {
		stopTimer();
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

  <div class="recording-section">
    <h2>Background Recording Demo</h2>
    {#if isRecording}
      <div class="recording-indicator">
        <span class="recording-dot"></span>
        Recording: {formatTime(elapsedSeconds)}
      </div>
    {:else}
      <div class="recording-indicator stopped">
        Not Recording
      </div>
    {/if}

    <div class="recording-controls">
      <button
        onclick="{startRecording}"
        disabled={isLoading || isRecording}
        class="btn btn-success"
      >
        {#if isLoading}
          Starting...
        {:else}
          🎤 Start Recording
        {/if}
      </button>
      <button
        onclick="{stopRecording}"
        disabled={isLoading || !isRecording}
        class="btn btn-danger"
      >
        {#if isLoading}
          Stopping...
        {:else}
          ⏹️ Stop Recording
        {/if}
      </button>
      <button
        onclick="{checkServiceStatus}"
        disabled={isLoading}
        class="btn btn-secondary"
      >
        Check Service Status
      </button>
    </div>

    <p class="help-text">
      This demonstrates the foreground service API. On mobile, backgrounding the app
      will keep the recording active. On Android, you'll see a persistent notification.
    </p>
  </div>

  <div class="controls-section">
    <h3>Permission Controls</h3>
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

  .recording-section {
    margin: 30px 0;
    padding: 20px;
    border: 2px solid #e0e0e0;
    border-radius: 10px;
    background-color: #f9f9f9;
  }

  .recording-indicator {
    font-size: 20px;
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

  .recording-indicator {
    background-color: #d4edda;
    color: #155724;
    border: 2px solid #c3e6cb;
  }

  .recording-indicator.stopped {
    background-color: #f0f0f0;
    color: #666;
    border: 2px solid #ccc;
  }

  .recording-dot {
    width: 12px;
    height: 12px;
    background-color: #dc3545;
    border-radius: 50%;
    animation: pulse 1.5s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }

  .recording-controls {
    display: flex;
    gap: 10px;
    justify-content: center;
    flex-wrap: wrap;
    margin: 15px 0;
  }

  .help-text {
    text-align: center;
    color: #666;
    font-size: 14px;
    font-style: italic;
    margin-top: 15px;
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

  .btn-success {
    background-color: #28a745;
    color: white;
  }

  .btn-success:hover:not(:disabled) {
    background-color: #218838;
  }

  .btn-danger {
    background-color: #dc3545;
    color: white;
  }

  .btn-danger:hover:not(:disabled) {
    background-color: #c82333;
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
