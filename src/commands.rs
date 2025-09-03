use tauri::{AppHandle, command, Runtime};

use crate::models::*;
use crate::Result;
use crate::AudioPermissionsExt;

#[command]
pub(crate) async fn request_permission<R: Runtime>(
    app: AppHandle<R>,
    payload: PermissionRequest,
) -> Result<PermissionResponse> {
    app.audio_permissions().request_permission(payload)
}

#[command]
pub(crate) async fn check_permission<R: Runtime>(
    app: AppHandle<R>,
    payload: PermissionRequest,
) -> Result<PermissionResponse> {
    app.audio_permissions().check_permission(payload)
}
