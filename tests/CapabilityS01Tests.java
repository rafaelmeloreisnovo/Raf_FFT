import java.util.HashMap;
import java.util.Map;
import org.rafaelia.rafffft.CapabilityEnvelope;
import org.rafaelia.rafffft.StrictEnvelopeJson;

/** Pure-Java tests; no Android classes, network, files, tokens or private data. */
public final class CapabilityS01Tests {
    private static int checks;
    private static void ok(boolean value, String id) {
        checks++;
        if (!value) throw new AssertionError("FAIL:"+id);
    }
    private static void deny(Map<String,Object> r, String code) {
        CapabilityEnvelope.Result v=CapabilityEnvelope.validate(r);
        ok(v.state.equals("BLOCKED"),code+":BLOCKED");
        ok(!v.claimAllowed,code+":claim_false");
        ok(v.digest.isEmpty(),code+":no_digest");
    }
    private static void badJson(String value, String code) {
        boolean threw=false;
        try { StrictEnvelopeJson.parse(value); }
        catch (IllegalArgumentException ex) {threw=true;}
        ok(threw,code);
    }
    private static Map<String,Object> base() {
        Map<String,Object> m = new HashMap<>();
        m.put("schema", "rafaelia.capability-request.s0");
        m.put("request_id", "RAFAELIA-S0-001");
        m.put("origin", "APK_UI");
        m.put("mode", "LOCAL_SYNTHETIC");
        m.put("operation", "FFT_Q16_TEST");
        m.put("subject_present", true);
        m.put("consent", "EXPLICIT_LOCAL_UI");
        m.put("target", "fixture:fft8-q16");
        m.put("privacy_class", "SYNTHETIC");
        m.put("write_requested", false);
        m.put("dry_run", true);
        m.put("source_ref", "synthetic:fft8-q16");
        m.put("claim_allowed", false);
        return m;
    }
    private static void mutation(String key, Object value) {
        Map<String,Object> m=base();m.put(key,value);deny(m,key+"="+value);
    }
    public static void main(String[] args) {
        String fixture = "{\"schema\":\"rafaelia.capability-request.s0\",\"request_id\":\"RAFAELIA-S0-001\",\"origin\":\"APK_UI\",\"mode\":\"LOCAL_SYNTHETIC\",\"operation\":\"FFT_Q16_TEST\",\"subject_present\":true,\"consent\":\"EXPLICIT_LOCAL_UI\",\"target\":\"fixture:fft8-q16\",\"privacy_class\":\"SYNTHETIC\",\"write_requested\":false,\"dry_run\":true,\"source_ref\":\"synthetic:fft8-q16\",\"claim_allowed\":false}";
        Map<String,Object> parsed=StrictEnvelopeJson.parse(fixture);
        ok(parsed.equals(base()),"parsed_base");
        CapabilityEnvelope.Result pass=CapabilityEnvelope.validate(parsed);
        ok(pass.state.equals("PREFLIGHT_SYNTHETIC_ONLY"),"base_preview");
        ok(pass.reason.equals("PROVIDER_AUTH_NOT_CHECKED"),"no_auth_claim");
        ok(pass.digest.matches("[0-9a-f]{64}"),"digest_shape");
        ok(!pass.claimAllowed,"claim_stays_false");
        ok(pass.digest.equals(CapabilityEnvelope.validate(base()).digest),"canonical_sort_order_independent");
        // The exact canonical SHA-256 will be independently cross-tested with Python below.
        System.out.println("S0.1_FIXTURE_DIGEST="+pass.digest);
        badJson("", "empty_json");
        badJson("[]", "no_array");
        badJson("null", "no_null");
        badJson("{\"a\":true,\"a\":false}", "duplicate_key");
        badJson("{\"a\":true,}", "trailing_comma");
        badJson("{\"a\":10}", "no_number");
        badJson("{\"a\":null}", "no_null_value");
        badJson("{\"a\":[]}", "no_array_value");
        badJson("{\"a\":{}}", "no_nested_value");
        badJson("{\"a\":\"é\"}", "reject_unicode_in_ascii_protocol");
        badJson("{\"a\":\"\\n\"}", "reject_controls");
        badJson("{\"a\":false} garbage", "trailing_garbage");
        badJson("{\"a\":false", "unterminated_object");
        badJson("{\"a\":\"unfinished}", "unterminated_string");
        badJson("{"+"x".repeat(5000)+"}", "oversize");
        Map<String,Object> m=base();m.remove("consent");deny(m,"missing_consent");
        m=base();m.put("pat","FAKE_DUMMY");deny(m,"extra_credential_field");
        mutation("consent","NONE");
        mutation("origin","BACKGROUND");
        mutation("subject_present",false);
        mutation("write_requested",true);
        mutation("dry_run",false);
        mutation("claim_allowed",true);
        mutation("mode","GITHUB_PRIVATE");
        mutation("privacy_class","PRIVATE_CONTENT");
        mutation("privacy_class","PRIVATE_METADATA");
        mutation("privacy_class","UNKNOWN");
        mutation("target","github:unknown/private-corpus");
        mutation("source_ref","file:///home/private");
        mutation("operation","DELETE_ALL");
        mutation("request_id","ab");
        mutation("request_id","");
        mutation("request_id","A".repeat(81));
        mutation("request_id",12);
        mutation("schema","rafaelia.capability-request.s1");
        mutation("write_requested","false");
        mutation("subject_present","true");
        mutation("claim_allowed",null);
        mutation("source_ref",null);
        mutation("source_ref","synthetic:fft8-q16"+"x".repeat(240));
        m=base();m.put("origin","GPT_UI");
        ok(CapabilityEnvelope.validate(m).state.equals("PREFLIGHT_SYNTHETIC_ONLY"),"untrusted_gpt_can_only_preview");
        m=base();m.put("mode","GITHUB_PUBLIC_METADATA");m.put("operation","FETCH_PUBLIC_METADATA_PREVIEW");m.put("privacy_class","PUBLIC");
        m.put("target","github:rafaelmeloreisnovo/Raf_FFT");m.put("source_ref","commit:"+"a".repeat(40));
        ok(CapabilityEnvelope.validate(m).state.equals("PREFLIGHT_SYNTHETIC_ONLY"),"public_metadata_preview_only");
        m.put("source_ref","main");deny(m,"no_mutable_head");
        m.put("source_ref","commit:"+"a".repeat(40));m.put("target","github:rafaelmeloreisnovo/Mapa");deny(m,"wrong_public_target");
        m=base();m.put("mode","DRIVE_SAF_LOCAL_PREVIEW");m.put("operation","LIST_SELECTED_URI_PREVIEW");
        m.put("target","android:saf:user-chosen-synthetic");m.put("source_ref","synthetic:saf-selection");
        ok(CapabilityEnvelope.validate(m).state.equals("PREFLIGHT_SYNTHETIC_ONLY"),"saf_shape_only");
        m.put("target","gdrive:private");deny(m,"drive_private_blocked");
        deny(null,"null_envelope");
        System.out.println("S0.1_CAPABILITY_CHECKS="+checks+" PASS_SCOPED");
    }
}
