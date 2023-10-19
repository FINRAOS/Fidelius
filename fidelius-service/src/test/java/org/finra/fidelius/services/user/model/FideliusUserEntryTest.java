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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Objects;


@RunWith(MockitoJUnitRunner.class)
public class FideliusUserEntryTest {

    @Test
    public void testConstructorGetter(){
        String id ="user";
        String dn ="someDn";
        String email="user@email.com";
        String name ="User Name";

        FideliusUserEntry fideliusUserEntry = new FideliusUserEntry(id, dn,email,name);

        Assert.assertEquals("Test ID getter", id, fideliusUserEntry.getUserId());
        Assert.assertEquals("Test Email getter", email, fideliusUserEntry.getEmail());
        Assert.assertEquals("Test Name getter", name, fideliusUserEntry.getName());
    }

    @Test
    public void testEquals() throws Exception {
        String id ="user";
        String dn ="someDn";
        String email="user@email.com";
        String name ="User Name";

        FideliusUserEntry fideliusUserEntry1 = new FideliusUserEntry(id,dn, email,name);
        FideliusUserEntry fideliusUserEntry2 = new FideliusUserEntry(id,dn, email,name);
        FideliusUserEntry fideliusUserEntry3 = new FideliusUserEntry("hello", dn,email,name);
        FideliusUserEntry fideliusUserEntry5 = new FideliusUserEntry(id, dn, "I",name);
        FideliusUserEntry fideliusUserEntry6 = new FideliusUserEntry(id, dn, email,"test");
        FideliusUserEntry fideliusUserEntry7 = new FideliusUserEntry(id, "newDn", email,"test");

        Assert.assertEquals("Self Check", fideliusUserEntry1, fideliusUserEntry1);
        Assert.assertEquals("Different but Same Objects", fideliusUserEntry1, fideliusUserEntry2);
        Assert.assertNotEquals("Different Objects", fideliusUserEntry1, "HI");
        Assert.assertNotEquals("Different id", fideliusUserEntry1, fideliusUserEntry3);
        Assert.assertNotEquals("Different email", fideliusUserEntry1, fideliusUserEntry5);
        Assert.assertNotEquals("Different name", fideliusUserEntry1, fideliusUserEntry6);
        Assert.assertNotEquals("Different role", fideliusUserEntry1, fideliusUserEntry7);
        Assert.assertNotEquals("Different role", fideliusUserEntry1, fideliusUserEntry7);
    }

    @Test
    public void testToString(){
        String id ="user";
        String dn ="someDn";
        String email="user@email.com";
        String name ="User Name";

        FideliusUserEntry fideliusUserEntry1 = new FideliusUserEntry(id,dn,email,name);

        Assert.assertNotNull("ToString returns some kind of string", fideliusUserEntry1.toString());
    }

    @Test
    public void testHashCode(){
        String id ="user";
        String dn ="someDn";
        String email="user@email.com";
        String name ="User Name";

        FideliusUserEntry fideliusUserEntry = new FideliusUserEntry(id,dn,email,name);

        Assert.assertEquals("Hashcode", fideliusUserEntry.hashCode(), Objects.hash(id,dn,email,name));

    }
}
