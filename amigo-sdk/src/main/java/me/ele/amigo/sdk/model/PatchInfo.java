package me.ele.amigo.sdk.model;


import org.json.JSONObject;

public class PatchInfo {

    /**
     * @return null if input is illegal
     */
    public static PatchInfo fromJson(String json) {
        try {
            PatchInfo patchInfo = new PatchInfo();
            JSONObject topObject = new JSONObject(json);
            patchInfo.hasPatch = topObject.getBoolean("has_patch");
            JSONObject patchObject = topObject.getJSONObject("patch");
            if (patchObject != null) {
                Patch patch = new Patch();
                patch.apkUrl = patchObject.getString("apk_url");
                patch.md5 = patchObject.getString("md5");
                int workPatternI = patchObject.getInt("work_pattern");
                if (workPatternI == 0) {
                    patch.workPattern = WorkPattern.WORK_LATER;
                } else if (workPatternI == 1) {
                    patch.workPattern = WorkPattern.WORK_NOW;
                }
                patchInfo.patch = patch;
            }
            return patchInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean hasPatch;
    private Patch patch;

    public boolean hasPatch() {
        return hasPatch;
    }

    public String apkUrl() {
        return patch == null ? "" : patch.apkUrl;
    }

    public String md5() {
        return patch == null ? "" : patch.md5;
    }

    public WorkPattern workPattern() {
        return patch.workPattern;
    }

    public static class Patch {
        private String apkUrl;
        private String md5;
        private WorkPattern workPattern;
    }

    public enum WorkPattern {
        WORK_LATER, WORK_NOW
    }
}
