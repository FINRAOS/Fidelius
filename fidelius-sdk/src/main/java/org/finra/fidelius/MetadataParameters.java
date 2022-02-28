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