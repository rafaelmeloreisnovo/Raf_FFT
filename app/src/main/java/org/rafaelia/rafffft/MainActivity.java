package org.rafaelia.rafffft;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.json.JSONObject;

/** S0 diagnostic surface: no private reads, no auth tokens, no background service. */
public final class MainActivity extends Activity {
    private static final int PICK_TREE = 410;
    private static final String PUBLIC_URL =
        "https://api.github.com/repos/rafaelmeloreisnovo/Raf_FFT/contents/README.md";
    private EventJournal journal;
    private boolean auditReady;
    private TextView status;
    private TextView audit;
    private boolean busy;
    private EditText envelopeInput;

    private TextView text(LinearLayout parent, String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(12, 8, 12, 9);
        t.setTextIsSelectable(true);
        parent.addView(t, new LinearLayout.LayoutParams(-1, -2));
        return t;
    }

    private void action(LinearLayout root, String name, Runnable operation) {
        Button b = new Button(this);
        b.setText(name);
        b.setAllCaps(false);
        b.setOnClickListener(v -> operation.run());
        root.addView(b, new LinearLayout.LayoutParams(-1, -2));
    }

    private boolean record(String name, String decision) {
        if (!auditReady) {
            status.setText("DENY_AUDIT_UNAVAILABLE: nenhuma operação permitida");
            return false;
        }
        try {
            journal.append(name, decision);
            audit.setText("Recibo local: evento #"+journal.sequence()
                +" | SHA-256="+journal.lastHash().substring(0, 16)
                +"…\nSem nomes de arquivos ou conteúdo privado no journal.");
            return true;
        } catch (Exception ex) {
            auditReady = false;
            status.setText("DENY_AUDIT_WRITE_FAILURE. Inspecionar antes de continuar.");
            return false;
        }
    }

