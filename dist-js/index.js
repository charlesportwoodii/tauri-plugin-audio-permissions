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

export { checkPermission, requestPermission };
