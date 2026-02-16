use tauri::{AppHandle, command, Runtime};

use crate::models::*;
use crate::Result;
use crate::AudioPermissionsExt;

#[command]
pub(crate) async fn request_permission<R: Runtime>(
    app: AppHandle<R>,
    payload: PermissionRequest,
) -> Result<PermissionResponse> {
    app.audio_permissions().request_permission(payload).await
}

#[command]
pub(crate) async fn check_permission<R: Runtime>(
    app: AppHandle<R>,
    payload: PermissionRequest,
) -> Result<PermissionResponse> {
    app.audio_permissions().check_permission(payload)
}

#[command]
pub(crate) async fn start_foreground_service<R: Runtime>(
    app: AppHandle<R>,
) -> Result<ServiceResponse> {
    app.audio_permissions().start_foreground_service()
}

#[command]
pub(crate) async fn stop_foreground_service<R: Runtime>(
    app: AppHandle<R>,
) -> Result<ServiceResponse> {
    app.audio_permissions().stop_foreground_service()
}

#[command]
pub(crate) async fn update_notification<R: Runtime>(
    app: AppHandle<R>,
    payload: NotificationUpdate,
) -> Result<ServiceResponse> {
    app.audio_permissions().update_notification(payload)
}

#[command]
pub(crate) async fn is_service_running<R: Runtime>(
    app: AppHandle<R>,
) -> Result<ServiceStatusResponse> {
    app.audio_permissions().is_service_running()
}

#[command]
pub(crate) async fn is_microphone_available<R: Runtime>(
    app: AppHandle<R>,
) -> Result<MicrophoneAvailabilityResponse> {
    app.audio_permissions().is_microphone_available()
}