    private boolean authorize(PolicyGate.Operation op) {
        PolicyGate.Decision d = PolicyGate.decide(op, true);
        return record(op.name(), d.code) && d.allowed;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(19,31,47));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(19,31,47));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 8, 12, 20);
        scroll.addView(root);
        setContentView(scroll);

        text(root,"RAFAELIA Ω · Raf_FFT / S0.1",20,Color.WHITE);
        text(root,"BANCADA PÚBLICA · SEM DADOS PRIVADOS · redimensionável para tela dividida",
            12,Color.rgb(140,220,205));
        status = text(root,"Inicializando auditoria local…",14,Color.WHITE);
        audit = text(root,"JOURNAL=TOKEN_VAZIO",12,Color.rgb(206,221,243));
        try {
            journal = new EventJournal(getNoBackupFilesDir());
            auditReady = true;
            if (!record("APP_START","ALLOW_LOCAL_SANDBOX")) auditReady = false;
            if (auditReady) status.setText("PRONTO_S0_1 · Privado=DENY · Rede=ação manual");
        } catch (Exception ex) {
            auditReady = false;
            status.setText("DENY_AUDIT_INIT_FAILURE · Nenhum acesso externo permitido");
        }

        text(root,"PEDIDO GPT/APK (pré-validação offline, NÃO é execução)",13,Color.rgb(140,220,205));
        envelopeInput = new EditText(this);
        envelopeInput.setTextColor(Color.WHITE);
        envelopeInput.setHintTextColor(Color.LTGRAY);
        envelopeInput.setTextSize(12);
        envelopeInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        envelopeInput.setMinLines(3);
        envelopeInput.setMaxLines(7);
        envelopeInput.setContentDescription("Pedido JSON de teste sintético, sem segredos ou dados privados");
        envelopeInput.setText("{\"schema\":\"rafaelia.capability-request.s0\","
            + "\"request_id\":\"RAFAELIA-S0-001\",\"origin\":\"APK_UI\","
            + "\"mode\":\"LOCAL_SYNTHETIC\",\"operation\":\"FFT_Q16_TEST\","
            + "\"subject_present\":true,\"consent\":\"EXPLICIT_LOCAL_UI\","
            + "\"target\":\"fixture:fft8-q16\",\"privacy_class\":\"SYNTHETIC\","
            + "\"write_requested\":false,\"dry_run\":true,"
            + "\"source_ref\":\"synthetic:fft8-q16\",\"claim_allowed\":false}");
        root.addView(envelopeInput,new LinearLayout.LayoutParams(-1,-2));
        action(root,"0 · Validar pedido (SEM executar)",this::validateDraft);
        text(root,"Pedidos vindos do GPT são texto não confiável. Somente o toque no botão"
            +" valida a forma; execução exige OUTRO toque e seu próprio gate.",11,Color.LTGRAY);

        action(root,"1 · Teste sintético offline", () -> {
            if (!authorize(PolicyGate.Operation.SYNTHETIC_TEST)) return;
            int[] real = {65536,0,0,0,0,0,0,0};
            int[] imaginary = new int[8];
            FftQ16.forward8(real, imaginary);
            boolean pass = true;
            for (int i=0; i<8; i++) if (real[i]!=65536 || imaginary[i]!=0) pass=false;
            if (record("SYNTHETIC_RESULT",pass?"PASS_SCOPED":"FAIL_SCOPED"))
                status.setText("FFT radix-2 Q16 N=8 · impulso → 8 bins=1 · "
                    +(pass?"PASS_SCOPED":"FAIL_SCOPED")+" · sem corpus ou rede");
        });
        action(root,"2 · GitHub público: README do Raf_FFT", this::readPublicGithub);
        action(root,"3 · Escolher pasta Drive (somente seletor)", this::chooseDriveTree);
        action(root,"4 · Testar negação: GitHub privado", () -> {
            authorize(PolicyGate.Operation.PRIVATE_GITHUB_READ);
            if (auditReady) status.setText("DENY_PRIVATE_AUTH_NOT_VERIFIED · nenhum GET privado");
        });
        action(root,"5 · Testar negação: conteúdo Drive", () -> {
            authorize(PolicyGate.Operation.DRIVE_CONTENT_READ);
            if (auditReady) status.setText("DENY_DRIVE_ACCOUNT_AND_SCOPE_UNVERIFIED · sem abrir arquivos");
        });
        action(root,"6 · Revogar acesso SAF à pasta", this::revokeFolderAccess);
        action(root,"7 · Copiar recibo mínimo para colar no GPT", this::copyReceipt);
        text(root,"GATES: ACCOUNT_VERIFIED=TOKEN_VAZIO · FOLDER_CONTENT=BLOCKED"
                +" · PRIVATE_GITHUB=BLOCKED · CLAIM_ALLOWED=false",
            11,Color.rgb(251,199,115));
    }

    /** Any pasted GPT request is treated as untrusted input, never as provider authority. */
    private void validateDraft() {
        if (!auditReady) { status.setText("DENY_AUDIT_UNAVAILABLE"); return; }
        CapabilityEnvelope.Result outcome;
        try {
            Map<String,Object> request=StrictEnvelopeJson.parse(envelopeInput.getText().toString());
            outcome=CapabilityEnvelope.validate(request);
        } catch (RuntimeException ex) {
            outcome=CapabilityEnvelope.validate(null);
        }
        // Journal contains only generic codes, never draft bytes, IDs, paths or tokens.
        if (!record("CAPABILITY_PREFLIGHT",outcome.state.equals("BLOCKED")
            ? "DENY_REQUEST_BLOCKED" : "OBSERVED_PREVIEW_ONLY")) return;
        if (outcome.state.equals("BLOCKED")) {
            status.setText("BLOCKED: "+outcome.reason+" · conteúdo não registrado · nenhuma ação executada");
        } else {
            status.setText("PREFLIGHT_SYNTHETIC_ONLY · "+outcome.reason
                +" · envelope SHA-256="+outcome.digest.substring(0,16)
                +"… · execute separadamente, se desejar");
        }
    }

    private void readPublicGithub() {
        if (busy) { record("PUBLIC_GITHUB_READ","DENY_DUPLICATE_IN_FLIGHT"); return; }
        if (!authorize(PolicyGate.Operation.PUBLIC_GITHUB_READ)) return;
        busy=true;
        status.setText("GET público único, URL allowlisted, limite 16 KiB…");
        new Thread(() -> {
            String outcome = "FAIL_PROVIDER";
            String display = "Falha tipada na consulta pública. Não houve operação privada.";
            HttpURLConnection conn = null;
            try {
                URL url = new URL(PUBLIC_URL);
                if (!"https".equals(url.getProtocol())
                    || !"api.github.com".equals(url.getHost())
                    || !"/repos/rafaelmeloreisnovo/Raf_FFT/contents/README.md".equals(url.getPath()))
                    throw new SecurityException("HOST_SCOPE");
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("Accept","application/vnd.github+json");
                conn.setRequestProperty("User-Agent","RafFFT-S0-public-readonly");
                int code = conn.getResponseCode();
                if (code != 200) throw new IllegalStateException("HTTP_NON_200");
                try (InputStream in = conn.getInputStream();
                     ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024]; int count;
                    while ((count = in.read(buffer)) != -1) {
                        if (bytes.size()+count > 16384) throw new IllegalStateException("BOUND_EXCEEDED");
                        bytes.write(buffer,0,count);
                    }
                    JSONObject json = new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
                    String sha = json.optString("sha", "");
                    if (!"README.md".equals(json.optString("path"))
                        || !"file".equals(json.optString("type"))
                        || !sha.matches("[0-9a-f]{40}")) throw new SecurityException("RESPONSE_SCOPE");
                    outcome = "PASS_SCOPED";
                    display = "GitHub público OK · README.md · Git blob SHA="+sha.substring(0,12)
                        +"… · bytes declarados="+json.optInt("size",-1);
                }
            } catch (Exception ignored) {
                // Error text may contain addresses or provider data; never persist it.
            } finally {
                if (conn!=null) conn.disconnect();
            }
            final String result=outcome, msg=display;
            runOnUiThread(() -> {
                busy=false;
                if (record("PUBLIC_GITHUB_RESULT",result)) status.setText(msg);
            });
        },"raffft-public-probe").start();
    }

    private void chooseDriveTree() {
        if (!authorize(PolicyGate.Operation.DRIVE_TREE_PICKER)) return;
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            // Do not request a persistent grant in S0.1: no file reading is authorized.
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i,PICK_TREE);
            status.setText("Seletor do Android aberto; nenhuma pasta foi lida.");
        } catch (RuntimeException ex) {
            if (record("DRIVE_PICKER_RESULT","DENY_PICKER_UNAVAILABLE"))
                status.setText("DENY_PICKER_UNAVAILABLE");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode != PICK_TREE) return;
        if (resultCode != RESULT_OK || data == null || data.getData()==null) {
            if (record("DRIVE_PICKER_RESULT","DENY_USER_CANCELLED")) status.setText("Seletor cancelado");
            return;
        }
        Uri selected = data.getData();
        if (!"content".equals(selected.getScheme())) {
            record("DRIVE_PICKER_RESULT","DENY_UNSAFE_URI_SCHEME");
            return;
        }
        // A user selection is not Google-account validation. S0.1 neither stores URI
        // nor persists the returned temporary read grant; all content remains denied.
        if (record("DRIVE_PICKER_RESULT","OBSERVED_TEMPORARY_REFERENCE_ONLY")) {
            status.setText("Referência SAF selecionada sem armazenamento ou leitura."
                +" CONTA=TOKEN_VAZIO · CONTEÚDO=DENY · GRANT_PERSISTENTE=NAO_SOLICITADO");
        }
    }

    private void revokeFolderAccess() {
        if (!auditReady) return;
        if (!record("SAF_REVOKE_REQUEST","ALLOW_USER_REVOCATION")) return;
        int released=0;
        try {
            for (android.content.UriPermission permission : getContentResolver().getPersistedUriPermissions()) {
                getContentResolver().releasePersistableUriPermission(permission.getUri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                released++;
            }
            if (record("SAF_REVOKE_RESULT","OBSERVED_GRANTS_REVOKED"))
                status.setText("Revogadas "+released+" permissões SAF do Raf_FFT; arquivos intocados.");
        } catch (SecurityException ex) {
            if (record("SAF_REVOKE_RESULT","FAIL_GRANT_REVOCATION"))
                status.setText("FAIL_GRANT_REVOCATION · revisão necessária");
        }
    }

    private void copyReceipt() {
        if (!authorize(PolicyGate.Operation.CLIPBOARD_RECEIPT)) return;
        String receipt = "RAFAELIA_RAF_FFT_S0\n"
            +"scope=PUBLIC_METADATA_AND_SYNTHETIC_ONLY\n"
            +"private_github=DENY\ndrive_content=DENY\n"
            +"account_identity=TOKEN_VAZIO\n"
            +"journal_sequence="+journal.sequence()+"\n"
            +"journal_sha256_chain_tip="+journal.lastHash()+"\n"
            +"claim_allowed=false\n";
        ClipboardManager cb = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if (cb == null) { record("CLIPBOARD_RESULT","FAIL_CLIPBOARD_UNAVAILABLE"); return; }
        cb.setPrimaryClip(ClipData.newPlainText("RAFAELIA S0 receipt (metadata only)",receipt));
        if (record("CLIPBOARD_RESULT","OBSERVED_COPY_SANITIZED"))
            status.setText("Recibo mínimo copiado. Cole manualmente na janela GPT; não contém corpus.");
    }
}
