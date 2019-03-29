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

import org.junit.Test;

import static org.junit.Assert.*;

public class DBCredentialTest {


    @Test
    public void getShortKeyWithComponent() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.component.dev.password");
        dbCredential.setComponent("component");
        dbCredential.setSdlc("dev");

        String result = dbCredential.getShortKey();

        assertEquals("password", result);
    }

    @Test
    public void getShortKeyWithComponentWithMoreThan4Fields() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.component.dev.secret.password");
        dbCredential.setComponent("component");
        dbCredential.setSdlc("dev");

        String result = dbCredential.getShortKey();

        assertEquals("secret.password", result);
    }

    @Test
    public void getShortKeyWithoutComponent() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev.password");
        dbCredential.setSdlc("dev");

        String result = dbCredential.getShortKey();

        assertEquals("password", result);
    }

    @Test
    public void getShortKeyWithoutComponentAndKeyWithMoreThan3Fields() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev.secret.password");
        dbCredential.setSdlc("dev");

        String result = dbCredential.getShortKey();

        assertEquals("secret.password", result);
    }

    @Test
    public void getShortKeyWithoutComponentAndKeyWithMoreThan3FieldsAndCharacters() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev_test.secret.<password*");
        dbCredential.setSdlc("dev_test");

        String result = dbCredential.getShortKey();

        assertEquals("secret.<password*", result);
    }

    @Test
    public void getShortKeyWithoutComponentAndKeyWithMoreThan3FieldsAndCharacters2() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.${dev.component.local.app.key");
        dbCredential.setSdlc("${dev");

        String result = dbCredential.getShortKey();

        assertEquals("APP.${dev.component.local.app.key", result);
    }

}