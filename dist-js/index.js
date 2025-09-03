import { invoke } from '@tauri-apps/api/core';

async function requestAudioPermissions() {
    return await invoke('plugin:audio-permissions|request_audio_permissions', {
        payload: {},
    });
}
async function checkAudioPermissions() {
    return await invoke('plugin:audio-permissions|check_audio_permissions', {
        payload: {},
    });
}

export { checkAudioPermissions, requestAudioPermissions };
