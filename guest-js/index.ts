import { invoke } from '@tauri-apps/api/core'

export interface PermissionResponse {
  granted: boolean;
}

export interface ServiceResponse {
  started?: boolean;
  stopped?: boolean;
  updated?: boolean;
}

export interface ServiceStatusResponse {
  running: boolean;
}

export interface NotificationUpdate {
  title?: string;
  message?: string;
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

export async function startForegroundService(): Promise<ServiceResponse> {
  return await invoke<ServiceResponse>('plugin:audio-permissions|start_foreground_service');
}

export async function stopForegroundService(): Promise<ServiceResponse> {
  return await invoke<ServiceResponse>('plugin:audio-permissions|stop_foreground_service');
}

export async function updateNotification(update: NotificationUpdate): Promise<ServiceResponse> {
  return await invoke<ServiceResponse>('plugin:audio-permissions|update_notification', {
    payload: update,
  });
}

export async function isServiceRunning(): Promise<ServiceStatusResponse> {
  return await invoke<ServiceStatusResponse>('plugin:audio-permissions|is_service_running');
}
