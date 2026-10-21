package dev.vcmi.builds;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Only authenticated ciphertext is stored in app-private preferences; the key stays in Android Keystore. */
final class TokenStore {
    private final SharedPreferences preferences;
    private final String alias;

    TokenStore(Context context) { this(context, "github-token", "dev.vcmi.builds.github-token.v1"); }
    TokenStore(Context context, String name, String keyAlias) {
        preferences=context.getSharedPreferences(name, Context.MODE_PRIVATE);
        alias=keyAlias;
    }
    private KeyStore keystore() throws GeneralSecurityException, IOException {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore"); store.load(null); return store;
    }
    private SecretKey key(boolean create) throws GeneralSecurityException, IOException {
        KeyStore store=keystore();
        SecretKey existing=(SecretKey)store.getKey(alias,null);
        if (existing!=null) return existing;
        if (!create) throw new GeneralSecurityException("Token key unavailable");
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    void save(String value) throws GeneralSecurityException, IOException {
        if (value.isEmpty()) { clear(); return; }
        if (value.length()>4096) throw new GeneralSecurityException("Token too long");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,key(true));
        String encoded=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+
            Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
        if (!preferences.edit().putString("encrypted",encoded).commit()) throw new IOException("Token save failed");
    }
    String load() throws GeneralSecurityException, IOException {
        String encoded=preferences.getString("encrypted",null);
        if (encoded==null) return "";
        try {
            String[] parts=encoded.split(":",-1);
            if (parts.length!=2) throw new GeneralSecurityException("Invalid token record");
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(false),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) { throw new GeneralSecurityException("Invalid token record",e); }
    }
    void clear() throws GeneralSecurityException, IOException {
        if (!preferences.edit().clear().commit()) throw new IOException("Token removal failed");
        keystore().deleteEntry(alias);
    }
}
