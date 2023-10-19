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

package org.finra.fidelius.model.db;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBCredential {

    private String name;
    private String version;
    private String updatedBy;
    private String updatedDate;
    private String sdlc;
    private String component;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }
    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getSdlc(){
        if (sdlc != null && !sdlc.isEmpty()) {
            return sdlc;
        } else {
            return null;
        }
    }
    public void setSdlc(String sdlc){ this.sdlc = sdlc;}

    public String getComponent() {
        if (component != null && !component.isEmpty()) {
            return component;
        } else {
            return null;
        }
    }
    public void setComponent(String component){ this.component = component;}


    public String getShortKey() {
        if(component != null && !component.isEmpty())
            return name.split("\\."+component+"\\."+sdlc+"\\.")[1];
        else {
            Pattern p = Pattern.compile("([-\\w]+)\\.([-\\w]+)\\.(\\S+)");
            Matcher m = p.matcher(this.name);
            if(m.matches())
                return m.group(3);
            return name;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DBCredential that = (DBCredential) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(updatedBy, that.updatedBy) &&
                Objects.equals(updatedDate, that.updatedDate) &&
                Objects.equals(sdlc, that.sdlc) &&
                Objects.equals(component, that.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, updatedBy, updatedDate, sdlc, component);
    }

    @Override
    public String toString() {
        return "DBCredential{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", updatedDate='" + updatedDate + '\'' +
                ", sdlc='" + sdlc + '\'' +
                ", component='" + component + '\'' +
                '}';
    }

}
