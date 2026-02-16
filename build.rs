const COMMANDS: &[&str] = &[
  "request_permission",
  "check_permission",
  "start_foreground_service",
  "stop_foreground_service",
  "update_notification",
  "is_service_running",
  "is_microphone_available",
];

fn main() {
  tauri_plugin::Builder::new(COMMANDS)
    .android_path("android")
    .ios_path("ios")
    .build();
}
