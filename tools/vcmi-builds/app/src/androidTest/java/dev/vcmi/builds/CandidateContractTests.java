package dev.vcmi.builds;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

/** Device contract tests use isolated files, synthetic plans and no user's token/results. */
final class CandidateContractTests {
    interface Operation { void run() throws Exception; }
    static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
    static void rejects(Operation operation,String name) throws Exception {
        boolean failed=false; try { operation.run(); } catch (Exception expected) { failed=true; }
        check(failed,name+" must reject");
    }
    static JSONObject plan() throws Exception {
        return new JSONObject().put("schema_version",1).put("feature_id","contract-test").put("title","Contract test")
            .put("summary","Synthetic plan").put("preparation","Synthetic setup").put("checks",new JSONArray()
            .put(new JSONObject().put("id","first").put("title","First check").put("steps","Do this").put("expected","Expect that")));
    }
    static String hash(byte[] data) throws Exception { return CandidateStore.hex(java.security.MessageDigest.getInstance("SHA-256").digest(data)); }
    static void entry(ZipOutputStream zip,String name,byte[] data) throws Exception { zip.putNextEntry(new ZipEntry(name)); zip.write(data); zip.closeEntry(); }
    static JSONObject fixture(File archive,String failure,boolean withPlan) throws Exception {
        byte[] bytes="synthetic APK bytes".getBytes(StandardCharsets.UTF_8); String apkHash=hash(bytes), sha="a".repeat(40);
        JSONObject selected=new JSONObject().put("repo","example/repo").put("sha",sha).put("run_id","123")
            .put("branch","ci/thor-candidate-validation").put("artifact_name","thor-candidate-arm64-123").put("title","Test candidate").put("run_url","https://github.com/example/repo/actions/runs/123");
        JSONObject receipt=new JSONObject().put("candidate_commit",failure.equals("commit")?"b".repeat(40):sha)
            .put("github_run_id","123").put("artifact_name","thor-candidate-arm64-123").put("git_ref","refs/heads/ci/thor-candidate-validation")
            .put("package_id",failure.equals("package")?"wrong.package":CandidateStore.PACKAGE).put("apk_sha256",apkHash)
            .put("android_abi","arm64-v8a").put("package_verification","passed")
            .put("tests",new JSONObject().put("native",failure.equals("ci")?"failed":"passed"));
        JSONObject plan=plan().put("candidate_commit",failure.equals("plan-commit")?"b".repeat(40):sha).put("apk_sha256",apkHash);
        if (failure.equals("duplicate-check")) plan.getJSONArray("checks").put(plan.getJSONArray("checks").getJSONObject(0));
        byte[] planBytes=plan.toString().getBytes(StandardCharsets.UTF_8);
        if (withPlan) receipt.put("validation_plan_sha256",failure.equals("plan-hash")?"f".repeat(64):hash(planBytes));
        try (ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(archive))) {
            entry(zip,"thor-candidate-arm64.apk",failure.equals("apk")?"modified".getBytes(StandardCharsets.UTF_8):bytes);
            entry(zip,"thor-candidate-arm64.apk.sha256",((failure.equals("checksum")?"f".repeat(64):apkHash)+"  thor-candidate-arm64.apk\n").getBytes(StandardCharsets.UTF_8));
            entry(zip,"thor-validation-receipt.json",receipt.toString().getBytes(StandardCharsets.UTF_8));
            if (withPlan) entry(zip,"thor-validation-plan.json",planBytes);
        }
        selected.put("digest","sha256:"+(failure.equals("archive")?"f".repeat(64):CandidateStore.digest(archive))); return selected;
    }
    static void run(Context context) throws Exception {
        File root=new File(context.getCacheDir(),"candidate-contract-tests-"+System.nanoTime()); root.mkdirs();
        try {
            File zip=new File(root,"fixture.zip"),apk=new File(root,"fixture.apk");
            JSONObject selected=fixture(zip,"",true), candidate=CandidateBundle.extract(zip,apk,selected);
            check(candidate.has("plan"),"Packaged plan restored");
            check(CandidateStore.digest(apk).equals(candidate.getString("apk_sha256")),"Extracted hash");
            for (String failure:new String[]{"archive","apk","checksum","commit","package","ci","plan-hash","plan-commit","duplicate-check"}) {
                JSONObject invalid=fixture(zip,failure,true); rejects(()->CandidateBundle.extract(zip,apk,invalid),failure);
            }
            JSONObject legacy=CandidateBundle.extract(zip,apk,fixture(zip,"",false)); check(!legacy.has("plan"),"Legacy artifact must not gain invented checks");
            rejects(()->CandidateBundle.verifyPackage(context,new File(context.getApplicationInfo().sourceDir)),"Wrong real APK package");
            CandidateStore store=new CandidateStore(new File(root,"saved")); String key=CandidateStore.key(candidate);
            CandidateStore.write(new File(store.directory(key),"candidate.json"),candidate);
            check(store.results(key).length()==0,"Fresh results");
            rejects(()->store.saveResult(key,"first","Pass","","f".repeat(64),"test device"),"Wrong installed APK");
            store.saveResult(key,"first","Blocked","Scenario unavailable",candidate.getString("apk_sha256"),"test device");
            CandidateStore restored=new CandidateStore(new File(root,"saved"));
            check(restored.results(key).getJSONObject("first").getString("state").equals("Blocked"),"Persist results across instances");
            check(restored.report(key).contains("Scenario unavailable") && restored.report(key).contains(candidate.getString("apk_sha256")),"Report includes identity and notes");
            check(!ValidationPlan.summary(candidate.getJSONObject("plan"),restored.results(key)).contains("All checks reported passed"),"Blocked is not passed");
            store.saveResult(key,"first","Pass","Retested",candidate.getString("apk_sha256"),"test device");
            check(ValidationPlan.summary(candidate.getJSONObject("plan"),store.results(key)).contains("All checks reported passed"),"Pass summary");
            JSONObject other=new JSONObject(candidate.toString()).put("sha","c".repeat(40));
            check(!CandidateStore.key(other).equals(key) && store.results(CandidateStore.key(other)).length()==0,"Results never carry to another candidate");
            rejects(()->store.saveResult(key,"unknown","Pass","",candidate.getString("apk_sha256"),"test"),"Unknown test");
            rejects(()->store.directory("../outside"),"Local path traversal");
            String installed=CandidateStore.installedHash(context);
            check(installed.matches("[a-f0-9]{64}"),"Actual installed Thor APK is readable and hashable");
        } finally { remove(root); }
    }
    static void remove(File file) { File[] children=file.listFiles(); if (children!=null) for (File child:children) remove(child); file.delete(); }
}
