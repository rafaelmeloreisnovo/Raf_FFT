# RAFAELIA Raf_FFT — S0 sandbox Android split-screen

**Status**: LOCAL_PATCH_ONLY · SOURCE_IMPLEMENTED_UNTESTED_ON_ANDROID · PRIVATE_ACCESS_BLOCKED · `claim_allowed=false`.

## Authority & exact producer

- Producer: public `rafaelmeloreisnovo/Raf_FFT`; baseline initially `README.md` only (nine bytes).
- This is NOT a patch to RLL, EstudioAudio, private corpus producer, Mapa or RafGitTools.
- Private GitHub candidate: private corpus producer (name intentionally omitted from the public pilot). No token or contents bundled.
- Drive official candidate: `private corpus producer` private folder — provider account and folder IDs never hardcoded into public APK. User confirmation and independently verifiable least-privilege authorization are missing.
- App does not use credentials, Device Admin, accessibility/overlay services, background services or remote analytics.

## Two-window interaction

```
Android display, portrait
+-----------------------------------------+
| ChatGPT (OS-managed independent app)    |
| requests, review, paste sanitized       |
| receipts; no hidden IPC or auto-send     |
+-----------------------------------------+
| RAFAELIA FFT · S0                       |
| source = PUBLIC / SANDBOX               |
| [Offline fixture] [Read GitHub README]  |
| [Choose Android SAF folder — no read]   |
| [Private: DENY] [Drive: DENY]            |
| [Revoke SAF] [Copy sanitized receipt]    |
| Journal count + tip + typed gaps         |
+-----------------------------------------+
```

On Android 10, set `android:resizeableActivity="true"`; validate actual split-screen compatibility of **both** ChatGPT and Raf_FFT on the target device. OS manages the app arrangement. The Raf_FFT app cannot place ChatGPT above itself or force an unsupported app into multi-window. Split-screen is visual adjacency, not shared sandbox or authority.

## Least privilege

| Transition | S0 | Bound |
|---|---|---|
| Synthetic fixture | allow after tap | radix-2 FFT Q16 N=8 impulse and zero/constant control vectors; no external data |
| Public GitHub | allow after tap | GET `api.github.com/repos/rafaelmeloreisnovo/Raf_FFT/contents/README.md` ONLY, TLS, exact path, max 16 KiB, timeouts |
| Google Drive | allow Android SAF chooser | user-picked content URI only; no file read; account identity `TOKEN_VAZIO` |
| Private GitHub corpus | DENY + journal | no OAuth/PAT/secret, no clone, no raw content |
| Drive corpus | DENY + journal | no directory traversal, no content read |
| Receipt to GPT | user copies | only source-independent gate/journal metadata; OS clipboard user action |
| Delete/merge/publish/release | NOT IMPLEMENTED | never performed |

Local durable receipt is an app-private append-only hash chain with `fsync`, verified on app startup. It records canonical event codes, result codes and timestamps, never URI, file names, private contents, access tokens or identities. Every in-app denial is recorded **only while the journal remains available**; on failure, operations stop fail-closed. A bare local hash chain is not externally attested or tamper-proof against privileged actors.

## Future S1 activation gates — NOT IMPLEMENTED

1. Explicitly name the exact GitHub repository/ref/path and Drive provider account/folder; verify permissions via each provider, not by Android SAF alone.
2. Establish a secure user-visible authorization flow. No PAT in APK, embedded OAuth client secret, universal `repo` scope, or direct plaintext sharing of private corpus with a public runner.
3. Use a private trusted connector/broker to return bounded **metadata only**, with authorization per field, purpose, request and expiry. Do not export raw conversation text by default.
4. Negative tests for wrong Google account, wrong Drive folder, wrong repository, expired consent, offline operation, tampered log, bit-flipped selector and changed SHA.
5. Producer SHA + build run + APK byte SHA-256 + device receipt + screen-multitask validation + ACL readback + rollback plan.
6. Human review before any source patch to private repo, publication, team sharing or data migration.

## Build & evidence boundary

- Java/Android framework only; no AndroidX/Kotlin/third-party runtime libraries. Android SDK and Google Android Gradle Plugin are build-time dependencies.
- Local `./scripts/test-local.sh` exercises pure-Java policy and log. It does NOT compile Android UI or produce APK.
- On configured build runner: `gradle :app:assembleDebug` then verify exact APK/manifest/signing/source SHA; no CI workflow automatically enabled by this patch.
- `TEMPORARY_SAF_SELECTION != GOOGLE_ACCOUNT_VERIFIED`; `APP_SANDBOX != CHATGPT_SANDBOX`; `SHA_CHAIN != EXTERNAL_TIMESTAMP`.
