use serde::de::DeserializeOwned;
use tauri::{plugin::PluginApi, AppHandle, Runtime};

use crate::models::*;

pub fn init<R: Runtime, C: DeserializeOwned>(
  app: &AppHandle<R>,
  _api: PluginApi<R, C>,
) -> crate::Result<AudioPermissions<R>> {
  Ok(AudioPermissions(app.clone()))
}

/// Access to the audio-permissions APIs.
pub struct AudioPermissions<R: Runtime>(AppHandle<R>);

impl<R: Runtime> AudioPermissions<R> {
  pub fn request_permission(&self, _payload: PermissionRequest) -> crate::Result<PermissionResponse> {
    // On desktop, audio permissions are typically granted by default
    Ok(PermissionResponse {
      granted: true,
    })
  }

  pub fn check_permission(&self, _payload: PermissionRequest) -> crate::Result<PermissionResponse> {
    // On desktop, audio permissions are typically granted by default
    Ok(PermissionResponse {
      granted: true,
    })
  }
}
