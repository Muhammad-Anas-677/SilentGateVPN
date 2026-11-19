# SilentGate VPN

Native Android VPN client (Kotlin, Jetpack Compose, Material 3, AMOLED black + green).
Package: `com.silentgatevpn.app` · Licence: GPL-3.0 · Made by Anas

## Layers (Smart Auto-Fallback)
1. **Obfuscated Cloudflare WARP**: Hiddify sing-box `warp` endpoint (auto free account, endpoint scanner, QUIC-style noise, AmneziaWG / WireGuard / MASQUE, `auto` races them).
2. **Stealth**: VLESS over WebSocket + TLS (your free Cloudflare Worker, `worker/worker.js`) or any custom profile.
3. **Light**: local TLS fragmentation + encrypted DNS (DoH).

All DNS is hijacked and sent over DoH through the active tunnel. IPv6 always enters the TUN (blocked or tunnelled), so no IPv6 leak.

## Build (mobile friendly, GitHub web only)
1. Create a new GitHub repository and upload every file of this project (keep the folder structure, including `.github/workflows/build.yml`).
2. Open **Actions → Build SilentGate VPN → Run workflow**.
3. First run builds `libbox.aar` from `hiddify/hiddify-sing-box` (branch `v5`, submodules) and takes a long time. The result is cached by commit, so later runs are much faster.
4. Download the `SilentGateVPN-apk` artifact and install it.

If a step fails, send the log of the failing step and it can be fixed quickly.

## Notes
- Debug-signed release APK: every CI run uses a fresh debug key, so install over an older build only works if you uninstall first (add your own keystore later to avoid this).
- The Worker script is an untested template. Cloudflare's terms may restrict proxy use.
- Kill switch: the app guides you to Android's Always-on VPN + Block connections without VPN.
- Source must stay public (GPL-3.0) when you share the APK.
