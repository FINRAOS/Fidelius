package org.finra.fidelius.model.rotate;

import org.json.simple.JSONObject;

import java.util.Objects;

public class RotateRequest {

    private String accountId;
    private String sourceType;
    private String sourceName;
    private String secret;
    private String ags;
    private String env;
    private String component;

    public RotateRequest(String accountId, String sourceType, String sourceName, String secret, String ags, String env, String component) {
        this.accountId = accountId;
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.secret = secret;
        this.ags = ags;
        this.env = env;
        this.component = component;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getAgs() {
        return ags;
    }

    public void setAgs(String ags) {
        this.ags = ags;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public JSONObject getJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("accountId", accountId);
        jsonObject.put("sourceType", sourceType);
        jsonObject.put("sourceName", sourceName);
        jsonObject.put("secret", secret);
        jsonObject.put("ags", ags);
        jsonObject.put("env", env);
        jsonObject.put("component", component);
        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RotateRequest that = (RotateRequest) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(sourceType, that.sourceType) &&
                Objects.equals(sourceName, that.sourceName) &&
                Objects.equals(secret, that.secret) &&
                Objects.equals(ags, that.ags) &&
                Objects.equals(env, that.env) &&
                Objects.equals(component, that.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, sourceType, sourceName, secret, ags, env, component);
    }
}
