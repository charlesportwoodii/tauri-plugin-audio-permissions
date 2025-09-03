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
  let handle = api.register_android_plugin("com.alaydriem.bvc.plugin.audio_permissions", "AudioPermissionPlugin")?;
  #[cfg(target_os = "ios")]
  let handle = api.register_ios_plugin(init_plugin_audio_permissions)?;
  Ok(AudioPermissions(handle))
}

/// Access to the audio-permissions APIs.
pub struct AudioPermissions<R: Runtime>(PluginHandle<R>);

impl<R: Runtime> AudioPermissions<R> {
  pub fn request_permission(&self, payload: PermissionRequest) -> crate::Result<PermissionResponse> {
    self
      .0
      .run_mobile_plugin("requestPermission", payload)
      .map_err(Into::into)
  }

  pub fn check_permission(&self, payload: PermissionRequest) -> crate::Result<PermissionResponse> {
    self
      .0
      .run_mobile_plugin("checkPermission", payload)
      .map_err(Into::into)
  }
}
