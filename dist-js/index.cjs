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

exports.checkPermission = checkPermission;
exports.requestPermission = requestPermission;
