use serde::de::DeserializeOwned;
use tauri::{plugin::PluginApi, AppHandle, Runtime};

use crate::models::*;

#[cfg(target_os = "macos")]
use objc::runtime::{Class, Object, BOOL, YES};
#[cfg(target_os = "macos")]
use objc::{msg_send, sel, sel_impl};
#[cfg(target_os = "macos")]
use block::ConcreteBlock;
#[cfg(target_os = "macos")]
use std::sync::mpsc;

#[cfg(target_os = "macos")]
#[link(name = "AVFoundation", kind = "framework")]
extern "C" {
    static AVMediaTypeAudio: *mut Object;
}

pub fn init<R: Runtime, C: DeserializeOwned>(
  app: &AppHandle<R>,
  _api: PluginApi<R, C>,
) -> crate::Result<AudioPermissions<R>> {
  Ok(AudioPermissions(app.clone()))
}

/// Access to the audio-permissions APIs.
///
/// # Platform Notes
///
/// ## macOS
/// The macOS implementation uses AVFoundation's AVCaptureDevice API to check and request
/// microphone permissions. Permission requests are properly awaited using a completion handler
/// block, so the function will block until the user responds to the permission dialog.
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

            // Use the AVMediaTypeAudio constant from AVFoundation framework
            let auth_status: i32 = msg_send![av_capture_device_class, authorizationStatusForMediaType: AVMediaTypeAudio];

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

            // First check current status - if already determined, return that
            let auth_status: i32 = msg_send![av_capture_device_class, authorizationStatusForMediaType: AVMediaTypeAudio];

            // AVAuthorizationStatusNotDetermined = 0 - need to request
            // AVAuthorizationStatusRestricted = 1 - return false
            // AVAuthorizationStatusDenied = 2 - return false
            // AVAuthorizationStatusAuthorized = 3 - return true
            if auth_status != 0 {
                return Ok(auth_status == 3);
            }

            // Status is undetermined, need to request permission
            // Create a channel to receive the result from the completion handler
            let (tx, rx) = mpsc::channel::<bool>();

            // Create a completion handler block
            let completion_handler = ConcreteBlock::new(move |granted: BOOL| {
                let _ = tx.send(granted == YES);
            });
            let completion_handler = completion_handler.copy();

            // Request access with the completion handler
            let _: () = msg_send![av_capture_device_class, requestAccessForMediaType: AVMediaTypeAudio completionHandler: &*completion_handler];

            // Wait for the user's response (blocking)
            match rx.recv() {
                Ok(granted) => Ok(granted),
                Err(_) => {
                    // Channel closed unexpectedly, check status as fallback
                    let status: i32 = msg_send![av_capture_device_class, authorizationStatusForMediaType: AVMediaTypeAudio];
                    Ok(status == 3)
                }
            }
        }
    }

    #[cfg(not(target_os = "macos"))]
    Ok(true)
}