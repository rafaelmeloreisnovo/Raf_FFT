#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT HUP INT TERM
# Compiles only platform-independent logic. No Android SDK, provider, credentials or network.
javac -encoding UTF-8 -d "$OUT" \
  app/src/main/java/org/rafaelia/rafffft/PolicyGate.java \
  app/src/main/java/org/rafaelia/rafffft/EventJournal.java \
  app/src/main/java/org/rafaelia/rafffft/FftQ16.java \
  app/src/main/java/org/rafaelia/rafffft/CapabilityEnvelope.java \
  app/src/main/java/org/rafaelia/rafffft/StrictEnvelopeJson.java \
  tests/ContractTests.java tests/CapabilityS01Tests.java
java -cp "$OUT" ContractTests
java -cp "$OUT" CapabilityS01Tests
printf '%s\n' 'ANDROID_APK_BUILD=NOT_RUN (requires Android SDK + Gradle)'
