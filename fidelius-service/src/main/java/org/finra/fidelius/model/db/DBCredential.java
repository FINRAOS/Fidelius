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

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DynamoDBTable(tableName = "")
public class DBCredential {

    private String name;
    private String version;
    private String updatedBy;
    private String updatedDate;
    private String sdlc;
    private String component;

    @DynamoDBHashKey(attributeName = "name")
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @DynamoDBRangeKey(attributeName = "version")
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }

    @DynamoDBAttribute(attributeName = "updatedBy")
    public String getUpdatedBy() {
        return updatedBy;
    }
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @DynamoDBAttribute(attributeName = "updatedOn")
    public String getUpdatedDate() {
        return updatedDate;
    }
    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    @DynamoDBAttribute(attributeName = "sdlc")
    public String getSdlc(){
        if (sdlc != null && !sdlc.isEmpty()) {
            return sdlc;
        } else {
            return null;
        }
    }
    public void setSdlc(String sdlc){ this.sdlc = sdlc;}

    @DynamoDBAttribute(attributeName = "component")
    public String getComponent() {
        if (component != null && !component.isEmpty()) {
            return component;
        } else {
            return null;
        }
    }
    public void setComponent(String component){ this.component = component;}

    @DynamoDBIgnore
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

    @DynamoDBIgnore
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
