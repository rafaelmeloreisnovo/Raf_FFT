package org.rafaelia.rafffft;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small strict JSON subset for a flat S0 request, rejecting duplicate keys and ambiguities. */
public final class StrictEnvelopeJson {
    private final String data;
    private int pos;
    private static final int MAX_CHARS = 4096;
    private StrictEnvelopeJson(String data) { this.data=data; }
    private void ws() {
        while(pos<data.length()) {
            char c=data.charAt(pos);
            if(c==' ' || c=='\n' || c=='\r' || c=='\t') pos++;
            else break;
        }
    }
    private boolean take(char c) {
        ws();
        if(pos<data.length() && data.charAt(pos)==c) {pos++;return true;}
        return false;
    }
    private IllegalArgumentException fail() { return new IllegalArgumentException("INVALID_S0_JSON"); }
    private String word() {
        ws();
        if(pos>=data.length() || data.charAt(pos++)!='"') throw fail();
        StringBuilder s=new StringBuilder();
        boolean finished=false;
        while(pos<data.length()) {
            char c=data.charAt(pos++);
            if(c=='"') { finished=true; break; }
            if(c=='\\') {
                if(pos>=data.length()) throw fail();
                char e=data.charAt(pos++);
                if(e=='"' || e=='\\' || e=='/') c=e;
                else if(e=='u') {
                    if(pos+4>data.length()) throw fail();
                    int code=0;
                    for(int i=0;i<4;i++) {
                        int digit=Character.digit(data.charAt(pos++),16);
                        if(digit<0) throw fail();
                        code=(code<<4)|digit;
                    }
                    c=(char)code;
                } else throw fail(); // No controls, linefeeds, or ambiguous escapes in S0 values.
            }
            if(c<0x20 || c>0x7e) throw fail(); // Closed ASCII protocol; UTF-8 UI labels remain separate.
            s.append(c);
            if(s.length()>240) throw fail();
        }
        if(!finished) throw fail();
        return s.toString();
    }
    private Object value() {
        ws();
        if(pos>=data.length()) throw fail();
        if(data.charAt(pos)=='"') return word();
        if(data.startsWith("true",pos)) {pos+=4;return Boolean.TRUE;}
        if(data.startsWith("false",pos)) {pos+=5;return Boolean.FALSE;}
        throw fail();
    }
    public static Map<String,Object> parse(String raw) {
        if(raw==null || raw.length()>MAX_CHARS) throw new IllegalArgumentException("S0_JSON_SIZE_LIMIT");
        StrictEnvelopeJson p=new StrictEnvelopeJson(raw);
        if(!p.take('{')) throw p.fail();
        LinkedHashMap<String,Object> out=new LinkedHashMap<>();
        if(!p.take('}')) {
            do {
                String key=p.word();
                if(out.containsKey(key) || out.size()>=13 || !p.take(':')) throw p.fail();
                out.put(key,p.value());
                if(p.take('}')) break;
                if(!p.take(',')) throw p.fail();
            } while(true);
        }
        p.ws();
        if(p.pos!=raw.length()) throw p.fail();
        return Collections.unmodifiableMap(out);
    }
}
