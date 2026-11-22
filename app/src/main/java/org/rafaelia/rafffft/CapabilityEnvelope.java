package org.rafaelia.rafffft;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * S0 offline policy validator. A matching envelope does NOT authorize a provider operation.
 * It is deliberately pure Java; never accepts credentials, private source IDs or raw content.
 */
public final class CapabilityEnvelope {
    private static final Set<String> FIELDS = new HashSet<>(Arrays.asList(
        "schema", "request_id", "origin", "mode", "operation", "subject_present",
        "consent", "target", "privacy_class", "write_requested", "dry_run",
        "source_ref", "claim_allowed"));

    public static final class Result {
        public final String state;
        public final String reason;
        public final String digest;
        public final boolean claimAllowed = false;
        private Result(String state, String reason, String digest) {
            this.state = state; this.reason = reason; this.digest = digest;
        }
    }

    private CapabilityEnvelope() {}

    private static Result block(String code) { return new Result("BLOCKED", code, ""); }

    private static String string(Map<String, ?> m, String key) {
        Object v = m.get(key);
        return v instanceof String ? (String)v : null;
    }

    private static boolean eq(String a, String b) { return b.equals(a); }

    private static String hex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            out[i*2] = digits[(bytes[i] & 255) >>> 4];
            out[i*2+1] = digits[bytes[i] & 15];
        }
        return new String(out);
    }

    /** JSON serialization of a closed, validated map. Values can only be strings or booleans. */
    private static String canonical(Map<String, ?> input) {
        StringBuilder s = new StringBuilder("{");
        for (Map.Entry<String, ?> e : new TreeMap<>(input).entrySet()) {
            if (s.length() > 1) s.append(',');
            s.append('"').append(e.getKey()).append("\":");
            Object value = e.getValue();
            if (value instanceof Boolean) {
                s.append(value);
            } else {
                s.append('"');
                String v = (String)value;
                for (int i = 0; i < v.length(); i++) {
                    char c = v.charAt(i);
                    if (c == '"' || c == '\\') s.append('\\');
                    if (c < 0x20) {
                        String h = Integer.toHexString(c);
                        s.append("\\u");
                        for (int pad=h.length(); pad<4; pad++) s.append('0');
                        s.append(h);
                    } else {
                        s.append(c);
                    }
                }
                s.append('"');
            }
        }
        return s.append('}').toString();
    }

    public static Result validate(Map<String, ?> request) {
        if (request == null) return block("INVALID_ENVELOPE");
        if (!request.keySet().equals(FIELDS)) return block("FIELDS_MISSING_OR_EXTRA");
        if (!eq(string(request,"schema"), "rafaelia.capability-request.s0")) return block("SCHEMA_MISMATCH");
        String id=string(request,"request_id");
        if (id == null || !id.matches("[A-Z0-9][A-Z0-9_-]{7,79}")) return block("REQUEST_ID_INVALID");
        String origin=string(request,"origin");
        if (!eq(origin,"APK_UI") && !eq(origin,"GPT_UI")) return block("ORIGIN_UNKNOWN");
        if (!Boolean.TRUE.equals(request.get("subject_present"))) return block("SUBJECT_NOT_PRESENT");
        if (!eq(string(request,"consent"),"EXPLICIT_LOCAL_UI")) return block("CONSENT_MISSING");
        if (!Boolean.FALSE.equals(request.get("write_requested"))) return block("WRITE_DENIED");
        if (!Boolean.TRUE.equals(request.get("dry_run"))) return block("NOT_DRY_RUN");
        if (!Boolean.FALSE.equals(request.get("claim_allowed"))) return block("CLAIM_FORBIDDEN");
        String mode=string(request,"mode");
        String op=string(request,"operation");
        String privacy=string(request,"privacy_class");
        String target=string(request,"target");
        String source=string(request,"source_ref");
        if (target==null || target.isEmpty() || target.length()>220) return block("TARGET_INVALID");
        if (source==null || source.isEmpty() || source.length()>220) return block("SOURCE_INVALID");
        if (eq(mode,"LOCAL_SYNTHETIC")) {
            if (!eq(op,"FFT_Q16_TEST") || !eq(privacy,"SYNTHETIC")) return block("OPERATION_PRIVACY_MISMATCH");
            if (!eq(target,"fixture:fft8-q16") || !eq(source,"synthetic:fft8-q16")) return block("SYNTHETIC_TARGET_UNBOUND");
        } else if (eq(mode,"GITHUB_PUBLIC_METADATA")) {
            if (!eq(op,"FETCH_PUBLIC_METADATA_PREVIEW") || !eq(privacy,"PUBLIC")) return block("OPERATION_PRIVACY_MISMATCH");
            if (!eq(target,"github:rafaelmeloreisnovo/Raf_FFT") ||
                !source.matches("commit:[0-9a-f]{40}")) return block("GITHUB_SCOPE_OR_COMMIT_UNBOUND");
        } else if (eq(mode,"DRIVE_SAF_LOCAL_PREVIEW")) {
            if (!eq(op,"LIST_SELECTED_URI_PREVIEW") || !eq(privacy,"SYNTHETIC")) return block("OPERATION_PRIVACY_MISMATCH");
            if (!eq(target,"android:saf:user-chosen-synthetic") ||
                !eq(source,"synthetic:saf-selection")) return block("DRIVE_SCOPE_UNBOUND");
        } else return block("MODE_NOT_ALLOWED");
        // The last step hashes the constrained envelope; this is NOT proof of consent.
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256")
                .digest(canonical(request).getBytes(StandardCharsets.UTF_8));
            return new Result("PREFLIGHT_SYNTHETIC_ONLY","PROVIDER_AUTH_NOT_CHECKED",hex(digest));
        } catch (NoSuchAlgorithmException ex) {
            return block("SHA256_UNAVAILABLE");
        }
    }
}
