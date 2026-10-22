package dev.vcmi.builds;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

final class CandidateStore {
    static final String PACKAGE="is.xyz.vcmi.thor";
    final File root;
    CandidateStore(Context context) { root=new File(context.getFilesDir(),"candidates"); root.mkdirs(); }
    CandidateStore(File directory) { root=directory; root.mkdirs(); }
    static String digest(File file) throws Exception {
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        try (InputStream in=new FileInputStream(file)) {
            byte[] buffer=new byte[65536]; int count;
            while ((count=in.read(buffer))!=-1) hash.update(buffer,0,count);
        }
        return hex(hash.digest());
    }
    static String hex(byte[] data) {
        StringBuilder result=new StringBuilder(); for (byte b:data) result.append(String.format(Locale.ROOT,"%02x",b&255)); return result.toString();
    }
    static String key(JSONObject candidate) throws Exception {
        String identity=candidate.getString("repo")+":"+candidate.getString("sha")+":"+candidate.getString("apk_sha256");
        return hex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
    }
    File directory(String key) throws IOException {
        ValidationPlan.require(key.matches("[a-f0-9]{64}"),"Invalid local candidate identity");
        File result=new File(root,key); result.mkdirs(); return result;
    }
    File apk(String key) throws IOException { return new File(directory(key),"build.apk"); }
    static JSONObject read(File file) throws Exception {
        try (InputStream in=new AtomicFile(file).openRead()) { return new JSONObject(new String(readBytes(in,1024*1024),StandardCharsets.UTF_8)); }
    }
    static byte[] readBytes(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buffer=new byte[8192]; int n;
        while ((n=in.read(buffer))!=-1) {
            if (out.size()+n>limit) throw new IOException("Metadata exceeds its size limit");
            out.write(buffer,0,n);
        }
        return out.toByteArray();
    }
    static void write(File file, JSONObject value) throws Exception {
        AtomicFile atomic=new AtomicFile(file); FileOutputStream out=null;
        try { out=atomic.startWrite(); out.write(value.toString(2).getBytes(StandardCharsets.UTF_8)); atomic.finishWrite(out); }
        catch (Exception e) { if (out!=null) atomic.failWrite(out); throw e; }
    }
    JSONObject load(String key) throws Exception { return read(new File(directory(key),"candidate.json")); }
    JSONObject results(String key) throws Exception {
        File file=new File(directory(key),"results.json"); return file.exists()?read(file):new JSONObject();
    }
    void saveResult(String key, String checkId, String state, String note, String installedHash, String device) throws Exception {
        JSONObject candidate=load(key);
        ValidationPlan.require(candidate.getString("apk_sha256").equals(installedHash),"Install this exact candidate before recording results");
        ValidationPlan.require(Arrays.asList(ValidationPlan.STATES).contains(state) && note.length()<=4000,"Invalid result");
        JSONObject plan=candidate.getJSONObject("plan"); boolean found=false;
        JSONArray checks=plan.getJSONArray("checks");
        for (int i=0;i<checks.length();i++) if (checks.getJSONObject(i).getString("id").equals(checkId)) found=true;
        ValidationPlan.require(found,"Unknown checklist item");
        JSONObject results=results(key);
        results.put(checkId,new JSONObject().put("state",state).put("note",note).put("at",System.currentTimeMillis())
            .put("installed_sha256",installedHash).put("device",device));
        write(new File(directory(key),"results.json"),results);
    }
    List<JSONObject> history() {
        List<JSONObject> result=new ArrayList<>(); File[] dirs=root.listFiles();
        if (dirs!=null) for (File dir:dirs) if (dir.isDirectory()) {
            try { result.add(read(new File(dir,"candidate.json"))); } catch (Exception ignored) { }
        }
        result.sort((a,b)->Long.compare(b.optLong("downloaded_at"),a.optLong("downloaded_at"))); return result;
    }
    static String installedHash(Context context) throws Exception {
        ApplicationInfo info=context.getPackageManager().getApplicationInfo(PACKAGE,0);
        ValidationPlan.require(info.splitSourceDirs==null || info.splitSourceDirs.length==0,"Split installations cannot be verified by this helper");
        return digest(new File(info.sourceDir));
    }
    String report(String key) throws Exception {
        JSONObject c=load(key), results=results(key), plan=c.optJSONObject("plan");
        StringBuilder text=new StringBuilder("Thor hardware validation — user-reported results\n\n");
        text.append(c.optString("title")).append("\nRepository: ").append(c.getString("repo"))
            .append("\nCandidate: ").append(c.getString("sha")).append("\nAPK SHA-256: ").append(c.getString("apk_sha256"))
            .append("\nPackage: ").append(PACKAGE).append("\nBuild: ").append(c.optString("run_url"))
            .append("\nChecklist SHA-256: ").append(c.optString("plan_sha256","unavailable")).append("\n\n");
        if (plan==null) return text.append("No packaged checklist. Hardware validation is incomplete.\n").toString();
        text.append(ValidationPlan.summary(plan,results)).append("\n\n");
        JSONArray checks=plan.getJSONArray("checks");
        for (int i=0;i<checks.length();i++) {
            JSONObject check=checks.getJSONObject(i), r=results.optJSONObject(check.getString("id"));
            text.append(i+1).append(". ").append(check.getString("title")).append(": ").append(r==null?"Not tested":r.optString("state")).append('\n');
            if (r!=null) text.append("   Device: ").append(r.optString("device")).append("; recorded: ")
                .append(new java.util.Date(r.optLong("at"))).append("\n   Note: ").append(r.optString("note")).append('\n');
        }
        return text.append("\nNo merge or promotion is authorized by this report.\n").toString();
    }
}
