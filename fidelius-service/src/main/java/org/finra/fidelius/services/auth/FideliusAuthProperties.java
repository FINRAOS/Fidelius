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

package org.finra.fidelius.services.auth;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="fidelius.auth")
@EnableAutoConfiguration
public class FideliusAuthProperties {
    /**
     * The header to look for the user's ID (authentication mechanism should inject this header)
     */
    private String userIdHeader;

    /**
     * The header containing the authenticated users email
     */
    private String userEmailHeader;

    /**
     * The header containing the authenticated users full name
     */
    private String userFullNameHeader;

    /**
     * The header to look for the user's LDAP groups (should be supplied by SSO mechanism
     */
    private String userMembershipsHeader;

    /**
     * This is a regex to extract user memberships from the header
     */
    private String userMembershipsPattern;

    /**
     * Ldap Specific settings go here
     */
    private FideliusLdapProperties ldap;

    /**
     * A regular expression to capture the groups the master team member is under.
     *
     * You need to provide an area to capture (example: GROUP_([a-zA-Z]+)_MASTER)
     */

    private String masterGroupsPattern;

    /**
     * A regular expression to capture the groups the ops team member is under.
     *
     * You need to provide an area to capture (example: GROUP_([a-zA-Z]+)_OPERATIONS)
     */

    private String opsGroupsPattern;

    /**
     * A regular expression to capture the groups the dev team member is under.
     *
     * You need to provide an area to capture (example: GROUP_([a-zA-Z]+)_DEVELOPER)
     */
    private String devGroupsPattern;

    public String getUserIdHeader() {
        return userIdHeader;
    }

    public FideliusAuthProperties setUserIdHeader(String userIdHeader) {
        this.userIdHeader = userIdHeader;
        return this;
    }

    public FideliusLdapProperties getLdap() {
        return ldap;
    }

    public FideliusAuthProperties setLdap(FideliusLdapProperties ldap) {
        this.ldap = ldap;
        return this;
    }

    public String getUserEmailHeader() {
        return userEmailHeader;
    }

    public FideliusAuthProperties setUserEmailHeader(String userEmailHeader) {
        this.userEmailHeader = userEmailHeader;
        return this;
    }

    public String getUserFullNameHeader() {
        return userFullNameHeader;
    }

    public FideliusAuthProperties setUserFullNameHeader(String userFullNameHeader) {
        this.userFullNameHeader = userFullNameHeader;
        return this;
    }

    public String getUserMembershipsHeader() {
        return userMembershipsHeader;
    }

    public FideliusAuthProperties setUserMembershipsHeader(String userMembershipsHeader) {
        this.userMembershipsHeader = userMembershipsHeader;
        return this;
    }

    public String getUserMembershipsPattern() {
        return userMembershipsPattern;
    }

    public FideliusAuthProperties setUserMembershipsPattern(String userMembershipsPattern) {
        this.userMembershipsPattern = userMembershipsPattern;
        return this;
    }

    public String getMasterGroupsPattern() {
        return masterGroupsPattern;
    }

    public FideliusAuthProperties setMasterGroupsPattern(String masterGroupsPattern) {
        this.masterGroupsPattern = masterGroupsPattern;
        return this;
    }

    public String getOpsGroupsPattern() {
        return opsGroupsPattern;
    }

    public FideliusAuthProperties setOpsGroupsPattern(String opsGroupsPattern) {
        this.opsGroupsPattern = opsGroupsPattern;
        return this;
    }

    public String getDevGroupsPattern() {
        return devGroupsPattern;
    }

    public FideliusAuthProperties setDevGroupsPattern(String devGroupsPattern) {
        this.devGroupsPattern = devGroupsPattern;
        return this;
    }

    public static class FideliusLdapProperties {
        /**
         * Is this LDAP configuration Active Directory-based?
         */
        private Boolean isActiveDirectory;

        /**
         * The ObjectClass value to in which to search will pull from
         */
        private String objectClass;
        /**
         * The regex pattern at which to capture the name of a particular group
         */
        private String pattern;

        /**
         * Alternative base where the membership groups are defined. Ignored if not set.
         */
        private String alternativeGroupsBase;

        /**
         * The base where all the membership groups are defined
         */
        private String groupsBase;

