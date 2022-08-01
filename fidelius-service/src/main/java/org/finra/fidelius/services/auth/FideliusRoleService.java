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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.finra.fidelius.exceptions.FideliusException;
import org.finra.fidelius.services.MembershipService;
import org.finra.fidelius.services.account.AccountsService;
import org.finra.fidelius.services.user.model.FideliusUserEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FideliusRoleService {

    @Inject
    private MembershipService membershipService;

    @Inject
    private AccountsService accountService;

    @Inject
    private FideliusAuthorizationService fideliusAuthorizationService;

    @Inject
    private FideliusAuthProperties fideliusAuthProperties;

    private final Logger logger = LoggerFactory.getLogger(FideliusRoleService.class);

    private static final String DEFAULT_DN = "distinguishedName";
    private static final String DEFAULT_CN = "cn";
    private static final String DEFAULT_PATTERN = "none";
    protected Pattern masterPattern;
    protected Pattern opsPattern;
    protected Pattern devPattern;

    private LoadingCache<String, Optional<List<String>>> ldapUserMasterApplicationCache = CacheBuilder.newBuilder()
            .maximumSize(1000L)
            .concurrencyLevel(10)
            .expireAfterWrite(10L, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<List<String>>>() {
                public Optional<List<String>> load(String userName) throws Exception {
                    return Optional.ofNullable(loadLdapUserMasterMemberships(userName));
                }
            });

    private LoadingCache<String, Optional<List<String>>> ldapUserOpsApplicationCache = CacheBuilder.newBuilder()
            .maximumSize(1000L)
            .concurrencyLevel(10)
            .expireAfterWrite(10L, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<List<String>>>() {
                public Optional<List<String>> load(String userName) throws Exception {
                    return Optional.ofNullable(loadLdapUserOpsMemberships(userName));
                }
            });

    private LoadingCache<String, Optional<Set<String>>> ldapUserDevApplicationCache = CacheBuilder.newBuilder()
            .maximumSize(1000L)
            .concurrencyLevel(10)
            .expireAfterWrite(10L, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<Set<String>>>() {
                public  Optional<Set<String>> load(String userName) throws Exception {
                    return Optional.ofNullable(loadLdapUserDevMemberships());
                }
            });

    @Autowired
    public FideliusRoleService(FideliusAuthorizationService fideliusAuthorizationService,
                               FideliusAuthProperties fideliusAuthProperties,
                               MembershipService membershipService,
                               AccountsService accountsService){
        this.fideliusAuthProperties = fideliusAuthProperties;
        this.fideliusAuthorizationService = fideliusAuthorizationService;
        this.masterPattern = Pattern.compile(fideliusAuthProperties.getMasterGroupsPattern());
        this.opsPattern = Pattern.compile(fideliusAuthProperties.getOpsGroupsPattern());
        this.devPattern = Pattern.compile(fideliusAuthProperties.getDevGroupsPattern());
        this.membershipService = membershipService;
        this.accountService = accountsService;
    }

    public FideliusRoleService(){}

    public FideliusUserEntry getUserProfile(){
        return fideliusAuthorizationService.getUser();
    }

    public Set<String> getDevMemberships(String requestorId){
        return ldapUserDevApplicationCache.getUnchecked(requestorId).get();
    }

    public Set<String> getDevMemberships(){
        return getDevMemberships(getUserProfile().getUserId());
    }

    public List<String> getOpsMemberships(String requestorId){
        return ldapUserOpsApplicationCache.getUnchecked(requestorId).get();
    }

    public List<String> getOpsMemberships(){
        List<String> memberships = getOpsMemberships(getUserProfile().getUserId());
        return memberships;
    }

    public List<String> getMasterMemberships(String requestorId){
        return ldapUserMasterApplicationCache.getUnchecked(requestorId).get();
    }

    public List<String> getMasterMemberships(){
        List<String> memberships = getMasterMemberships(getUserProfile().getUserId());
        return memberships;
    }

    public FideliusRole getRole(){
        return checkFideliusRole();
    }

    public boolean isAuthorized(String application, String account, String permission){
        if((getRole().equals(FideliusRole.OPS) || getRole().equals(FideliusRole.DEV) || getRole().equals(FideliusRole.MASTER)) && permission.equals("LIST_CREDENTIALS"))
            return true;

        return false;
    }

    public boolean isAuthorized(String application, String account){
        if(getRole().equals(FideliusRole.OPS) || getRole().equals(FideliusRole.MASTER))
            return true;

        try {
            if(getRole().equals(FideliusRole.DEV)) {
                String sdlc = accountService.getAccountByAlias(account).getSdlc();
                return (!sdlc.equals("prod") && loadLdapUserDevMemberships().contains(application.toUpperCase()));
            }
        } catch (Exception e) {
            logger.error("Error getting accounts", e);
            e.printStackTrace();
        }
        return false;
    }

    public boolean isAuthorizedToDelete(String application, String account){
        if(getRole().equals(FideliusRole.MASTER))
            return true;

        try {
            String sdlc = accountService.getAccountByAlias(account).getSdlc();

            if(getRole().equals(FideliusRole.OPS) && !sdlc.equals("prod")){
                return true;
            }else if(getRole().equals(FideliusRole.DEV)) {
                return (!sdlc.equals("prod") && loadLdapUserDevMemberships().contains(application.toUpperCase()));
            }
        } catch (Exception e) {
            logger.error("Error getting accounts", e);
            e.printStackTrace();
        }
        return false;
    }

    public String fetchAwsAccountId(String accountAlias) {
        return accountService.getAccountByAlias(accountAlias).getAccountId();
    }

    private List<String> loadLdapUserMasterMemberships(String userName){
        List<String> memberships = new ArrayList<>();
        fideliusAuthorizationService.getMasterMemberships(masterPattern, opsPattern).forEach((membership) -> {
            Matcher m = masterPattern.matcher(membership);
            if(m.find()) {
                try {
                    memberships.addAll(membershipService.getAllMemberships(userName));
                } catch(Exception e) {
                    logger.error("Error getting Master role memberships", e);
                    e.printStackTrace();
                }
            }
        });
        return memberships;
    }

    private List<String> loadLdapUserOpsMemberships(String userName){
        List<String> memberships = new ArrayList<>();
        fideliusAuthorizationService.getOpsMemberships(masterPattern, opsPattern).forEach((membership) -> {
            Matcher m = opsPattern.matcher(membership);
            if(m.find()) {
                try {
                    memberships.addAll(membershipService.getAllMemberships(userName));
                } catch(Exception e) {
                    logger.error("Error getting Ops role memberships", e);
                    e.printStackTrace();
                }
            }
        });
        return memberships;
    }

    private Set<String> loadLdapUserDevMemberships(){
        Set<String> memberships = new HashSet<>();
        fideliusAuthorizationService.getDevMemberships().forEach((membership) -> {
            Matcher m = devPattern.matcher(membership);
            if(m.find()) {
                String application = m.group(1).toUpperCase();
                memberships.add(application);
            }
        });
        return memberships;
    }

    private FideliusRole checkFideliusRole() {
        if(!getMasterMemberships().isEmpty()) {
            return FideliusRole.MASTER;
        } else if(!getOpsMemberships().isEmpty()) {
            return FideliusRole.OPS;
        }else if(!getDevMemberships().isEmpty()) {
            return FideliusRole.DEV;
        } else {
            return FideliusRole.UNAUTHORIZED;
        }
    }
}
