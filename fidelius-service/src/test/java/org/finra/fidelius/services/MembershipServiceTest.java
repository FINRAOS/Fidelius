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

package org.finra.fidelius.services;

import org.finra.fidelius.model.membership.Membership;
import org.finra.fidelius.services.rest.RESTService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MembershipServiceTest {

    @InjectMocks
    MembershipService membershipService;

    @Mock
    private RESTService restService;

    @Before
    public void initMocks() {

    }

    @Test
    public void getAllMemberships() throws Exception {

        ArrayList<String> opsMemberships;
        opsMemberships = new ArrayList<>();
        opsMemberships.add("Application1");
        opsMemberships.add("Application2");
        opsMemberships.add("Application3");

        HashMap<String, List<String>> node;
        node = new HashMap<>();
        node.put("test", opsMemberships);

        Membership response = new Membership(node);

        when(restService.makeCall(any(), any(), any(), any())).thenReturn(response);

        Assert.assertTrue(membershipService.getAllMemberships("testUser").contains("APPLICATION1"));
        Assert.assertTrue(membershipService.getAllMemberships("testUser").contains("APPLICATION2"));
        Assert.assertTrue(membershipService.getAllMemberships("testUser").contains("APPLICATION3"));

    }

    @Test
    public void getEmptyMemberships() throws Exception {

        when(restService.makeCall(any(), any(), any(), any())).thenReturn(new Membership());

        Assert.assertTrue(membershipService.getAllMemberships("testUser").size() == 0);

    }
}
