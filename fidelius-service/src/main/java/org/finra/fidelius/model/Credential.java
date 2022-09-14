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

package org.finra.fidelius.model;

import org.finra.fidelius.model.validators.IsValidActiveDirectoryPassword;
import org.hibernate.validator.constraints.NotBlank;
import org.jvnet.hk2.annotations.Optional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Date;

@IsValidActiveDirectoryPassword
public class Credential implements Comparable<Credential>{

    @Optional
    private String lastUpdatedBy;

    @Optional
    private ZonedDateTime lastUpdatedDate;

    @Pattern(regexp = "[^\\s]+")
    private String component;

    @Pattern(regexp = "[^\\s]+")
    private String longKey;

    private Boolean isActiveDirectory;

    private String sourceType;

    private String source;

    @NotBlank
    @NotNull
    private String shortKey;

    @NotBlank
    @NotNull
    private String account;

    @NotBlank
    @NotNull
    private String region;

    @NotBlank
    @NotNull
    private String environment;

    @NotBlank
    @NotNull
    private String application;

    @NotBlank
    @NotNull
    private String secret;

    public Credential(){ }

    public Credential(String shortKey, String longKey, String account, String region, String application, String environment,
                      String component, String lastUpdatedBy, String lastUpdatedDate) {
        this.shortKey = shortKey;
        this.longKey = longKey;
        this.account = account;
        this.region = region;
        this.application = application;
        this.environment = environment;
        this.component = component;
        this.lastUpdatedBy = lastUpdatedBy;
        if(lastUpdatedDate != null)
            try {
                this.lastUpdatedDate = ZonedDateTime.parse(lastUpdatedDate);
            } catch(DateTimeParseException exception) {

            }
    }

    public Credential(String shortKey, AttributeValue longKey, String account, String region, String application, AttributeValue environment,
                      AttributeValue component, String lastUpdatedBy, AttributeValue lastUpdatedDate) {
        this.shortKey = shortKey;
        this.longKey = longKey.s();
        this.account = account;
        this.region = region;
        this.application = application;
        this.environment = environment.s();
        if(component != null) {
            this.component = component.s();
        } else {
            this.component = null;
        }
        if(lastUpdatedBy != null) {
            this.lastUpdatedBy = lastUpdatedBy;
        } else {
            this.lastUpdatedBy = null;
        }
        if(lastUpdatedDate != null)
            try {
                this.lastUpdatedDate = ZonedDateTime.parse(lastUpdatedDate.s());
            } catch(DateTimeParseException exception) {

            }
    }

    public Credential(String shortKey, String longKey, String account, String region, String application, String environment,
                      String component, String lastUpdatedBy, String lastUpdatedDate, String source, String sourceType) {
        this.shortKey = shortKey;
        this.longKey = longKey;
        this.account = account;
        this.region = region;
        this.application = application;
        this.environment = environment;
        this.component = component;
        this.lastUpdatedBy = lastUpdatedBy;
        this.source = source;
        this.sourceType = sourceType;
        if(lastUpdatedDate != null)
            try {
                this.lastUpdatedDate = ZonedDateTime.parse(lastUpdatedDate);
            } catch(DateTimeParseException exception) {

            }
    }

    public Credential(String shortKey, String longKey, String account, String region, String application, String environment,
                      String component, String lastUpdatedBy, String lastUpdatedDate, String secret) {
        this.shortKey = shortKey;
        this.longKey = longKey;
        this.account = account;
        this.region = region;
        this.application = application;
        this.environment = environment;
        this.component = component;
        this.lastUpdatedBy = lastUpdatedBy;
        if(lastUpdatedDate != null)
            try {
                this.lastUpdatedDate = ZonedDateTime.parse(lastUpdatedDate);
            } catch(DateTimeParseException exception) {

            }
        this.secret = secret;
    }

    public Credential(String shortKey, String longKey, String account, String region, String application, String environment,
                      String component, String lastUpdatedBy, String lastUpdatedDate, String secret, String source, String sourceType) {
        this.shortKey = shortKey;
        this.longKey = longKey;
        this.account = account;
        this.region = region;
        this.application = application;
        this.environment = environment;
        this.component = component;
        this.lastUpdatedBy = lastUpdatedBy;
        this.source = source;
        this.sourceType = sourceType;
        if(lastUpdatedDate != null)
            try {
                this.lastUpdatedDate = ZonedDateTime.parse(lastUpdatedDate);
            } catch(DateTimeParseException exception) {

            }
        this.secret = secret;
    }

    public String getShortKey() {
        return shortKey;
    }

    public void setShortKey(String shortKey) {
        this.shortKey = shortKey;
    }

    public String getLongKey() {
        return longKey;
    }

    public void setLongKey(String longKey) {
        this.longKey = longKey;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public ZonedDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(String lastUpdatedDate) {
        this.lastUpdatedDate = ZonedDateTime.parse(lastUpdatedDate);
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Boolean getIsActiveDirectory() {
        return isActiveDirectory;
    }

    public void setIsActiveDirectory(Boolean activeDirectory) {
        isActiveDirectory = activeDirectory;
    }

    @Override
    public int compareTo(Credential o) {
        return o.getLastUpdatedDate().compareTo(getLastUpdatedDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Credential that = (Credential) o;

        if (!shortKey.equals(that.shortKey)) return false;
        if (longKey != null ? !longKey.equals(that.longKey): that.longKey != null) return false;
        if (!account.equals(that.account)) return false;
        if (!environment.equals(that.environment)) return false;
        if (!region.equals(that.region)) return false;
        if (!application.equals(that.application)) return false;
        if (component != null ? !component.equals(that.component) : that.component != null) return false;
        if (!lastUpdatedBy.equals(that.lastUpdatedBy)) return false;
        if (lastUpdatedDate != null || that.lastUpdatedDate != null)
             return lastUpdatedDate.equals(that.lastUpdatedDate);
         return false;
    }



    @Override
    public int hashCode() {
        int result = (shortKey!= null ? shortKey.hashCode(): 0);
        result = 31 * result + (longKey != null ? longKey.hashCode() : 0);
        result = 31 * result + (account != null? account.hashCode(): 0);
        result = 31 * result + (region != null? region.hashCode(): 0);
        result = 31 * result + (application != null? application.hashCode(): 0);
        result = 31 * result + (environment != null ? environment.hashCode(): 0);
        result = 31 * result + (component != null ? component.hashCode() : 0);
        result = 31 * result + (lastUpdatedBy != null ? lastUpdatedBy.hashCode() : 0);
        result = 31 * result + (lastUpdatedDate != null ? lastUpdatedDate.hashCode(): 0);
        return result;
    }

    @Override
    public String toString() {
        return "Credential{" +
                "lastUpdatedBy='" + lastUpdatedBy + '\'' +
                ", lastUpdatedDate=" + lastUpdatedDate +
                ", component='" + component + '\'' +
                ", longKey='" + longKey + '\'' +
                ", isActiveDirectory=" + isActiveDirectory +
                ", shortKey='" + shortKey + '\'' +
                ", account='" + account + '\'' +
                ", region='" + region + '\'' +
                ", environment='" + environment + '\'' +
                ", membership='" + application + '\'' +
                ", secret='" + secret + '\'' +
                '}';
    }
}
