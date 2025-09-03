import { invoke } from '@tauri-apps/api/core'

export interface PermissionResponse {
  granted: boolean;
}

export async function requestAudioPermissions(): Promise<PermissionResponse> {
  return await invoke<PermissionResponse>('plugin:audio-permissions|request_audio_permissions', {
    payload: {},
  });
}

export async function checkAudioPermissions(): Promise<PermissionResponse> {
  return await invoke<PermissionResponse>('plugin:audio-permissions|check_audio_permissions', {
    payload: {},
  });
}
