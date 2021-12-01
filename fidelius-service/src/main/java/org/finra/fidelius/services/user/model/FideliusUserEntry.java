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

package org.finra.fidelius.services.user.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class FideliusUserEntry {
    private String userId;
    private String email;
    private String name;
    private String dn;

    public FideliusUserEntry(String userId, String dn, String email, String name) {
        this.userId = userId;
        this.dn = dn;
        this.email = email;
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getDn() {
        return dn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FideliusUserEntry that = (FideliusUserEntry) o;
        return Objects.equal(userId, that.userId) &&
                Objects.equal(email, that.email) &&
                Objects.equal(name, that.name)&&
                Objects.equal(dn, that.dn);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId, dn, email, name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("userId", userId)
                .add("email", email)
                .add("name", name)
                .add("dn", dn)
                .toString();
    }
}