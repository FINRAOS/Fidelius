/*
 * Copyright (c) 2019. Fidelius Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.finra.fidelius;

public class MetadataParameters {

    private String sourceType;
    private String source;
    private String version;
    private String fullName;
    private String updateBy;
    private String updatedOn;
    private String component;
    private String sdlc;


    public String getSourceType() {
        return sourceType;
    }

    public MetadataParameters setSourceType(String sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    public String getSource() {
        return source;
    }

    public MetadataParameters setSource(String source) {
        this.source = source;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public MetadataParameters setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public MetadataParameters setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public MetadataParameters setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
        return this;
    }
    public String getUpdateOn() {
        return updatedOn;
    }

    public MetadataParameters setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
        return this;
    }

    public String getComponent() {
        return component;
    }

    public MetadataParameters setComponent(String component) {
        this.component = component;
        return this;
    }

    public String getSdlc() {
        return sdlc;
    }

    public MetadataParameters setSdlc(String sdlc) {
        this.sdlc = sdlc;
        return this;
    }

    @Override
    public String toString() {
        return "MetadataParameters{" +
                "sourceType='" + sourceType + '\'' +
                ", source='" + source + '\'' +
                ", version='" + version + '\'' +
                ", fullName='" + fullName + '\'' +
                ", updateBy='" + updateBy + '\'' +
                ", updatedOn='" + updatedOn + '\'' +
                ", component='" + component + '\'' +
                ", sdlc='" + sdlc + '\'' +
                '}';
    }

    /*
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetadataParameters that = (MetadataParameters) o;

        if (!sourceType.equals(that.sourceType)) return false;
        if (!source.equals(that.source)) return false;
        if (!version.equals(that.version)) return false;
        if (!fullName.equals(that.fullName)) return false;
        if (updateBy != null ? !updateBy.equals(that.updateBy) : that.updateBy != null) return false;
        if (updatedOn != null ? !updatedOn.equals(that.updatedOn) : that.updatedOn != null) return false;
        if (component != null ? !component.equals(that.component) : that.component != null) return false;
        if (sdlc != null ? !sdlc.equals(that.sdlc) : that.sdlc != null) return false;
        if (!Arrays.equals(dataKeyBytes, that.dataKeyBytes)) return false;
        if (!Arrays.equals(credentialBytes, that.credentialBytes)) return false;
        return Arrays.equals(hmacBytes, that.hmacBytes);
    }
    */

    @Override
    public int hashCode() {
        int result = sourceType.hashCode();
        result = 31 * result + source.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + fullName.hashCode();
        result = 31 * result + (updateBy != null ? updateBy.hashCode() : 0);
        result = 31 * result + (updatedOn != null ? updatedOn.hashCode() : 0);
        result = 31 * result + (component != null ? component.hashCode() : 0);
        result = 31 * result + (sdlc != null ? sdlc.hashCode() : 0);
        return result;
    }
}