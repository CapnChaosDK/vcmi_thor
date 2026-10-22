package dev.vcmi.builds;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public final class AdvancedDownloadsActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private EditText repo, token, branch;
    private Spinner source;
    private LinearLayout results;
    private TextView status;
    private Button refresh, install, saveToken, forgetToken;
    private TokenStore tokenStore;
    private TextView tokenStatus;
    private ProgressBar progress;
    private boolean busy, awaitingPermission;
    private String installedCandidate;
    private static final long MAX_BYTES = 2L * 1024 * 1024 * 1024;
    private static final Uri APK_URI = Uri.parse("content://dev.vcmi.builds.apk/build.apk");

    private static final class BuildItem {
        final String label, detail, url; final boolean zip;
        BuildItem(String l, String d, String u, boolean z) { label=l; detail=d; url=u; zip=z; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(28, 40, 28, 32); page.setBackgroundColor(Color.rgb(18,23,32)); scroll.addView(page);
        TextView title = text("VCMI · THOR", 28); page.addView(title);
        page.addView(text("Advanced downloads — no candidate checklist or verification", 17));
        page.addView(text("Repository", 14));
        repo = input("owner/repository or GitHub URL", false); page.addView(repo);
        repo.setText(getPreferences(0).getString("repo", "CapnChaosDK/vcmi_thor"));
        source = new Spinner(this);
        source.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"GitHub Releases", "GitHub Actions artifacts"}));
        source.setSelection(getPreferences(0).getInt("source", 0)); page.addView(source);
        branch = input("Branch (Actions only; blank = all)", false); page.addView(branch);
        branch.setText(getPreferences(0).getString("branch", "ci/thor-candidate-validation"));
        token = input("GitHub token (required for Actions/private repo)", true); page.addView(token);
        token.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        token.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        tokenStore = new TokenStore(this);
        tokenStatus = text("Save token to remember it on this device.",12); page.addView(tokenStatus);
        try {
            token.setText(tokenStore.load());
            if (token.length()>0) tokenStatus.setText("Saved token restored securely.");
        } catch (Exception e) { tokenStatus.setText("Saved token could not be unlocked. Paste and save it again, or tap Forget token."); }
        saveToken = new Button(this); saveToken.setText("Save token"); page.addView(saveToken);
        saveToken.setOnClickListener(v -> saveToken());
        forgetToken = new Button(this); forgetToken.setText("Forget token"); page.addView(forgetToken);
        forgetToken.setOnClickListener(v -> {
            token.setText(""); results.removeAllViews();
            try {
                tokenStore.clear();
                tokenStatus.setText("Token forgotten on this device. This does not revoke it on GitHub.");
            } catch (Exception e) { tokenStatus.setText("Could not remove the saved token. Please try again."); }
        });
        page.addView(text("Token is encrypted on this device. Device: " + String.join(", ", Build.SUPPORTED_ABIS), 12));
        refresh = new Button(this); refresh.setText("Check newest builds"); page.addView(refresh);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setVisibility(View.GONE); page.addView(progress);
        status = text("Enter your repository to get started.", 14); page.addView(status);
        install = new Button(this); install.setText("Install downloaded APK"); install.setVisibility(View.GONE); page.addView(install);
        install.setOnClickListener(v -> installApk());
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); page.addView(results);
        refresh.setOnClickListener(v -> load()); setContentView(scroll);
        if (new File(getFilesDir(), "build.apk").exists()) {
            installedCandidate = getPreferences(0).getString("candidate", "Previously downloaded build");
            install.setVisibility(View.VISIBLE); status.setText("Ready: " + installedCandidate);
        }
    }
    private TextView text(String value, int size) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(Color.rgb(225,232,242)); t.setPadding(0,12,0,12); return t;
    }
    private EditText input(String hint, boolean secret) {
        EditText t=new EditText(this); t.setHint(hint); t.setSingleLine(true); t.setTextSize(15);
        t.setInputType(secret ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        t.setSaveEnabled(!secret); return t;
    }
    private void working(boolean value) {
        busy=value; refresh.setEnabled(!value); repo.setEnabled(!value); source.setEnabled(!value);
        branch.setEnabled(!value); token.setEnabled(!value); install.setEnabled(!value);
        saveToken.setEnabled(!value); forgetToken.setEnabled(!value);
        for (int i=0;i<results.getChildCount();i++) results.getChildAt(i).setEnabled(!value);
        progress.setVisibility(value ? View.VISIBLE : View.GONE); progress.setIndeterminate(true);
    }
    private void message(String s) { runOnUiThread(() -> { if (!isDestroyed()) status.setText(s); }); }
    private void fail(Exception e) {
        runOnUiThread(() -> { if (!isDestroyed()) { working(false); status.setText("Could not complete: " + e.getMessage()); } });
    }
    static String repository(String raw) {
        String value=raw.trim().replaceFirst("^https://github\\.com/", "").replaceFirst("/+$", "").replaceFirst("\\.git$", "");
        if (!value.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Use owner/repository or its GitHub URL.");
        return value;
    }
    private boolean saveToken() {
        try {
            tokenStore.save(token.getText().toString().trim());
            results.removeAllViews(); // Download buttons capture the token used to list their builds.
            tokenStatus.setText(token.length()==0?"No token saved.":"Token saved securely for app restarts and device reboots.");
            return true;
        } catch (Exception e) {
            tokenStatus.setText("Could not save token securely. Tap Forget token, then paste and save it again.");
            return false;
        }
    }
    private void load() {
        if (busy) return;
        final String repository;
        try { repository=repository(repo.getText().toString()); } catch (Exception e) { status.setText(e.getMessage()); return; }
        final String auth=token.getText().toString().trim(), filter=branch.getText().toString().trim();
        final boolean actions=source.getSelectedItemPosition()==1;
        if (actions && auth.isEmpty()) { status.setText("GitHub requires a token to download Actions artifacts. Use a fine-grained token with Actions: read access to this repository."); return; }
        if (!saveToken()) return;
        getPreferences(0).edit().putString("repo", repository).putString("branch", filter).putInt("source", actions?1:0).apply();
        results.removeAllViews(); working(true); status.setText("Looking for builds…");
        worker.execute(() -> {
            try {
                List<BuildItem> items=new ArrayList<>();
                String base="https://api.github.com/repos/"+repository;
                if (!actions) {
                    JSONArray releases=new JSONArray(json(base+"/releases?per_page=30", auth));
                    for (int i=0;i<releases.length();i++) {
                        JSONObject release=releases.getJSONObject(i);
                        if (release.optBoolean("draft")) continue;
                        JSONArray assets=release.getJSONArray("assets");
                        for (int a=0;a<assets.length();a++) {
                            JSONObject asset=assets.getJSONObject(a); String name=asset.getString("name");
                            if (!name.toLowerCase(Locale.ROOT).endsWith(".apk")) continue;
                            items.add(new BuildItem(name, release.optString("tag_name")+" · "+asset.optString("updated_at")+" · "+mb(asset.optLong("size")), asset.getString("url"), false));
                        }
                    }
                } else {
                    JSONObject response=new JSONObject(json(base+"/actions/runs?status=success&per_page=30"+(filter.isEmpty()?"":"&branch="+URLEncoder.encode(filter,"UTF-8")), auth));
                    JSONArray runs=response.getJSONArray("workflow_runs");
                    for (int i=0;i<runs.length() && items.size()<40;i++) {
                        JSONObject run=runs.getJSONObject(i);
                        JSONArray artifacts=new JSONObject(json(base+"/actions/runs/"+run.getLong("id")+"/artifacts?per_page=100", auth)).getJSONArray("artifacts");
                        for (int a=0;a<artifacts.length();a++) {
                            JSONObject artifact=artifacts.getJSONObject(a); if (artifact.optBoolean("expired")) continue;
                            String sha=run.optString("head_sha"); if (sha.length()>7) sha=sha.substring(0,7);
                            items.add(new BuildItem(artifact.getString("name"),run.optString("name")+" #"+run.optLong("run_number")+" · "+run.optString("head_branch")+" · "+sha+"\n"+run.optString("created_at")+" · "+mb(artifact.optLong("size_in_bytes")),artifact.getString("archive_download_url"),true));
                        }
                    }
                }
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    working(false); status.setText(items.isEmpty()?"No builds found. Check the source, branch, or whether artifacts have expired.":"Newest first. Choose the APK for your device; Actions archives may contain several files.");
                    for (BuildItem item:items) {
                        Button button=new Button(this); button.setAllCaps(false); button.setText(item.label+"\n"+item.detail+"\nDownload" ); results.addView(button);
                        button.setOnClickListener(v -> download(item, auth));
                    }
                });
            } catch (Exception e) { fail(e); }
        });
    }
    private static String mb(long value) { return String.format(Locale.ROOT,"%.1f MB",value/1048576.0); }
    private HttpURLConnection connect(String address, String auth, String accept) throws IOException {
        URL url=new URL(address);
        for (int redirects=0;redirects<8;redirects++) {
            if (!"https".equals(url.getProtocol())) throw new IOException("Only HTTPS downloads are supported");
            HttpURLConnection c=(HttpURLConnection)url.openConnection(); c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(20000); c.setReadTimeout(30000); c.setRequestProperty("User-Agent","VCMI-Builds/0.1"); c.setRequestProperty("Accept",accept);
            if ("api.github.com".equals(url.getHost()) && !auth.isEmpty()) c.setRequestProperty("Authorization","Bearer "+auth);
            int code=c.getResponseCode();
            if (code>=300 && code<400) {
                String location=c.getHeaderField("Location"); c.disconnect();
                if (location==null) throw new IOException("Missing download redirect"); url=new URL(url,location); continue;
            }
            if (code<200 || code>=300) {
                c.disconnect(); throw new IOException("GitHub HTTP "+code+(code==401||code==403?" — check token permissions or API rate limit":code==404?" — repository/build not found, expired, or private":""));
            }
            return c;
        }
        throw new IOException("Too many redirects");
    }
    private String json(String url, String auth) throws IOException {
        HttpURLConnection c=connect(url,auth,"application/vnd.github+json");
        try (InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[16384]; int n;
            while ((n=in.read(buffer))!=-1) { if (out.size()+n>16*1024*1024) throw new IOException("API response too large"); out.write(buffer,0,n); }
            return out.toString("UTF-8");
        } finally { c.disconnect(); }
    }
    private void download(BuildItem item, String auth) {
        if (busy) return; working(true); status.setText("Downloading "+item.label+"…");
        worker.execute(() -> {
            File partial=new File(getCacheDir(),"download.part");
            try {
                // Actions returns a redirect using GitHub's API media type; release assets use octet-stream.
                HttpURLConnection c=connect(item.url,auth,item.zip?"application/vnd.github+json":"application/octet-stream");
                long expected=c.getContentLengthLong(), total=0, lastUpdate=0;
                try (InputStream in=c.getInputStream(); OutputStream out=new FileOutputStream(partial)) {
                    byte[] buffer=new byte[65536]; int n;
                    while ((n=in.read(buffer))!=-1) {
                        if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
                        total+=n; if (total>MAX_BYTES) throw new IOException("Download exceeds 2 GB limit"); out.write(buffer,0,n);
                        if (System.currentTimeMillis()-lastUpdate>300) {
                            lastUpdate=System.currentTimeMillis(); final long bytes=total;
                            runOnUiThread(() -> { if (!isDestroyed()) { status.setText("Downloading "+mb(bytes)+(expected>0?" / "+mb(expected):"")); if (expected>0) {progress.setIndeterminate(false); progress.setProgress((int)(bytes*100/expected));} } });
                        }
                    }
                    if (expected>=0 && total!=expected) throw new IOException("Incomplete download; try again");
                } finally { c.disconnect(); }
                if (item.zip) chooseApk(partial,item.label); else prepare(partial,item.label);
            } catch (Exception e) { partial.delete(); fail(e); }
        });
    }
    private void chooseApk(File archive, String label) throws IOException {
        List<String> apks=new ArrayList<>();
        try (ZipFile zip=new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries=zip.entries();
            while (entries.hasMoreElements()) { ZipEntry e=entries.nextElement(); if (!e.isDirectory() && e.getName().toLowerCase(Locale.ROOT).endsWith(".apk")) apks.add(e.getName()); }
        }
        if (apks.isEmpty()) throw new IOException("This artifact contains no APK. Select an Android APK artifact.");
        if (apks.size()==1) { extract(archive,apks.get(0)); prepare(new File(getCacheDir(),"extracted.apk"),label+" / "+apks.get(0)); archive.delete(); return; }
        runOnUiThread(() -> {
            if (isDestroyed()) return;
            new AlertDialog.Builder(this).setTitle("Choose APK · "+Build.SUPPORTED_ABIS[0]).setItems(apks.toArray(new String[0]),(dialog,index) -> worker.execute(() -> {
                try { extract(archive,apks.get(index)); prepare(new File(getCacheDir(),"extracted.apk"),label+" / "+apks.get(index)); } catch (Exception e) { fail(e); } finally { archive.delete(); }
            })).setOnCancelListener(dialog -> { archive.delete(); working(false); status.setText("Download cancelled."); }).show();
        });
    }
    private void extract(File archive, String entry) throws IOException {
        // Never use archive paths as filesystem paths.
        File target=new File(getCacheDir(),"extracted.apk");
        try (ZipFile zip=new ZipFile(archive); InputStream in=zip.getInputStream(zip.getEntry(entry)); OutputStream out=new FileOutputStream(target)) {
            byte[] buffer=new byte[65536]; long total=0; int n;
            while ((n=in.read(buffer))!=-1) { if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled"); total+=n; if (total>MAX_BYTES) throw new IOException("APK exceeds 2 GB limit"); out.write(buffer,0,n); }
        } catch (IOException e) { target.delete(); throw e; }
    }
    private void prepare(File file, String label) throws IOException {
        if (getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),0)==null) { file.delete(); throw new IOException("Downloaded file is not a readable Android APK"); }
        File destination=new File(getFilesDir(),"build.apk");
        if (!file.renameTo(destination)) throw new IOException("Could not save APK");
        getPreferences(0).edit().putString("candidate",label).apply(); installedCandidate=label;
        runOnUiThread(() -> { if (!isDestroyed()) { working(false); install.setVisibility(View.VISIBLE); status.setText("Ready to install: "+label); } });
    }
    private void installApk() {
        if (!getPackageManager().canRequestPackageInstalls()) {
            awaitingPermission=true;
            status.setText("Allow installation from VCMI Builds, then return here.");
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()))); return;
        }
        try {
            Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(APK_URI,"application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); intent.setClipData(ClipData.newRawUri("APK",APK_URI)); startActivity(intent);
        } catch (Exception e) { status.setText("Unable to open Android installer: "+e.getMessage()); }
    }
    @Override protected void onResume() { super.onResume(); if (awaitingPermission) { awaitingPermission=false; if (getPackageManager().canRequestPackageInstalls()) installApk(); } }
    @Override protected void onDestroy() { super.onDestroy(); worker.shutdownNow(); }
}
