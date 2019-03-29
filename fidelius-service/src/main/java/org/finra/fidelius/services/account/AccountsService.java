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

package org.finra.fidelius.services.account;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.finra.fidelius.model.account.Account;
import org.finra.fidelius.services.rest.RESTService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AccountsService {

    @Inject
    private RESTService restService;

    @Value("${fidelius.account-server-url}")
    private String accountServerURL;

    @Value("${fidelius.account-server-uri}")
    private String accountServerURI;

    /*
     * Cache for storing accounts. Alleviates excessive calls to account info server.
     */
    private final LoadingCache<String, List<Account>> accountCache = CacheBuilder.newBuilder()
            .maximumSize(1)
            .concurrencyLevel(10)
            .refreshAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<String, List<Account>>() {
                @Override
                public List<Account> load(String account) throws Exception {
                    return loadAccounts();
                }
            });


    private List<Account> loadAccounts() {
        return Arrays.asList(restService.makeCall(accountServerURL, accountServerURI, Account[].class));
    }

    public List<Account> getAccounts() {
        return accountCache.getUnchecked(accountServerURI);
    }

    public Account getAccountByAlias(String alias){
        List<Account> result =  getAccounts()
                .stream()
                .filter(account -> account.getAlias().equalsIgnoreCase(alias))
                .collect(Collectors.toList());

        return !result.isEmpty() ? result.get(0) : null;
    }
}
