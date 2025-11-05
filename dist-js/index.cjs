'use strict';

var core = require('@tauri-apps/api/core');

async function requestPermission() {
    return await core.invoke('plugin:audio-permissions|request_permission', {
        payload: {},
    });
}
async function checkPermission() {
    return await core.invoke('plugin:audio-permissions|check_permission', {
        payload: {},
    });
}
async function startForegroundService() {
    return await core.invoke('plugin:audio-permissions|start_foreground_service');
}
async function stopForegroundService() {
    return await core.invoke('plugin:audio-permissions|stop_foreground_service');
}
async function updateNotification(update) {
    return await core.invoke('plugin:audio-permissions|update_notification', {
        payload: update,
    });
}
async function isServiceRunning() {
    return await core.invoke('plugin:audio-permissions|is_service_running');
}

exports.checkPermission = checkPermission;
exports.isServiceRunning = isServiceRunning;
exports.requestPermission = requestPermission;
exports.startForegroundService = startForegroundService;
exports.stopForegroundService = stopForegroundService;
exports.updateNotification = updateNotification;
