use serde::de::DeserializeOwned;
use tauri::{
  plugin::{PluginApi, PluginHandle},
  AppHandle, Runtime,
};

use crate::models::*;

#[cfg(target_os = "ios")]
tauri::ios_plugin_binding!(init_plugin_audio_permissions);

// initializes the Kotlin or Swift plugin classes
pub fn init<R: Runtime, C: DeserializeOwned>(
  _app: &AppHandle<R>,
  api: PluginApi<R, C>,
) -> crate::Result<AudioPermissions<R>> {
  #[cfg(target_os = "android")]
  let handle = api.register_android_plugin("com.charlesportwoodii.tauri.plugin.audio_permissions", "AudioPermissionPlugin")?;
  #[cfg(target_os = "ios")]
  let handle = api.register_ios_plugin(init_plugin_audio_permissions)?;
  Ok(AudioPermissions(handle))
}

/// Access to the audio-permissions APIs.
pub struct AudioPermissions<R: Runtime>(PluginHandle<R>);

impl<R: Runtime> AudioPermissions<R> {
  pub async fn request_permission(&self, payload: PermissionRequest) -> crate::Result<PermissionResponse> {
    match payload.permission_type {
      crate::models::PermissionType::Audio => {
        self
          .0
          .run_mobile_plugin_async("requestPermission", payload)
          .await
          .map_err(Into::into)
      }
      crate::models::PermissionType::Notification => {
        self
          .0
          .run_mobile_plugin_async("requestPermission", payload)
          .await
          .map_err(Into::into)
      }
    }
  }

  pub fn check_permission(&self, payload: PermissionRequest) -> crate::Result<PermissionResponse> {
    // Check permission only applies to audio for now
    // Notification permission checking is done during request
    self
      .0
      .run_mobile_plugin("checkPermission", payload)
      .map_err(Into::into)
  }

  pub fn start_foreground_service(&self) -> crate::Result<ServiceResponse> {
    self
      .0
      .run_mobile_plugin("startForegroundService", ())
      .map_err(Into::into)
  }

  pub fn stop_foreground_service(&self) -> crate::Result<ServiceResponse> {
    self
      .0
      .run_mobile_plugin("stopForegroundService", ())
      .map_err(Into::into)
  }

  pub fn update_notification(&self, payload: NotificationUpdate) -> crate::Result<ServiceResponse> {
    self
      .0
      .run_mobile_plugin("updateNotification", payload)
      .map_err(Into::into)
  }

  pub fn is_service_running(&self) -> crate::Result<ServiceStatusResponse> {
    self
      .0
      .run_mobile_plugin("isServiceRunning", ())
      .map_err(Into::into)
  }
}
