package dev.vcmi.builds;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class GitHubClient {
    static String repository(String raw) {
        String value=raw.trim().replaceFirst("^https://github\\.com/","").replaceFirst("/+$","").replaceFirst("\\.git$","");
        if (!value.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Use owner/repository or its GitHub URL.");
        return value;
    }
    static HttpURLConnection connect(String address,String auth,String accept) throws IOException {
        URL url=new URL(address);
        for (int redirects=0;redirects<8;redirects++) {
            if (!"https".equals(url.getProtocol())) throw new IOException("Only HTTPS downloads are supported");
            HttpURLConnection c=(HttpURLConnection)url.openConnection(); c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(20000); c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent","VCMI-Builds/0.3"); c.setRequestProperty("Accept",accept);
            if ("api.github.com".equals(url.getHost()) && !auth.isEmpty()) c.setRequestProperty("Authorization","Bearer "+auth);
            int code=c.getResponseCode();
            if (code>=300 && code<400) {
                String location=c.getHeaderField("Location"); c.disconnect();
                if (location==null) throw new IOException("Missing download redirect"); url=new URL(url,location); continue;
            }
            if (code<200 || code>=300) {
                c.disconnect(); throw new IOException("GitHub HTTP "+code+(code==401||code==403?" — check your token, Actions read permission, or rate limit":code==404||code==410?" — repository/build unavailable or expired":""));
            }
            return c;
        }
        throw new IOException("Too many redirects");
    }
    static JSONObject json(String address,String auth) throws Exception {
        HttpURLConnection c=connect(address,auth,"application/vnd.github+json");
        try (InputStream in=c.getInputStream()) { return new JSONObject(new String(CandidateStore.readBytes(in,4*1024*1024),StandardCharsets.UTF_8)); }
        finally { c.disconnect(); }
    }
    static List<JSONObject> list(String repo,String branch,String auth) throws Exception {
        String base="https://api.github.com/repos/"+repository(repo);
        JSONArray runs=json(base+"/actions/workflows/thor-ci.yml/runs?per_page=20&branch="+URLEncoder.encode(branch,"UTF-8"),auth).getJSONArray("workflow_runs");
        List<JSONObject> result=new ArrayList<>();
        for (int i=0;i<runs.length();i++) {
            JSONObject run=runs.getJSONObject(i);
            if (!run.getString("head_branch").equals(branch)) continue;
            JSONObject c=new JSONObject().put("repo",repo).put("branch",branch).put("sha",run.getString("head_sha"))
                .put("run_id",Long.toString(run.getLong("id"))).put("run_url",run.getString("html_url"))
                .put("title",run.optString("display_title",run.optString("name"))).put("created_at",run.getString("created_at"))
                .put("state",run.getString("status").equals("completed")?run.optString("conclusion","unknown"):run.getString("status"));
            if ("success".equals(c.getString("state"))) {
                JSONArray artifacts=json(base+"/actions/runs/"+c.getString("run_id")+"/artifacts?per_page=100",auth).getJSONArray("artifacts");
                c.put("availability","No candidate APK — preflight only, expired, or unavailable");
                for (int a=0;a<artifacts.length();a++) {
                    JSONObject artifact=artifacts.getJSONObject(a);
                    if (!artifact.getString("name").equals("thor-candidate-arm64-"+c.getString("run_id"))) continue;
                    c.put("availability",artifact.optBoolean("expired")?"Artifact expired":"Ready to download");
                    if (artifact.optBoolean("expired")) continue;
                    c.put("artifact_id",Long.toString(artifact.getLong("id"))).put("artifact_name",artifact.getString("name"))
                        .put("digest",artifact.optString("digest","")).put("size",artifact.getLong("size_in_bytes"));
                }
            }
            result.add(c);
        }
        return result;
    }
}
