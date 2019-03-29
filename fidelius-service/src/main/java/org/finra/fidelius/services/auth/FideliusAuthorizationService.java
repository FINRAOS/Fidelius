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
import org.finra.fidelius.authfilter.parser.IFideliusUserProfile;
import org.finra.fidelius.services.user.model.FideliusUserEntry;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class FideliusAuthorizationService {

    final Supplier<IFideliusUserProfile> fideliusUserProfileSupplier;

    /* User Cache */
    final LoadingCache<String, Optional<FideliusUserEntry>> userCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .concurrencyLevel(10)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<String, Optional<FideliusUserEntry>>() {
                @Override
                public Optional<FideliusUserEntry> load(String userName) throws Exception {
                    return Optional.ofNullable(loadUser(userName));
                }
            });

    /* User Dev -> Application Cache */
    private final LoadingCache<String, Optional<Set<String>>> userDevMembershipCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .concurrencyLevel(10)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<Set<String>>>() {
                @Override
                public Optional<Set<String>> load(String userName) throws Exception {
                    return Optional.ofNullable(loadUserMemberships(userName));
                }
            });

    /* User Ops -> Application Cache */
    private final LoadingCache<String, Optional<Set<String>>> userOpsMembershipCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .concurrencyLevel(10)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<Set<String>>>() {
                @Override
                public Optional<Set<String>> load(String userName) throws Exception {
                    return Optional.ofNullable(loadUserMemberships(userName));
                }
            });


   /* User Master -> Application Cache */
    private final LoadingCache<String, Optional<Set<String>>> userMasterMembershipCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .concurrencyLevel(10)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<Set<String>>>() {
                @Override
                public Optional<Set<String>> load(String userName) throws Exception {
                    return Optional.ofNullable(loadUserMemberships(userName));
                }
            });

    public FideliusAuthorizationService(Supplier<IFideliusUserProfile> fideliusUserProfileSupplier) {
        this.fideliusUserProfileSupplier = fideliusUserProfileSupplier;
    }

    public Set<String> getDevMemberships(){
        return userDevMembershipCache.getUnchecked(fideliusUserProfileSupplier.get().getName()).get();
    }

    public Set<String> getOpsMemberships(){
        return userOpsMembershipCache.getUnchecked(fideliusUserProfileSupplier.get().getName()).get();
    }

    public Set<String> getMasterMemberships(){
        return userMasterMembershipCache.getUnchecked(fideliusUserProfileSupplier.get().getName()).get();
    }

    public FideliusUserEntry getUser(){
        return userCache.getUnchecked(fideliusUserProfileSupplier.get().getName()).get();
    }

    abstract FideliusUserEntry loadUser(String userName);

    abstract Set<String> loadUserMemberships(String userName);

    abstract Set<String> loadOpsMemberships(String userName);

    abstract Set<String> loadMasterMemberships(String userName);
}
