use tauri::{
  plugin::{Builder, TauriPlugin},
  Manager, Runtime,
};

pub use models::*;

#[cfg(desktop)]
mod desktop;
#[cfg(mobile)]
mod mobile;
#[cfg(target_os = "macos")]
mod macos_permissions;

mod commands;
mod error;
mod models;

pub use error::{Error, Result};

#[cfg(desktop)]
use desktop::AudioPermissions;
#[cfg(mobile)]
use mobile::AudioPermissions;

/// Extensions to [`tauri::App`], [`tauri::AppHandle`] and [`tauri::Window`] to access the audio-permissions APIs.
pub trait AudioPermissionsExt<R: Runtime> {
  fn audio_permissions(&self) -> &AudioPermissions<R>;
}

impl<R: Runtime, T: Manager<R>> crate::AudioPermissionsExt<R> for T {
  fn audio_permissions(&self) -> &AudioPermissions<R> {
    self.state::<AudioPermissions<R>>().inner()
  }
}

/// Initializes the plugin.
pub fn init<R: Runtime>() -> TauriPlugin<R> {
  Builder::new("audio-permissions")
    .invoke_handler(tauri::generate_handler![commands::request_permission, commands::check_permission])
    .setup(|app, api| {
      #[cfg(mobile)]
      let audio_permissions = mobile::init(app, api)?;
      #[cfg(desktop)]
      let audio_permissions = desktop::init(app, api)?;
      app.manage(audio_permissions);
      Ok(())
    })
    .build()
}
