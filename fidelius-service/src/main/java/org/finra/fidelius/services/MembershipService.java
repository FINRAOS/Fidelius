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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service
public class MembershipService {

    @Inject
    private RESTService restService;

    @Value("${fidelius.membership-server-url}")
    protected String membershipServerUrl;

    @Value("${fidelius.membership-server-uri}")
    protected String membershipServerUri;

    public List<String> getAllMemberships() {
        Membership memberships = restService.makeCall(membershipServerUrl, membershipServerUri, Membership.class);
        memberships.getMemberships().replaceAll(String::toUpperCase);

        return memberships.getMemberships();
    }
}
