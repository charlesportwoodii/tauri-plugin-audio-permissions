'use strict';

var core = require('@tauri-apps/api/core');

async function requestAudioPermissions() {
    return await core.invoke('plugin:audio-permissions|request_audio_permissions', {
        payload: {},
    });
}
async function checkAudioPermissions() {
    return await core.invoke('plugin:audio-permissions|check_audio_permissions', {
        payload: {},
    });
}

exports.checkAudioPermissions = checkAudioPermissions;
exports.requestAudioPermissions = requestAudioPermissions;
