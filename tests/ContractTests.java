import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.rafaelia.rafffft.EventJournal;
import org.rafaelia.rafffft.FftQ16;
import org.rafaelia.rafffft.PolicyGate;

public final class ContractTests {
    private static int checks = 0;
    private static void require(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        for (PolicyGate.Operation op : PolicyGate.Operation.values()) {
            require(!PolicyGate.decide(op, false).allowed, "no implicit gesture: "+op);
        }
        require(!PolicyGate.decide(null, true).allowed, "unknown operation denied");
        require(PolicyGate.decide(PolicyGate.Operation.SYNTHETIC_TEST,true).allowed, "fixture");
        require(PolicyGate.decide(PolicyGate.Operation.PUBLIC_GITHUB_READ,true).allowed, "public probe");
        require(PolicyGate.decide(PolicyGate.Operation.DRIVE_TREE_PICKER,true).allowed, "picker only");
        require(PolicyGate.decide(PolicyGate.Operation.CLIPBOARD_RECEIPT,true).allowed, "sanitized clipboard");
        require(!PolicyGate.decide(PolicyGate.Operation.PRIVATE_GITHUB_READ,true).allowed, "no private github");
        require(!PolicyGate.decide(PolicyGate.Operation.DRIVE_CONTENT_READ,true).allowed, "no drive data");

        int[] impulse = {65536,0,0,0,0,0,0,0};
        int[] imaginary = new int[8];
        FftQ16.forward8(impulse, imaginary);
        for(int k=0;k<8;k++) require(impulse[k]==65536 && imaginary[k]==0, "FFT impulse bin "+k);
        int[] constant = {8192,8192,8192,8192,8192,8192,8192,8192};
        imaginary = new int[8];
        FftQ16.forward8(constant, imaginary);
        require(constant[0]==65536 && imaginary[0]==0, "FFT DC");
        for(int k=1;k<8;k++) require(constant[k]==0 && imaginary[k]==0, "FFT DC-only bin "+k);
        boolean fftRangeGate=false;
        try { FftQ16.forward8(new int[]{65537,0,0,0,0,0,0,0},new int[8]); }
        catch(IllegalArgumentException ok) { fftRangeGate=true; }
        require(fftRangeGate, "FFT input range fail closed");
        File dir = Files.createTempDirectory("raf_fft_s0_synthetic_").toFile();
        EventJournal j = new EventJournal(dir);
        require(j.sequence() == 0, "genesis");
        j.append("SYNTHETIC_TEST", "ALLOW_OFFLINE_FIXTURE");
        j.append("PRIVATE_GITHUB_READ", "DENY_PRIVATE_AUTH_NOT_VERIFIED");
        String h = j.lastHash();
        EventJournal reloaded = new EventJournal(dir);
        require(reloaded.sequence() == 2 && reloaded.lastHash().equals(h), "readback");
        boolean rejectsInvalid = false;
        try { reloaded.append("USER_DATA:SECRET", "ALLOW"); }
        catch (IOException expected) { rejectsInvalid = true; }
        require(rejectsInvalid, "no arbitrary payload in logs");
        // Deliberate synthetic integrity fault; never operates on any real provider data.
        try (FileOutputStream f = new FileOutputStream(new File(dir,"events_v1.jsonl.txt"),true)) {
            f.write("tamper\n".getBytes(StandardCharsets.UTF_8));
        }
        boolean detected = false;
        try { new EventJournal(dir); }
        catch (IOException expected) { detected = true; }
        require(detected, "tampered journal detected");
        System.out.println("PASS_SCOPED_PURE_JAVA checks="+checks+" no_android_sdk no_network no_private_data");
    }
}
