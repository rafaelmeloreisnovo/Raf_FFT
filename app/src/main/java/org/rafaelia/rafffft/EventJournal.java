package org.rafaelia.rafffft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Local, metadata-only, append-only audit. Hash chain is integrity evidence, NOT a signature. */
public final class EventJournal {
    private static final long MAX_BYTES = 2L * 1024L * 1024L;
    private static final String GENESIS =
        "0000000000000000000000000000000000000000000000000000000000000000";
    private final File file;
    private long sequence;
    private String lastHash = GENESIS;

    public EventJournal(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("JOURNAL_DIR_UNAVAILABLE");
        }
        file = new File(directory, "events_v1.jsonl.txt");
        verify();
    }

    public synchronized long sequence() { return sequence; }
    public synchronized String lastHash() { return lastHash; }

    private static void checkToken(String value) throws IOException {
        if (value == null || !value.matches("[A-Z0-9_]{1,64}"))
            throw new IOException("NONCANONICAL_EVENT_TOKEN");
    }

    private static String sha256(String value) throws IOException {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            char[] out = new char[64];
            final char[] hex = "0123456789abcdef".toCharArray();
            for (int i = 0; i < d.length; i++) {
                out[i*2] = hex[(d[i] & 255) >>> 4];
                out[i*2+1] = hex[d[i] & 15];
            }
            return new String(out);
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA256_UNAVAILABLE", ex);
        }
    }

    private void verify() throws IOException {
        if (!file.exists()) return;
        if (file.length() > MAX_BYTES) throw new IOException("JOURNAL_SIZE_LIMIT");
        if (file.length() != 0) {
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(file.length() - 1);
                if (raf.readByte() != '\n') throw new IOException("JOURNAL_UNTERMINATED");
            }
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                 new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] p = line.split("\\|", -1);
                if (p.length != 6) throw new IOException("JOURNAL_RECORD_SHAPE");
                long next;
                try { next = Long.parseLong(p[0]); }
                catch (NumberFormatException e) { throw new IOException("JOURNAL_SEQUENCE", e); }
                if (next != sequence + 1) throw new IOException("JOURNAL_SEQUENCE_GAP");
                try { Long.parseLong(p[1]); }
                catch (NumberFormatException e) { throw new IOException("JOURNAL_TIME", e); }
                checkToken(p[2]); checkToken(p[3]);
                if (!lastHash.equals(p[4])) throw new IOException("JOURNAL_PARENT_MISMATCH");
                String body = p[0]+"|"+p[1]+"|"+p[2]+"|"+p[3]+"|"+p[4];
                if (!sha256(body).equals(p[5])) throw new IOException("JOURNAL_DIGEST_MISMATCH");
                sequence = next;
                lastHash = p[5];
            }
        }
    }

    /** Refuse all new operations if the durable log cannot be appended. */
    public synchronized String append(String event, String decision) throws IOException {
        checkToken(event); checkToken(decision);
        if (file.length() >= MAX_BYTES) throw new IOException("JOURNAL_SIZE_LIMIT");
        String body = (sequence+1)+"|"+System.currentTimeMillis()+"|"+event+"|"+decision+"|"+lastHash;
        String h = sha256(body);
        byte[] bytes = (body+"|"+h+"\n").getBytes(StandardCharsets.UTF_8);
        if (file.length() + bytes.length > MAX_BYTES) throw new IOException("JOURNAL_SIZE_LIMIT");
        try (FileOutputStream out = new FileOutputStream(file, true)) {
            out.write(bytes);
            out.flush();
            out.getFD().sync();
        }
        sequence++;
        lastHash = h;
        return h;
    }
}
