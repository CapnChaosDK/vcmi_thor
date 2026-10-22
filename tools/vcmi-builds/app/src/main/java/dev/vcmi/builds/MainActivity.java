package dev.vcmi.builds;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Candidate-oriented UI. Durable files own results; the foreground service owns downloads. */
public final class MainActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private CandidateStore store;
    private SharedPreferences preferences;
    private LinearLayout page;
    private TextView transfer, installed;
    private Button cancelDownload;
    private String screen="home", selectedKey="", lastTransfer="";
    private JSONObject selected;
    private boolean querying, preparingInstall;
    private int generation;
    private static final int INSTALL=41, UNKNOWN_SOURCES=42;
    private final Runnable ticker=new Runnable() {
        public void run() {
            if (transfer!=null) {
                SharedPreferences state=CandidateDownloadService.state(MainActivity.this);
                String message=state.getString("message","");
                if (state.getBoolean("busy",false) && !CandidateDownloadService.running) {
                    message="Download interrupted. Open Builds and retry.";
                    state.edit().putBoolean("busy",false).putString("message",message).apply();
                }
                if (!message.equals(lastTransfer)) { transfer.setText(message); lastTransfer=message; }
                if (cancelDownload!=null) cancelDownload.setEnabled(downloading());
            }
            handler.postDelayed(this,1000);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); store=new CandidateStore(this);
        preferences=getSharedPreferences("MainActivity",MODE_PRIVATE); // Preserve 0.2.0 setup.
        selectedKey=preferences.getString("selected-key","");
        if (state!=null) screen=state.getString("screen","home");
        if (screen.equals("detail") && !selectedKey.isEmpty()) openLocal(selectedKey);
        else if (screen.equals("settings")) settings(); else home();
    }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); state.putString("screen",screen); }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density); }
    private void shell(String title) {
        generation++;
        cancelDownload=null;
        ScrollView scroll=new ScrollView(this); page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18),dp(24),dp(18),dp(24)); page.setBackgroundColor(Color.rgb(18,23,32)); scroll.addView(page);
        setContentView(scroll); label(title,26); transfer=label(CandidateDownloadService.state(this).getString("message",""),13); lastTransfer=transfer.getText().toString();
        if (!screen.equals("home")) button("Back to builds",v->home());
    }
    private TextView label(String text,int size) {
        TextView view=new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(Color.rgb(225,232,242));
        view.setPadding(0,dp(8),0,dp(8)); page.addView(view); return view;
    }
    private Button button(String text,View.OnClickListener action) {
        Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setOnClickListener(action); page.addView(b); return b;
    }
    private void message(String text) { new AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK",null).show(); }
    private static String shortSha(JSONObject c) { String sha=c.optString("sha",""); return sha.substring(0,Math.min(10,sha.length())); }
    private String repo() { return preferences.getString("repo","CapnChaosDK/vcmi_thor"); }
    private String branch() { return preferences.getString("branch","ci/thor-candidate-validation"); }
    private boolean downloading() { return CandidateDownloadService.running || CandidateDownloadService.state(this).getBoolean("busy",false); }
    private void home() {
        screen="home"; selected=null; shell("Thor · Build & validate");
        label(repo()+"\n"+branch(),14);
        button("Settings",v->settings());
        button("Check newest builds",v->refresh());
        button("Downloaded candidates & results",v->history());
        button("Open latest verified download",v->{
            String key=CandidateDownloadService.state(this).getString("key","");
            if (key.isEmpty()) message("No completed download yet. Choose a ready candidate below."); else openLocal(key);
        });
        if (!selectedKey.isEmpty()) button("Continue previous validation",v->openLocal(selectedKey));
        cancelDownload=button("Cancel download",v->{ if (downloading()) startService(new Intent(this,CandidateDownloadService.class).setAction("cancel")); });
        cancelDownload.setEnabled(downloading());
        File cached=new File(getFilesDir(),"build-list.json");
        try {
            JSONObject cache=CandidateStore.read(cached);
            if (cache.getString("repo").equals(repo()) && cache.getString("branch").equals(branch())) {
                label("Last checked: "+new Date(cache.getLong("at"))+"\nRefresh for current build status.",13);
                renderRuns(cache.getJSONArray("runs"));
            }
        } catch (Exception ignored) { label("Check builds to find the latest candidate, including builds still running or failed.",16); }
    }
    private void renderRuns(JSONArray runs) throws Exception {
        boolean firstReady=true;
        for (int i=0;i<runs.length();i++) {
            JSONObject c=runs.getJSONObject(i);
            String heading=i==0?"Latest run · ":"";
            if (c.has("artifact_id") && firstReady) { heading+="Latest installable · "; firstReady=false; }
            String caption=heading+c.getString("title")+"\n"+c.getString("state")+" · "+shortSha(c)+" · "+c.getString("created_at");
            if (c.has("availability")) caption+="\n"+c.getString("availability");
            button(caption,v->showRemote(c));
        }
        if (runs.length()==0) label("No Thor CI runs found for this branch.",16);
    }
    private void refresh() {
        if (querying) return;
        final String repository=repo(), filter=branch(); querying=true; transfer.setText("Checking GitHub builds…");
        worker.execute(()->{
            try {
                List<JSONObject> runs=GitHubClient.list(repository,filter,new TokenStore(this).load());
                JSONArray array=new JSONArray(); for (JSONObject c:runs) array.put(c);
                CandidateStore.write(new File(getFilesDir(),"build-list.json"),new JSONObject().put("repo",repository).put("branch",filter).put("at",System.currentTimeMillis()).put("runs",array));
                runOnUiThread(()->{ querying=false; if (!isDestroyed() && screen.equals("home")) home(); });
            } catch (Exception e) { runOnUiThread(()->{ querying=false; if (!isDestroyed()) message("Could not refresh: "+e.getMessage()); }); }
        });
    }
    private void showRemote(JSONObject c) {
        screen="remote"; shell(c.optString("title"));
        label(c.optString("state")+" · "+shortSha(c)+"\n"+c.optString("created_at"),16);
        button("View build on GitHub",v->browse(c.optString("run_url")));
        if (c.has("artifact_id")) {
            label("Download: "+(c.optLong("size")/1048576)+" MB\nThe APK, receipt and checklist will be verified together.",15);
            button("Download and verify",v->download(c)).setEnabled(!downloading());
        } else label(c.optString("availability","This run has no installable candidate yet. Refresh Builds when it completes."),16);
        TextView preview=label("Loading feature summary from this commit…",16); int viewGeneration=generation;
        worker.execute(()->{
            String text;
            try {
                java.net.HttpURLConnection connection=GitHubClient.connect("https://api.github.com/repos/"+GitHubClient.repository(c.getString("repo"))+"/contents/CI/thor-validation-plan.json?ref="+c.getString("sha"),new TokenStore(this).load(),"application/vnd.github.raw+json");
                JSONObject plan;
                try (InputStream in=connection.getInputStream()) { plan=new JSONObject(new String(CandidateStore.readBytes(in,256*1024),java.nio.charset.StandardCharsets.UTF_8)); }
                finally { connection.disconnect(); }
                ValidationPlan.validate(plan);
                text=plan.getString("title")+"\n\n"+plan.getString("summary")+"\n\nPreparation\n"+plan.getString("preparation")+"\n\n"+plan.getJSONArray("checks").length()+" planned checks. Download to verify the packaged checklist.";
            } catch (Exception e) { text="A feature preview is unavailable for this commit. Download a ready candidate to inspect its packaged checklist; older builds may not contain one."; }
            final String value=text; runOnUiThread(()->{ if (!isDestroyed() && generation==viewGeneration) preview.setText(value); });
        });
    }
    private void download(JSONObject c) {
        if (downloading()) { message("A download is already running."); return; }
        try {
            if (new TokenStore(this).load().isEmpty()) { message("Save a GitHub token in Settings first. Actions read permission is sufficient for candidate downloads."); return; }
            if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},70);
            startForegroundService(new Intent(this,CandidateDownloadService.class).putExtra("candidate",c.toString()));
            home();
        } catch (Exception e) { message("Could not start download: "+e.getMessage()); }
    }
    private void history() {
        screen="history"; shell("Downloaded candidates & results");
        for (JSONObject c:store.history()) {
            try {
                String key=CandidateStore.key(c); JSONObject plan=c.optJSONObject("plan");
                String summary=plan==null?"No packaged checklist":ValidationPlan.summary(plan,store.results(key));
                button(c.optString("title")+"\n"+shortSha(c)+" · "+summary,v->openLocal(key));
            } catch (Exception ignored) { }
        }
        if (store.history().isEmpty()) label("No verified candidates downloaded yet.",16);
    }
    private void openLocal(String key) {
        try {
            selected=store.load(key); selectedKey=key; preferences.edit().putString("selected-key",key).apply();
            screen="detail"; shell(selected.getString("title"));
            label("Candidate "+shortSha(selected)+"\nVerified APK: "+selected.getString("apk_sha256"),13);
            installed=label("Checking the installed Thor APK…",16);
            button("Install this candidate",v->install());
            button("Launch VCMI",v->launch());
            button("Recheck installed build",v->checkInstalled());
            JSONObject plan=selected.optJSONObject("plan");
            if (plan==null) {
                label("This older candidate has no packaged checklist. It can be installed, but this helper cannot record a complete validation for it.",16);
                button("Open this commit’s validation backlog",v->browse("https://github.com/"+selected.optString("repo")+"/blob/"+selected.optString("sha")+"/AYN_THOR_BACKLOG.md"));
            } else {
                label(plan.getString("summary"),17); label("Preparation\n"+plan.getString("preparation"),16);
                if (!plan.optString("limitations").isEmpty()) label("Known limits\n"+plan.optString("limitations"),15);
                JSONObject results=store.results(key); label(ValidationPlan.summary(plan,results),18);
                JSONArray checks=plan.getJSONArray("checks");
                for (int i=0;i<checks.length();i++) {
                    JSONObject check=checks.getJSONObject(i), r=results.optJSONObject(check.getString("id"));
                    button((i+1)+". "+check.getString("title")+"\n"+(r==null?"Not tested":r.optString("state")),v->editCheck(key,check));
                }
                button("Copy validation report",v->report(key,false)); button("Share validation report",v->report(key,true));
            }
            button("View CI build",v->browse(selected.optString("run_url"))); checkInstalled();
        } catch (Exception e) { home(); message("Could not open saved candidate: "+e.getMessage()); }
    }
    private void checkInstalled() {
        if (!screen.equals("detail") || selected==null) return;
        final String key=selectedKey,expected=selected.optString("apk_sha256");
        worker.execute(()->{
            String hash="",error="";
            try { hash=CandidateStore.installedHash(this); } catch (PackageManager.NameNotFoundException e) { error="Thor is not installed."; }
            catch (Exception e) { error="Installed APK could not be verified. Installation and validation remain unconfirmed."; }
            final String digest=hash, detail=error;
            runOnUiThread(()->{
                if (isDestroyed() || !screen.equals("detail") || !selectedKey.equals(key)) return;
                installed.setText(expected.equals(digest)?"Installed APK verified — ready to test.":!detail.isEmpty()?detail:"A different Thor APK is installed. Install this candidate to record its results.");
                if (expected.equals(digest)) preferences.edit().remove("pending-install").putString("install-result","Installed and checksum verified").apply();
            });
        });
    }
    private void install() {
        if (preparingInstall) return;
        preparingInstall=true;
        final String key=selectedKey;
        worker.execute(()->{
            try {
                JSONObject c=store.load(key);
                ValidationPlan.require(CandidateStore.digest(store.apk(key)).equals(c.getString("apk_sha256")),"Downloaded APK is damaged; installation blocked");
                runOnUiThread(()->{ preparingInstall=false; if (!isDestroyed() && screen.equals("detail") && selectedKey.equals(key)) beginInstall(key); });
            } catch (Exception e) { runOnUiThread(()->{ preparingInstall=false; if (!isDestroyed()) message(e.getMessage()); }); }
        });
    }
    private void beginInstall(String key) {
        preferences.edit().putString("pending-install",key).apply();
        try {
            if (!getPackageManager().canRequestPackageInstalls()) {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())),UNKNOWN_SOURCES); return;
            }
            Uri uri=Uri.parse("content://dev.vcmi.builds.apk/candidates/"+key+"/build.apk");
            Intent intent=new Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri).putExtra(Intent.EXTRA_RETURN_RESULT,true)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("Verified Thor APK",uri));
            startActivityForResult(intent,INSTALL);
        } catch (Exception e) { preferences.edit().remove("pending-install").apply(); message("Could not open installer: "+e.getMessage()); }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        String key=preferences.getString("pending-install","");
        if (request==UNKNOWN_SOURCES) {
            if (getPackageManager().canRequestPackageInstalls() && !key.isEmpty()) { openLocal(key); install(); }
            else { preferences.edit().remove("pending-install").apply(); message("Installation permission was not granted. You can retry when ready."); }
        } else if (request==INSTALL) {
            String outcome=result==RESULT_OK?"Installer finished; verifying installed APK.":result==RESULT_CANCELED?"Installation cancelled or not completed.":"Installation failed. Check package signature, storage and Android’s installer message.";
            preferences.edit().putString("install-result",outcome).remove("pending-install").apply();
            if (!key.isEmpty()) openLocal(key); message(outcome);
        }
    }
    private void launch() {
        if (selected==null) return;
        final String key=selectedKey, expected=selected.optString("apk_sha256");
        worker.execute(()->{
            try {
                ValidationPlan.require(expected.equals(CandidateStore.installedHash(this)),"Install this exact candidate before launching its validation session");
                runOnUiThread(()->{
                    if (isDestroyed() || !screen.equals("detail") || !selectedKey.equals(key)) return;
                    Intent launch=getPackageManager().getLaunchIntentForPackage(CandidateStore.PACKAGE);
                    if (launch==null) message("Thor’s launcher is unavailable."); else startActivity(launch);
                });
            } catch (Exception e) { runOnUiThread(()->{ if (!isDestroyed()) message("Launch not confirmed: "+e.getMessage()); }); }
        });
    }
    private void editCheck(String key,JSONObject check) {
        try {
            String id=check.getString("id"), draft="draft."+key+"."+id;
            JSONObject result=store.results(key).optJSONObject(id);
            LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(16),dp(8),dp(16),dp(8));
            TextView instructions=new TextView(this); instructions.setText(check.getString("steps")+"\n\nExpected\n"+check.getString("expected")); form.addView(instructions);
            Spinner state=new Spinner(this); state.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ValidationPlan.STATES));
            String previous=preferences.getString(draft+".state",result==null?"Not tested":result.optString("state","Not tested"));
            state.setSelection(Math.max(0,Arrays.asList(ValidationPlan.STATES).indexOf(previous))); form.addView(state);
            EditText note=new EditText(this); note.setHint("Notes / failed steps / unavailable scenario"); note.setMinLines(2); note.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4000)});
            note.setText(preferences.getString(draft+".note",result==null?"":result.optString("note"))); form.addView(note);
            note.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s,int start,int count,int after){} public void onTextChanged(CharSequence s,int start,int before,int count){ preferences.edit().putString(draft+".note",s.toString()).apply(); } public void afterTextChanged(Editable e){} });
            state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){ public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int position,long row){ preferences.edit().putString(draft+".state",ValidationPlan.STATES[position]).apply(); }});
            ScrollView scroll=new ScrollView(this); scroll.addView(form);
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle(check.getString("title")).setView(scroll).setNegativeButton("Close (keep draft)",null).setPositiveButton("Save result",null).create();
            dialog.setOnShowListener(unused->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                final String chosen=ValidationPlan.STATES[state.getSelectedItemPosition()], notes=note.getText().toString();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                worker.execute(()->{
                    try {
                        String hash=CandidateStore.installedHash(this);
                        store.saveResult(key,id,chosen,notes,hash,Build.MANUFACTURER+" "+Build.MODEL+" / Android "+Build.VERSION.RELEASE);
                        preferences.edit().remove(draft+".state").remove(draft+".note").apply();
                        runOnUiThread(()->{ if (!isDestroyed()) { dialog.dismiss(); openLocal(key); } });
                    } catch (Exception e) { runOnUiThread(()->{ if (!isDestroyed()) { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); message("Result not saved: "+e.getMessage()); } }); }
                });
            })); dialog.show();
        } catch (Exception e) { message("Could not open check: "+e.getMessage()); }
    }
    private void report(String key,boolean share) {
        try {
            String text=store.report(key);
            if (share) startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"Share validation results"));
            else { getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Thor validation",text)); Toast.makeText(this,"Report copied",Toast.LENGTH_SHORT).show(); }
        } catch (Exception e) { message("Could not create report: "+e.getMessage()); }
    }
    private EditText field(String label,String value,boolean secret) {
        label(label,15); EditText field=new EditText(this); field.setSingleLine(true); field.setText(value);
        field.setInputType(secret?InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        field.setSaveEnabled(!secret); page.addView(field); return field;
    }
    private void settings() {
        screen="settings"; shell("Settings");
        EditText repository=field("Repository",repo(),false), filter=field("Candidate branch",branch(),false);
        String saved=""; try { saved=new TokenStore(this).load(); } catch (Exception e) { label("Saved token could not be unlocked. Forget it and save a replacement.",15); }
        EditText token=field("GitHub token",saved,true); token.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        token.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        label("Encrypted on this device. Candidate downloads need Actions: read. Expired or revoked tokens must be replaced.",14);
        button("Save settings & token",v->{
            try {
                ValidationPlan.require(!downloading(),"Wait for the active download or cancel it first");
                String configured=GitHubClient.repository(repository.getText().toString()), configuredBranch=filter.getText().toString().trim();
                ValidationPlan.require(!configuredBranch.isEmpty() && configuredBranch.length()<=200,"Enter a candidate branch");
                new TokenStore(this).save(token.getText().toString().trim());
                preferences.edit().putString("repo",configured).putString("branch",configuredBranch).apply(); home();
            } catch (Exception e) { message("Settings not saved: "+e.getMessage()); }
        });
        button("Forget token",v->{
            if (downloading()) { message("Cancel the active download before forgetting its token."); return; }
            token.setText(""); try { new TokenStore(this).clear(); message("Token forgotten on this device. It has not been revoked on GitHub."); } catch (Exception e) { message("Could not remove saved token. Please retry."); }
        });
        button("Advanced downloads / GitHub Releases",v->startActivity(new Intent(this,AdvancedDownloadsActivity.class)));
        label("Advanced downloads do not produce verified Thor validation sessions.",14);
    }
    private void browse(String url) {
        if (url.startsWith("https://github.com/")) startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
    }
    @Override protected void onResume() { super.onResume(); handler.post(ticker); checkInstalled(); }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }
    @Override public void onBackPressed() { if (!screen.equals("home")) home(); else super.onBackPressed(); }
    @Override protected void onDestroy() { handler.removeCallbacks(ticker); worker.shutdownNow(); super.onDestroy(); }
}
