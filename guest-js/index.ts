import { invoke } from '@tauri-apps/api/core'

export const PermissionType = {
  Audio: 'audio',
  Notification: 'notification',
} as const;

export type PermissionType = typeof PermissionType[keyof typeof PermissionType];

export interface PermissionRequest {
  permissionType?: PermissionType;
}

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

/**
 * Wraps a promise with a timeout to prevent hanging UI
 * @param promise The promise to wrap
 * @param timeoutMs Timeout in milliseconds (default: 30000ms = 30s)
 * @returns The promise result or throws a timeout error
 */
async function withTimeout<T>(promise: Promise<T>, timeoutMs: number = 30000): Promise<T> {
  let timeoutHandle: ReturnType<typeof setTimeout> | undefined;

  const timeoutPromise = new Promise<never>((_, reject) => {
    timeoutHandle = setTimeout(() => {
      reject(new Error(`Operation timed out after ${timeoutMs}ms`));
    }, timeoutMs);
  });

  try {
    return await Promise.race([promise, timeoutPromise]);
  } finally {
    if (timeoutHandle !== undefined) {
      clearTimeout(timeoutHandle);
    }
  }
}

export async function requestPermission(request?: PermissionRequest): Promise<PermissionResponse> {
  try {
    return await withTimeout(
      invoke<PermissionResponse>('plugin:audio-permissions|request_permission', {
        payload: request || { permissionType: PermissionType.Audio },
      })
    );
  } catch (error) {
    // If user denies or times out, return denied instead of throwing
    if (error instanceof Error && error.message.includes('timed out')) {
      return { granted: false };
    }
    throw error;
  }
}

export async function checkPermission(request?: PermissionRequest): Promise<PermissionResponse> {
  try {
    return await withTimeout(
      invoke<PermissionResponse>('plugin:audio-permissions|check_permission', {
        payload: request || { permissionType: PermissionType.Audio },
      }),
      10000 // Shorter timeout for checks (10s)
    );
  } catch (error) {
    // If check times out, return denied instead of throwing
    if (error instanceof Error && error.message.includes('timed out')) {
      return { granted: false };
    }
    throw error;
  }
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
