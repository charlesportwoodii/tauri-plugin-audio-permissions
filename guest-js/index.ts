import { invoke } from '@tauri-apps/api/core'

export interface PermissionResponse {
  granted: boolean;
}

export async function requestPermission(): Promise<PermissionResponse> {
  return await invoke<PermissionResponse>('plugin:audio-permissions|request_permission', {
    payload: {},
  });
}

export async function checkPermission(): Promise<PermissionResponse> {
  return await invoke<PermissionResponse>('plugin:audio-permissions|check_permission', {
    payload: {},
  });
}
