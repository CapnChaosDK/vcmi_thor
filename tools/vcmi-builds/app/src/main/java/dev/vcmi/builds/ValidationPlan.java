package dev.vcmi.builds;

import org.json.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Shared limits mirror CI/thor_validation_plan.py. No executable content or remote assets. */
final class ValidationPlan {
    static final String[] STATES={"Not tested","Pass","Fail","Blocked"};
    static void require(boolean valid, String message) throws IOException {
        if (!valid) throw new IOException(message);
    }
    static String string(JSONObject object, String key, int limit) throws Exception {
        Object value=object.get(key);
        require(value instanceof String,"Invalid "+key);
        String text=(String)value;
        require(!text.trim().isEmpty() && text.codePointCount(0,text.length())<=limit,"Invalid "+key);
        return text;
    }
    static void validate(JSONObject plan) throws Exception {
        require(plan.get("schema_version") instanceof Integer && plan.getInt("schema_version")==1,"Unsupported checklist version");
        require(string(plan,"feature_id",80).matches("[a-z0-9-]+"),"Invalid feature identity");
        string(plan,"title",160); string(plan,"summary",4000); string(plan,"preparation",4000);
        Object limitations=plan.opt("limitations");
        require(limitations==null || limitations instanceof String && ((String)limitations).codePointCount(0,((String)limitations).length())<=4000,"Invalid limitations");
        JSONArray checks=plan.getJSONArray("checks");
        require(checks.length()>0 && checks.length()<=50,"Checklist must contain 1–50 checks");
        Set<String> ids=new HashSet<>();
        for (int i=0;i<checks.length();i++) {
            JSONObject check=checks.getJSONObject(i);
            String id=string(check,"id",80);
            require(id.matches("[a-z0-9-]+") && ids.add(id),"Duplicate or invalid check identity");
            string(check,"title",160); string(check,"steps",4000); string(check,"expected",4000);
        }
    }
    static String summary(JSONObject plan, JSONObject results) throws Exception {
        JSONArray checks=plan.getJSONArray("checks"); int passed=0,failed=0,blocked=0;
        for (int i=0;i<checks.length();i++) {
            JSONObject result=results.optJSONObject(checks.getJSONObject(i).getString("id"));
            String state=result==null?"Not tested":result.optString("state","Not tested");
            if (state.equals("Pass")) passed++;
            if (state.equals("Fail")) failed++;
            if (state.equals("Blocked")) blocked++;
        }
        return passed+"/"+checks.length()+" passed · "+failed+" failed · "+blocked+" blocked"+
            (passed==checks.length()?"\nAll checks reported passed — not a promotion approval.":"");
    }
}
