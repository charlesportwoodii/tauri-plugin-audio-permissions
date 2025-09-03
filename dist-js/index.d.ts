export interface PermissionResponse {
    granted: boolean;
}
export declare function requestPermission(): Promise<PermissionResponse>;
export declare function checkPermission(): Promise<PermissionResponse>;
