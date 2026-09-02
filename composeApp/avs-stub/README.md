# avs stub (iOS)

A **stub** replacement for Wire's proprietary `avs` (Audio Video Signaling) framework, so this demo
links and runs on iOS **without calling**.

See the top-level [README.md](../../README.md) → **"iOS: linking AVS"** for why this is needed. This
folder is the concrete implementation.

## Layout

- `src/avs_stub.m` — the stub sources (no-op `wcall_*` functions + empty `AVSFlowManager`/
  `AVSMediaManager` classes), generated from the linker's undefined-symbol report.
- `build-avs-stub.sh` — builds `ios-arm64/` and `ios-arm64-simulator/` `avs.framework` (static).
- `ios-arm64/…`, `ios-arm64-simulator/…` — the built stub frameworks (committed; ~6 KB each).

`composeApp/build.gradle.kts` points each iOS target's framework at the matching slice via
`linkerOpts("-F", …)`. Because the app framework is dynamic, the stub's symbols get linked into it —
no separate `avs.framework` is needed at runtime.

## Regenerating

If a Kalium/avs bump changes the referenced symbols, re-link an iOS app against an empty stub,
collect the `Undefined symbols` list, regenerate `src/avs_stub.m`, then run `./build-avs-stub.sh`.
