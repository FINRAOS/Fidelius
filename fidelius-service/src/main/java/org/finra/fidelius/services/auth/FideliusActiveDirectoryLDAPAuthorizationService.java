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

import org.finra.fidelius.authfilter.parser.IFideliusUserProfile;
import org.finra.fidelius.services.user.model.FideliusUserEntry;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.query.SearchScope;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class FideliusActiveDirectoryLDAPAuthorizationService extends FideliusOpenLDAPAuthorizationService {
    //active directory specific filter for finding nested group membership
    private static final String LDAP_MATCHING_RULE_IN_CHAIN = "1.2.840.113556.1.4.1941";

    public FideliusActiveDirectoryLDAPAuthorizationService(LdapTemplate ldapTemplate,
                                                           Supplier<IFideliusUserProfile> fideliusUserProfileSupplier,
                                                           FideliusAuthProperties fideliusAuthProperties) {
        super(ldapTemplate, fideliusUserProfileSupplier, fideliusAuthProperties);
    }

    @Override
    protected Set<String> loadUserMemberships(String userName) {
        {
            Optional<FideliusUserEntry> user = userCache.getUnchecked(userName);
            String userDn = user.get().getDn();

            LdapQuery memberOfApplication = LdapQueryBuilder.query()
                    .base(ldapUserGroupsBase)
                    .searchScope(SearchScope.SUBTREE)
                    .attributes(ldapUserCn, ldapUserDn)
                    .filter("(member:" + LDAP_MATCHING_RULE_IN_CHAIN + ":=" + userDn + ")");

            List<String> userMemberships = ldapTemplate.search(memberOfApplication, getStringAttributesMapper(ldapUserCn));

            //If no memberships are found in the primary location, check the alternative base.
            if(ldapUserGroupsAlternativeBase != null && !ldapUserGroupsAlternativeBase.isEmpty()) {
                LdapQuery memberOfApplicationAlternateLocation = LdapQueryBuilder.query()
                        .base(ldapUserGroupsAlternativeBase)
                        .searchScope(SearchScope.SUBTREE)
                        .attributes(ldapUserCn, ldapUserDn)
                        .filter("(member:" + LDAP_MATCHING_RULE_IN_CHAIN + ":=" + userDn + ")");

                userMemberships = ldapTemplate.search(memberOfApplicationAlternateLocation, getStringAttributesMapper(ldapUserCn));
            }

            return new HashSet<>(userMemberships);
        }
    }

    protected Set<String> loadOpsMemberships(String userName) {
        {
            Optional<FideliusUserEntry> user = userCache.getUnchecked(userName);
            String userDn = user.get().getDn();

            LdapQuery memberOfApplication = LdapQueryBuilder.query()
                    .base(ldapProperties.getGroupsBase())
                    .searchScope(SearchScope.SUBTREE)
                    .attributes(ldapUserCn, ldapUserDn)
                    .filter("(member:" + LDAP_MATCHING_RULE_IN_CHAIN + ":=" + userDn + ")");

            List<String> userMemberships = ldapTemplate.search(memberOfApplication, getStringAttributesMapper(ldapUserCn));

            //If no memberships are found in the primary location, check the alternative base.
            if(ldapUserGroupsAlternativeBase != null && !ldapUserGroupsAlternativeBase.isEmpty() && (userMemberships == null || userMemberships.isEmpty())) {
                LdapQuery memberOfApplicationAlternateLocation = LdapQueryBuilder.query()
                        .base(ldapProperties.getAlternativeUsersBase())
                        .searchScope(SearchScope.SUBTREE)
                        .attributes(ldapUserCn, ldapUserDn)
                        .filter("(member:" + LDAP_MATCHING_RULE_IN_CHAIN + ":=" + userDn + ")");

                userMemberships = ldapTemplate.search(memberOfApplicationAlternateLocation, getStringAttributesMapper(ldapUserCn));
            }

            return new HashSet<>(userMemberships);
        }
    }

    protected Set<String> loadMasterMemberships(String userName) {
        {
            Optional<FideliusUserEntry> user = userCache.getUnchecked(userName);
            String userDn = user.get().getDn();

            LdapQuery memberOfApplication = LdapQueryBuilder.query()
                    .base(ldapProperties.getGroupsBase())
                    .searchScope(SearchScope.SUBTREE)
                    .attributes(ldapUserCn, ldapUserDn)
                    .filter("(member:" + LDAP_MATCHING_RULE_IN_CHAIN + ":=" + userDn + ")");

            List<String> userMemberships = ldapTemplate.search(memberOfApplication, getStringAttributesMapper(ldapUserCn));

            //If no memberships are found in the primary location, check the alternative base.
            if(ldapUserGroupsAlternativeBase != null && !ldapUserGroupsAlternativeBase.isEmpty() && (userMemberships == null || userMemberships.isEmpty())) {
                LdapQuery memberOfApplicationAlternateLocation = LdapQueryBuilder.query()
                        .base(ldapProperties.getAlternativeUsersBase())
                        .searchScope(SearchScope.SUBTREE)
                        .attributes(ldapUserCn, ldapUserDn)
                        .filter("(member:" + LDAP_MATCHING_RULE_IN_CHAIN + ":=" + userDn + ")");

                userMemberships = ldapTemplate.search(memberOfApplicationAlternateLocation, getStringAttributesMapper(ldapUserCn));
            }

            return new HashSet<>(userMemberships);
        }
    }

}
