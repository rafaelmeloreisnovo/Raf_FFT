# Capability handoff S0.1 — Raf_FFT Android UI × RAFAELIA Capability S0

State: `LOCAL_CANDIDATE`, `claim_allowed=false`, `PROVIDER_AUTH_NOT_CHECKED`.
Producer: `rafaelmeloreisnovo/Raf_FFT` (public); Mapa governs federation routes, not this producer's code.
Predecessors: `Raf_FFT` S0 local patch and `RAFAELIA_Omega_Capability_Contract_S0_20261122.zip`.

## UI contract

- A human may copy a JSON request into the APK. The explicit **Validate (NO execute)** button runs pure Java `StrictEnvelopeJson` + `CapabilityEnvelope`; the result is a preview only. Pasted text is not logged, networked, or executed.
- Execution remains behind a distinct manual button and a separate `PolicyGate`.
- The app's sole network route remains one public GitHub README metadata GET, bounded at 16 KiB, no authorization header. The contract's public-GitHub shape is a *preview*, not permission to use GitHub credentials.
- The SAF tree picker does not read the selected folder and does **not** request persistence. Its temporary grant must not be treated as Google OAuth or proof of account identity. An explicit revocation button handles residual permissions from older builds.
- `GPT_UI` is an untrusted origin string; it does not authenticate ChatGPT or the person. No hidden GPT↔APK IPC exists.
- Fail closed on malformed/duplicate/unknown JSON keys, invalid types, private content, ambiguous target, missing human presence, write requests, non-dry-run requests, or absent journal.
- Android permission set: INTERNET only. `contracts/capability-request.s0.schema.json` is a source-level projection of the offline S0 input contract; the runtime validates with closed, pure-Java rules and does not infer external authority from the JSON Schema. No credential ingestion, filesystem wide permission, location, audio, background activity, overlays, accessibility service or secret.
- Do not paste secrets into a software keyboard. Sandbox logs minimize metadata, but keyboard/clipboard providers have independent privacy boundaries.

## Provenance & tests

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`

S0 preview does not attest that a commit exists or that the Google Drive account/folder is accessible.
`claim_allowed=false` for federation, privacy guarantee, device functionality and scientific conclusions.
A local hash chain does not provide hardware-backed tamper proofing or independent timestamping.

Acceptance for remote candidate: independent review, latest `AGENTS.md` / `START HERE` / Mapa gate, source HEAD, private-data scan, workflow review, branch-protected provider checks, APK hash, and physical split-screen E2E. No auto-merge, release, or private sync.

Rollback: local `git apply -R` of the combined patch, preserving any separate source and historical evidence; no external provider state changed by this candidate.
