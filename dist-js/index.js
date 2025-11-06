import { invoke } from '@tauri-apps/api/core';

const PermissionType = {
    Audio: 'audio',
    Notification: 'notification',
};
async function requestPermission(request) {
    return await invoke('plugin:audio-permissions|request_permission', {
        payload: request || { permissionType: PermissionType.Audio },
    });
}
async function checkPermission(request) {
    return await invoke('plugin:audio-permissions|check_permission', {
        payload: request || { permissionType: PermissionType.Audio },
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

export { PermissionType, checkPermission, isServiceRunning, requestPermission, startForegroundService, stopForegroundService, updateNotification };
