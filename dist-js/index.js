import { invoke } from '@tauri-apps/api/core';

async function requestPermission() {
    return await invoke('plugin:audio-permissions|request_permission', {
        payload: {},
    });
}
async function checkPermission() {
    return await invoke('plugin:audio-permissions|check_permission', {
        payload: {},
    });
}
async function startForegroundService() {
    return await invoke('plugin:audio-permissions|start_foreground_service');
}
async function stopForegroundService() {
    return await invoke('plugin:audio-permissions|stop_foreground_service');
}
async function updateNotification(update) {
    return await invoke('plugin:audio-permissions|update_notification', {
        payload: update,
    });
}
async function isServiceRunning() {
    return await invoke('plugin:audio-permissions|is_service_running');
}

export { checkPermission, isServiceRunning, requestPermission, startForegroundService, stopForegroundService, updateNotification };
