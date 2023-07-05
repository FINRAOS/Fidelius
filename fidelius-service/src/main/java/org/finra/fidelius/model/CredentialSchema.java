package org.finra.fidelius.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@DynamoDbBean
public class CredentialSchema {
    private String name;
    private String version;
    private String component;
    private String contents;
    private String hmac;
    private String key;
    private String sdlc;
    private String source;
    private String sourceType;
    private String updatedBy;
    private String updatedOn;

    @DynamoDbPartitionKey
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDbSortKey
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public String getHmac() {
        return hmac;
    }

    public void setHmac(String hmac) {
        this.hmac = hmac;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSdlc() {
        return sdlc;
    }

    public void setSdlc(String sdlc) {
        this.sdlc = sdlc;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
    }

    public Map<String, AttributeValue> getMapOfAttributeValues() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("name", AttributeValue.builder().s(this.name).build());
        map.put("version", AttributeValue.builder().s(this.version).build());
        map.put("component", AttributeValue.builder().s(this.component).build());
        map.put("hmac", AttributeValue.builder().s(this.hmac).build());
        map.put("key", AttributeValue.builder().s(this.key).build());
        map.put("sdlc", AttributeValue.builder().s(this.sdlc).build());
        map.put("source", AttributeValue.builder().s(this.source).build());
        map.put("sourceType", AttributeValue.builder().s(this.sourceType).build());
        map.put("updatedBy", AttributeValue.builder().s(this.updatedBy).build());
        map.put("updatedOn", AttributeValue.builder().s(this.updatedOn).build());
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CredentialSchema that = (CredentialSchema) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(component, that.component) &&
                Objects.equals(contents, that.contents) &&
                Objects.equals(hmac, that.hmac) &&
                Objects.equals(key, that.key) &&
                Objects.equals(sdlc, that.sdlc) &&
                Objects.equals(source, that.source) &&
                Objects.equals(sourceType, that.sourceType) &&
                Objects.equals(updatedBy, that.updatedBy) &&
                Objects.equals(updatedOn, that.updatedOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, component, contents, hmac, key, sdlc, source, sourceType, updatedBy, updatedOn);
    }

    @Override
    public String toString() {
        return "CredentialSchema{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", component='" + component + '\'' +
                ", contents='" + contents + '\'' +
                ", hmac='" + hmac + '\'' +
                ", key='" + key + '\'' +
                ", sdlc='" + sdlc + '\'' +
                ", source='" + source + '\'' +
                ", sourceType='" + sourceType + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", updatedOn='" + updatedOn + '\'' +
                '}';
    }
}
