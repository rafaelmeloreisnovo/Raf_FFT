# Raf_FFT

## RAFAELIA Ω — Sandbox S0 (candidate Git patch, no remote changes)

Minimal Android 10+ split-screen diagnostic surface (Java + Android platform only):

- public GitHub metadata GET, manually triggered and strictly allowlisted;
- offline FFT radix-2 Q16 (8 points) and synthetic test vectors;
- Drive folder selection through Android SAF, **without reading contents or persisting a grant**;
- private GitHub and Drive content requests **DENIED by design**;
- local append-only event journal for allow/deny with hash chain and fail-closed behavior;
- user-controlled clipboard export of **metadata-only** receipt for pasting into ChatGPT;
- `resizeableActivity=true` for the lower half of Android split screen.

Do not put a token, email, folder ID, user profile or private corpus in this **public** repository/APK. Selecting a SAF folder does not verify its Google account. No background polling, shared sandbox, auto-merge, deletion or publication.

Read [`docs/ARCHITECTURE_S0.md`](docs/ARCHITECTURE_S0.md) for scope, evidence, acceptance gates and rollback.

Tests: `./scripts/test-local.sh`. Android APK build (only when configured and authorized): `gradle :app:assembleDebug` with SDK API 35 and Android Gradle Plugin 8.9.2. No APK or device result is claimed by this repository patch alone.

### Additive S0.1 capability preflight

- JSON request (copy/paste from GPT or local input) is **strictly parsed and checked offline**.
- `PREFLIGHT_SYNTHETIC_ONLY` means the envelope is well-formed and scoped; it does **not** authorize GitHub, Drive, Supabase, or the device.
- No background GPT control, auto test, private corpus access or token handoff.
- A separate tap and `PolicyGate` are mandatory for each permitted synthetic/public action.
- See `docs/CAPABILITY_HANDOFF_S0_1.md` and `docs/CAPABILITY_REQUEST_EXAMPLE.json`.
- Optional Actions workflow is **source only / NOT_RUN** until a deliberately reviewed PR is opened.
