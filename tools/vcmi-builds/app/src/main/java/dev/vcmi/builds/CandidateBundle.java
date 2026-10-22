package dev.vcmi.builds;

import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Verifies the selected Actions artifact before any installable bytes become visible. */
final class CandidateBundle {
    static final long MAX_APK=1024L*1024*1024;
    static void verifyPackage(android.content.Context context,File apk) throws Exception {
        android.content.pm.PackageInfo info=context.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),0);
        ValidationPlan.require(info!=null && CandidateStore.PACKAGE.equals(info.packageName),"APK is not the Thor package");
    }
    static String text(ZipFile zip, String name) throws Exception {
        ZipEntry entry=zip.getEntry(name); ValidationPlan.require(entry!=null && !entry.isDirectory(),"Missing "+name);
        try (InputStream in=zip.getInputStream(entry)) { return new String(CandidateStore.readBytes(in,256*1024),StandardCharsets.UTF_8); }
    }
    static JSONObject extract(File archive, File apk, JSONObject selected) throws Exception {
        String archiveHash=selected.optString("digest","");
        ValidationPlan.require(archiveHash.matches("sha256:[a-f0-9]{64}"),"GitHub artifact checksum is unavailable");
        ValidationPlan.require(archiveHash.substring(7).equals(CandidateStore.digest(archive)),"Artifact checksum mismatch");
        try (ZipFile zip=new ZipFile(archive)) {
            Set<String> names=new HashSet<>(); Enumeration<? extends ZipEntry> entries=zip.entries(); int count=0;
            while (entries.hasMoreElements()) {
                ZipEntry entry=entries.nextElement();
                ValidationPlan.require(++count<=32 && names.add(entry.getName()),"Invalid or oversized artifact file list");
            }
            JSONObject receipt=new JSONObject(text(zip,"thor-validation-receipt.json"));
            String hash=receipt.getString("apk_sha256");
            ValidationPlan.require(hash.matches("[a-f0-9]{64}"),"Invalid APK checksum");
            ValidationPlan.require(receipt.getString("candidate_commit").equals(selected.getString("sha")) &&
                receipt.getString("github_run_id").equals(selected.getString("run_id")) &&
                receipt.getString("artifact_name").equals(selected.getString("artifact_name")) &&
                receipt.getString("git_ref").equals("refs/heads/"+selected.getString("branch")) &&
                receipt.getString("package_id").equals(CandidateStore.PACKAGE) &&
                receipt.getString("android_abi").equals("arm64-v8a") &&
                receipt.getString("package_verification").equals("passed"),"Build receipt does not match the selected candidate");
            JSONObject tests=receipt.getJSONObject("tests");
            ValidationPlan.require(tests.length()>0,"Missing CI test evidence");
            Iterator<String> testNames=tests.keys(); while (testNames.hasNext())
                ValidationPlan.require(tests.getString(testNames.next()).equals("passed"),"Candidate CI tests did not pass");
            String[] checksum=text(zip,"thor-candidate-arm64.apk.sha256").trim().split("\\s+");
            ValidationPlan.require(checksum.length==2 && checksum[0].equals(hash) && checksum[1].equals("thor-candidate-arm64.apk"),"APK checksum file disagrees with receipt");
            JSONObject result=new JSONObject(selected.toString()).put("receipt",receipt).put("apk_sha256",hash);
            if (zip.getEntry("thor-validation-plan.json")!=null) {
                String raw=text(zip,"thor-validation-plan.json");
                String planHash=CandidateStore.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
                ValidationPlan.require(planHash.equals(receipt.getString("validation_plan_sha256")),"Checklist checksum mismatch");
                JSONObject plan=new JSONObject(raw); ValidationPlan.validate(plan);
                ValidationPlan.require(plan.getString("candidate_commit").equals(selected.getString("sha")) &&
                    plan.getString("apk_sha256").equals(hash),"Checklist belongs to a different build");
                result.put("plan",plan).put("plan_sha256",planHash).put("title",plan.getString("title"));
            } else ValidationPlan.require(!receipt.has("validation_plan_sha256"),"Receipt requires a missing checklist");
            ZipEntry entry=zip.getEntry("thor-candidate-arm64.apk");
            ValidationPlan.require(entry!=null && !entry.isDirectory(),"Missing Thor APK");
            try (InputStream in=zip.getInputStream(entry); OutputStream out=new FileOutputStream(apk)) {
                byte[] buffer=new byte[65536]; int n; long total=0;
                while ((n=in.read(buffer))!=-1) {
                    ValidationPlan.require(!Thread.currentThread().isInterrupted(),"Download cancelled");
                    ValidationPlan.require((total+=n)<=MAX_APK,"APK exceeds size limit"); out.write(buffer,0,n);
                }
            }
            ValidationPlan.require(CandidateStore.digest(apk).equals(hash),"APK checksum mismatch");
            return result.put("downloaded_at",System.currentTimeMillis());
        } catch (Exception e) { apk.delete(); throw e; }
    }
}
