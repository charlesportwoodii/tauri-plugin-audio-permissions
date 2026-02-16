'use strict';

var core = require('@tauri-apps/api/core');

const PermissionType = {
    Audio: 'audio',
    Notification: 'notification',
};
/**
 * Wraps a promise with a timeout to prevent hanging UI
 * @param promise The promise to wrap
 * @param timeoutMs Timeout in milliseconds (default: 30000ms = 30s)
 * @returns The promise result or throws a timeout error
 */
async function withTimeout(promise, timeoutMs = 30000) {
    let timeoutHandle;
    const timeoutPromise = new Promise((_, reject) => {
        timeoutHandle = setTimeout(() => {
            reject(new Error(`Operation timed out after ${timeoutMs}ms`));
        }, timeoutMs);
    });
    try {
        return await Promise.race([promise, timeoutPromise]);
    }
    finally {
        if (timeoutHandle !== undefined) {
            clearTimeout(timeoutHandle);
        }
    }
}
async function requestPermission(request) {
    try {
        return await withTimeout(core.invoke('plugin:audio-permissions|request_permission', {
            payload: request || { permissionType: PermissionType.Audio },
        }));
    }
    catch (error) {
        // If user denies or times out, return denied instead of throwing
        if (error instanceof Error && error.message.includes('timed out')) {
            return { granted: false };
        }
        throw error;
    }
}
async function checkPermission(request) {
    try {
        return await withTimeout(core.invoke('plugin:audio-permissions|check_permission', {
            payload: request || { permissionType: PermissionType.Audio },
        }), 10000 // Shorter timeout for checks (10s)
        );
    }
    catch (error) {
        // If check times out, return denied instead of throwing
        if (error instanceof Error && error.message.includes('timed out')) {
            return { granted: false };
        }
        throw error;
    }
}
async function startForegroundService(options) {
    const channel = new core.Channel();
    if (options?.onPermissionRevoked) {
        channel.onmessage = options.onPermissionRevoked;
    }
    return await core.invoke('plugin:audio-permissions|start_foreground_service', {
        onPermissionRevoked: channel,
    });
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
async function isMicrophoneAvailable() {
    return await core.invoke('plugin:audio-permissions|is_microphone_available');
}

exports.PermissionType = PermissionType;
exports.checkPermission = checkPermission;
exports.isMicrophoneAvailable = isMicrophoneAvailable;
exports.isServiceRunning = isServiceRunning;
exports.requestPermission = requestPermission;
exports.startForegroundService = startForegroundService;
exports.stopForegroundService = stopForegroundService;
exports.updateNotification = updateNotification;
