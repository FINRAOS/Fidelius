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

package org.finra.fidelius.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finra.fidelius.services.auth.FideliusRole;
import org.finra.fidelius.services.auth.FideliusRoleService;
import org.finra.fidelius.services.user.model.FideliusUserEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("local")
@RunWith(SpringRunner.class)
@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @MockBean
    private FideliusRoleService fideliusRoleService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private MockHttpServletRequestBuilder getRoleRequest = get("/auth/role");


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @WithMockUser
    public void getRoleShouldReturnDevUser() throws Exception {
        FideliusUserEntry user =  new FideliusUserEntry("U250", "someDn", "user@company.com", "John Doe");
        FideliusRole role = FideliusRole.DEV;

        when(fideliusRoleService.getUserProfile()).thenReturn(user);
        when(fideliusRoleService.getRole()).thenReturn(role);

        mockMvc.perform(getRoleRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("role", is("DEV")))
                .andExpect(jsonPath("name", is("John Doe")));
    }

    @Test
    @WithMockUser
    public void getRoleShouldReturnUnAuthorizedUser() throws Exception {
        FideliusUserEntry user =  new FideliusUserEntry("U250", "someDn", "user@company.com", "John Doe");
        FideliusRole role = FideliusRole.UNAUTHORIZED;

        when(fideliusRoleService.getUserProfile()).thenReturn(user);
        when(fideliusRoleService.getRole()).thenReturn(role);

        mockMvc.perform(getRoleRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("role", is("UNAUTHORIZED")))
                .andExpect(jsonPath("name", is("John Doe")));
    }
}