        /**
         * If AWS enabled groups are different from the base groups then set this, otherwise the groupsBase will be used.
         */
        private String awsGroupsBase;

        /**
         * The base location for ldap
         */
        private String base;

        /**
         * The base where the system will search for users
         */
        private String usersBase;

        /**
         * Alternative base where the system will search for users
         */
        private String alternativeUsersBase;

        /**
         * The base where to search for test users (could be the same as the users base, but sometimes it could be different)
         */
        private String testUsersBase;

        /**
         * The LDAP Attribute for the user's cn
         */
        private String usersCnAttribute;

        /**
         * The LDAP Attribute to get the user's login id
         */

        private String usersIdAttribute;

        /**
         * The LDAP Attribute to get the user's name
         */
        private String usersNameAttribute;

        /**
         * The LDAP Attribute to get the user's email
         */
        private String usersEmailAttribute;

        /**
         * The Distinguished Name for the user
         */
        private String usersDnAttribute;

        public Boolean getIsActiveDirectory() {
            return isActiveDirectory;
        }

        public FideliusLdapProperties setIsActiveDirectory(Boolean activeDirectory) {
            isActiveDirectory = activeDirectory;
            return this;
        }

        public String getPattern() {
            return pattern;
        }

        public FideliusLdapProperties setPattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public String getObjectClass() {
            return objectClass;
        }

        public FideliusLdapProperties setObjectClass(String objectClass) {
            this.objectClass = objectClass;
            return this;
        }

        public String getGroupsBase() {
            return groupsBase;
        }

        public FideliusLdapProperties setGroupsBase(String groupsBase) {
            this.groupsBase = groupsBase;
            return this;
        }

        public String getAlternativeGroupsBase() {
            return alternativeGroupsBase;
        }

        public FideliusLdapProperties setAlternativeGroupsBase(String alternativeGroupsBase) {
            this.alternativeGroupsBase = alternativeGroupsBase;
            return this;
        }

        public String getAwsGroupsBase() {
            return awsGroupsBase;
        }

        public FideliusLdapProperties setAwsGroupsBase(String awsGroupsBase) {
            this.awsGroupsBase = awsGroupsBase;
            return this;
        }

        public String getBase() {
            return base;
        }

        public FideliusLdapProperties setBase(String base) {
            this.base = base;
            return this;
        }

        public String getAlternativeUsersBase() {
            return alternativeUsersBase;
        }

        public FideliusLdapProperties setAlternativeUsersBase(String alternativeUsersBase) {
            this.alternativeUsersBase = alternativeUsersBase;
            return this;
        }

        public String getUsersBase() {
            return usersBase;
        }

        public FideliusLdapProperties setUsersBase(String usersBase) {
            this.usersBase = usersBase;
            return this;
        }

        public String getTestUsersBase() {
            return testUsersBase;
        }

        public FideliusLdapProperties setTestUsersBase(String testUsersBase) {
            this.testUsersBase = testUsersBase;
            return this;
        }

        public String getUsersCnAttribute() {
            return usersCnAttribute;
        }

        public FideliusLdapProperties setUsersCnAttribute(String usersCnAttribute) {
            this.usersCnAttribute = usersCnAttribute;
            return this;
        }

        public String getUsersIdAttribute() {
            return usersIdAttribute;
        }

        public FideliusLdapProperties setUsersIdAttribute(String usersIdAttribute) {
            this.usersIdAttribute = usersIdAttribute;
            return this;
        }

        public String getUsersNameAttribute() {
            return usersNameAttribute;
        }

        public FideliusLdapProperties setUsersNameAttribute(String usersNameAttribute) {
            this.usersNameAttribute = usersNameAttribute;
            return this;
        }

        public String getUsersEmailAttribute() {
            return usersEmailAttribute;
        }

        public FideliusLdapProperties setUsersEmailAttribute(String usersEmailAttribute) {
            this.usersEmailAttribute = usersEmailAttribute;
            return this;
        }

        public String getUsersDnAttribute() {
            return usersDnAttribute;
        }

        public FideliusLdapProperties setUsersDnAttribute(String usersDnAttribute) {
            this.usersDnAttribute = usersDnAttribute;
            return this;
        }
    }
}
