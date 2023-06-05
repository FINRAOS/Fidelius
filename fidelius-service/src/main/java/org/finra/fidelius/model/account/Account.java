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

package org.finra.fidelius.model.account;

import java.util.List;
import java.util.Objects;

public class Account {

    private String accountId;
    private String name;
    private String sdlc;
    private String alias;
    private List<Region> regions;

    public Account() {}
    public Account(String accountId, String name, String sdlc, String alias, List<Region> regions) {
        this.accountId = accountId;
        this.name = name;
        this.sdlc = sdlc;
        this.alias = alias;
        this.regions = regions;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSdlc() {
        return sdlc;
    }

    public void setSdlc(String sdlc) {
        this.sdlc = sdlc;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public void setRegions(List<Region> regions) {
        this.regions = regions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountId, account.accountId) &&
                Objects.equals(name, account.name) &&
                Objects.equals(sdlc, account.sdlc) &&
                Objects.equals(alias, account.alias) &&
                Objects.equals(regions, account.regions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, name, sdlc, alias, regions);
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId='" + accountId + '\'' +
                ", name='" + name + '\'' +
                ", sdlc='" + sdlc + '\'' +
                ", alias='" + alias + '\'' +
                ", regions=" + regions +
                '}';
    }
}
