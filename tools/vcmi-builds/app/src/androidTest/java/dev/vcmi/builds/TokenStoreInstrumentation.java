package dev.vcmi.builds;

import android.app.Instrumentation;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import java.security.KeyStore;

/** Uses an isolated record and synthetic token; never reads the user's saved token. */
public final class TokenStoreInstrumentation extends Instrumentation {
    private String phase;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); phase=arguments==null?null:arguments.getString("phase"); start(); }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    @Override public void onStart() {
        Bundle result=new Bundle();
        TokenStore store=new TokenStore(getTargetContext(),"token-store-test","dev.vcmi.builds.token-test");
        if ("seed".equals(phase) || "verify".equals(phase)) {
            try {
                if ("seed".equals(phase)) store.save("synthetic-upgrade-check");
                else { check(store.load().equals("synthetic-upgrade-check"),"Token survives process restart and APK replacement"); store.clear(); }
                result.putString("stream","TokenStore "+phase+": passed\n"); finish(Activity.RESULT_OK,result);
            } catch (Throwable failure) { result.putString("stream","TokenStore "+phase+": failed\n"); finish(Activity.RESULT_CANCELED,result); }
            return;
        }
        try {
            Context context=getTargetContext();
            store.clear(); check(store.load().isEmpty(),"Empty store");
            store.save("synthetic-test-token");
            check(new TokenStore(context,"token-store-test","dev.vcmi.builds.token-test").load().equals("synthetic-test-token"),"Restore from a new store instance");
            String encoded=context.getSharedPreferences("token-store-test",0).getString("encrypted","");
            check(!encoded.contains("synthetic-test-token"),"Plaintext must not be stored");
            store.save("synthetic-test-token");
            check(!encoded.equals(context.getSharedPreferences("token-store-test",0).getString("encrypted","")),"Fresh nonce for each save");
            String[] parts=encoded.split(":");
            byte[] tampered=android.util.Base64.decode(parts[1],android.util.Base64.NO_WRAP); tampered[0]^=1;
            context.getSharedPreferences("token-store-test",0).edit().putString("encrypted",parts[0]+":"+android.util.Base64.encodeToString(tampered,android.util.Base64.NO_WRAP)).commit();
            boolean rejected=false;
            try { store.load(); } catch (java.security.GeneralSecurityException expected) { rejected=true; }
            check(rejected,"Tampered ciphertext rejected");
            store.save("replacement-test-token"); check(store.load().equals("replacement-test-token"),"Replacement");
            context.getSharedPreferences("token-store-test",0).edit().putString("encrypted","broken").commit();
            rejected=false;
            try { store.load(); } catch (java.security.GeneralSecurityException expected) { rejected=true; }
            check(rejected,"Corrupt record rejected");
            store.clear(); store.save("after-corruption"); check(store.load().equals("after-corruption"),"Recovery");
            KeyStore keys=KeyStore.getInstance("AndroidKeyStore"); keys.load(null); keys.deleteEntry("dev.vcmi.builds.token-test");
            rejected=false;
            try { store.load(); } catch (java.security.GeneralSecurityException expected) { rejected=true; }
            check(rejected,"Missing key rejected");
            store.clear(); store.save("forget-me"); store.save("");
            check(store.load().isEmpty(),"Forget and empty save");
            keys.load(null); check(!keys.containsAlias("dev.vcmi.builds.token-test"),"Forget removes key");
            result.putString("stream","TokenStore: all device checks passed\n"); finish(Activity.RESULT_OK,result);
        } catch (Throwable failure) {
            result.putString("stream","TokenStore check failed: "+failure.getClass().getSimpleName()+": "+failure.getMessage());
            finish(Activity.RESULT_CANCELED,result);
        } finally { try { store.clear(); } catch (Exception ignored) { } }
    }
}
