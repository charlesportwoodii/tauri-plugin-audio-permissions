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
pub struct AudioPermissions<R: Runtime>(AppHandle<R>);

impl<R: Runtime> AudioPermissions<R> {
  pub fn request_permission(&self, _payload: PermissionRequest) -> crate::Result<PermissionResponse> {
    let granted = request_microphone_permission()?;
    Ok(PermissionResponse { granted })
  }

  pub fn check_permission(&self, _payload: PermissionRequest) -> crate::Result<PermissionResponse> {
    let granted = check_microphone_permission()?;
    Ok(PermissionResponse { granted })
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