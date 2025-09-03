// Learn more about Tauri commands at https://v2.tauri.app/develop/calling-rust/#commands
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(
            tauri_plugin_log::Builder::new()
                .level(log::LevelFilter::Info)
                .build(),
        )
        .plugin(tauri_plugin_audio_permissions::init())
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
