export interface PermissionResponse {
    granted: boolean;
}
export declare function requestAudioPermissions(): Promise<PermissionResponse>;
export declare function checkAudioPermissions(): Promise<PermissionResponse>;
