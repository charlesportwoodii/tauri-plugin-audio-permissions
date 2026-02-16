export declare const PermissionType: {
    readonly Audio: "audio";
    readonly Notification: "notification";
};
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
export interface MicrophoneAvailabilityResponse {
    available: boolean;
    toggleSupported: boolean;
}
export interface PermissionChangeEvent {
    permissionType: string;
    granted: boolean;
}
export interface StartForegroundServiceOptions {
    onPermissionRevoked?: (event: PermissionChangeEvent) => void;
}
export declare function requestPermission(request?: PermissionRequest): Promise<PermissionResponse>;
export declare function checkPermission(request?: PermissionRequest): Promise<PermissionResponse>;
export declare function startForegroundService(options?: StartForegroundServiceOptions): Promise<ServiceResponse>;
export declare function stopForegroundService(): Promise<ServiceResponse>;
export declare function updateNotification(update: NotificationUpdate): Promise<ServiceResponse>;
export declare function isServiceRunning(): Promise<ServiceStatusResponse>;
export declare function isMicrophoneAvailable(): Promise<MicrophoneAvailabilityResponse>;
