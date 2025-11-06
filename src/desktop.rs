use serde::de::DeserializeOwned;
use tauri::{plugin::PluginApi, AppHandle, Runtime};

use crate::models::*;


#[cfg(target_os = "macos")]
use objc::runtime::{Object, Class};
#[cfg(target_os = "macos")]
use objc::{msg_send, sel, sel_impl};
#[cfg(target_os = "macos")]
use std::ffi::CStr;

pub fn init<R: Runtime, C: DeserializeOwned>(
  app: &AppHandle<R>,
  _api: PluginApi<R, C>,
) -> crate::Result<AudioPermissions<R>> {
  Ok(AudioPermissions(app.clone()))
}

/// Access to the audio-permissions APIs.
///
/// # Async Implementation Notes
///
/// ## MacOS
/// TODO: The macOS implementation currently passes a null completion handler to
/// requestAccessForMediaType, which doesn't properly await the user's response.
/// This should be updated to use a proper completion handler and resolve the promise
/// when the user responds to the permission dialog, similar to the Android implementation.
///
/// ## Windows/Linux
/// Desktop platforms typically don't require explicit permission requests for microphone access.
pub struct AudioPermissions<R: Runtime>(AppHandle<R>);

impl<R: Runtime> AudioPermissions<R> {
  pub async fn request_permission(&self, payload: PermissionRequest) -> crate::Result<PermissionResponse> {
    match payload.permission_type {
      crate::models::PermissionType::Audio => {
        let granted = request_microphone_permission()?;
        Ok(PermissionResponse { granted })
      }
      crate::models::PermissionType::Notification => {
        // Desktop doesn't need notification permissions
        Ok(PermissionResponse { granted: true })
      }
    }
  }

  pub fn check_permission(&self, payload: PermissionRequest) -> crate::Result<PermissionResponse> {
    match payload.permission_type {
      crate::models::PermissionType::Audio => {
        let granted = check_microphone_permission()?;
        Ok(PermissionResponse { granted })
      }
      crate::models::PermissionType::Notification => {
        // Desktop doesn't need notification permissions
        Ok(PermissionResponse { granted: true })
      }
    }
  }

  // Desktop doesn't need foreground services - these are no-ops
  pub fn start_foreground_service(&self) -> crate::Result<ServiceResponse> {
    Ok(ServiceResponse {
      started: Some(true),
      stopped: None,
      updated: None,
    })
  }

  pub fn stop_foreground_service(&self) -> crate::Result<ServiceResponse> {
    Ok(ServiceResponse {
      started: None,
      stopped: Some(true),
      updated: None,
    })
  }

  pub fn update_notification(&self, _payload: NotificationUpdate) -> crate::Result<ServiceResponse> {
    Ok(ServiceResponse {
      started: None,
      stopped: None,
      updated: Some(true),
    })
  }

  pub fn is_service_running(&self) -> crate::Result<ServiceStatusResponse> {
    Ok(ServiceStatusResponse { running: false })
  }
}

pub fn check_microphone_permission() -> crate::Result<bool> {
    #[cfg(target_os = "macos")]
    {
        unsafe {
            let av_capture_device_class = Class::get("AVCaptureDevice").ok_or_else(|| {
                crate::Error::Platform("AVCaptureDevice class not found".to_string())
            })?;

            // Get the authorization status for audio
            let audio_media_type: *mut Object = msg_send![av_capture_device_class, mediaTypeAudio];
            let auth_status: i32 = msg_send![av_capture_device_class, authorizationStatusForMediaType: audio_media_type];

            // AVAuthorizationStatus values:
            // AVAuthorizationStatusNotDetermined = 0
            // AVAuthorizationStatusRestricted = 1
            // AVAuthorizationStatusDenied = 2
            // AVAuthorizationStatusAuthorized = 3
            Ok(auth_status == 3) // AVAuthorizationStatusAuthorized
        }
    }

    #[cfg(not(target_os = "macos"))]
    Ok(true)
}

pub fn request_microphone_permission() -> crate::Result<bool> {
    #[cfg(target_os = "macos")]
    {
        unsafe {
            let av_capture_device_class = Class::get("AVCaptureDevice").ok_or_else(|| {
                crate::Error::Platform("AVCaptureDevice class not found".to_string())
            })?;

            let audio_media_type: *mut Object = msg_send![av_capture_device_class, mediaTypeAudio];

            // Request access - this is async but we'll return the current status
            let _: () = msg_send![av_capture_device_class, requestAccessForMediaType: audio_media_type completionHandler: null::<*const Object>()];

            // Return current status (user will need to check again after permission dialog)
            let auth_status: i32 = msg_send![av_capture_device_class, authorizationStatusForMediaType: audio_media_type];
            Ok(auth_status == 3) // AVAuthorizationStatusAuthorized
        }
    }

    #[cfg(not(target_os = "macos"))]
    Ok(true)
}