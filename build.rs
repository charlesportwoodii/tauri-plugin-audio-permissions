const COMMANDS: &[&str] = &["request_permission", "check_permission"];

fn main() {
  tauri_plugin::Builder::new(COMMANDS)
    .android_path("android")
    .ios_path("ios")
    .build();
}
