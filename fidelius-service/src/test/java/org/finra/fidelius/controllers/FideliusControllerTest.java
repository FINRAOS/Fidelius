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
import org.finra.fidelius.exceptions.FideliusException;
import org.finra.fidelius.model.Credential;
import org.finra.fidelius.model.HistoryEntry;
import org.finra.fidelius.model.Metadata;
import org.finra.fidelius.model.account.Account;
import org.finra.fidelius.services.CredentialsService;
import org.finra.fidelius.services.MembershipService;

import org.finra.fidelius.services.account.AccountsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@ActiveProfiles("local")
@RunWith(SpringRunner.class)
@WebMvcTest(FideliusController.class)
public class FideliusControllerTest {

    @MockBean
    private MembershipService membershipService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CredentialsService credentialsService;

    @MockBean
    private AccountsService accountsService;

    @Autowired
    ObjectMapper objectMapper;

    private Credential credential = new Credential("shortKey", null, "APP","dev","us-east-3", "environment",
            null, null, null);

    private Metadata metadata = new Metadata("shortKey", null, "APP","dev","us-east-3", "environment",
            "sourceType", "source", null, null, null);

    private MockHttpServletRequestBuilder getCredentialSecretRequest = get("/credentials/secret")
            .param("account", "dev")
            .param("region", "us-east-1")
            .param("application", "membership")
            .param("environment", "environment")
            .param("component", "component")
            .param("shortKey", "shortKey");

    private MockHttpServletRequestBuilder getMetadataRequest = get("/credentials/metadata")
            .param("account", "dev")
            .param("region", "us-east-1")
            .param("application", "membership")
            .param("environment", "environment")
            .param("component", "component")
            .param("shortKey", "shortKey");


    private MockHttpServletRequestBuilder getActiveDirectoryRegularExpressionRequest = get("/validActiveDirectoryRegularExpression");

    private MockHttpServletRequestBuilder getAccountsRequest = get("/accounts");

    private MockHttpServletRequestBuilder deleteSecretRequest = delete("/credentials/secret")
            .param("account", "dev")
            .param("region", "us-east-1")
            .param("application", "membership")
            .param("environment", "environment")
            .param("component", "component")
            .param("shortKey", "shortKey");

    private MockHttpServletRequestBuilder deleteMetadataRequest = delete("/credentials/metadata")
            .param("account", "dev")
            .param("region", "us-east-1")
            .param("application", "membership")
            .param("environment", "environment")
            .param("component", "component")
            .param("shortKey", "shortKey");


    private MockHttpServletRequestBuilder createCredentialRequest = post("/credentials/secret")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\n" +
                    "  \"account\": \"dev\",\n" +
                    "  \"application\": \"pet\",\n" +
                    "  \"membership\": \"us-east-1\",\n" +
                    "  \"environment\": \"dev\",\n" +
                    "  \"region\": \"us-east-1\",\n" +
                    "  \"secret\": \"secret\",\n" +
                    "  \"shortKey\": \"shortKey\",\n" +
                    "  \"isActiveDirectory\": false\n" +
                    "}");

    private MockHttpServletRequestBuilder createMetadataRequest = post("/credentials/metadata")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\n" +
                    "  \"account\": \"dev\",\n" +
                    "  \"application\": \"pet\",\n" +
                    "  \"membership\": \"us-east-1\",\n" +
                    "  \"environment\": \"dev\",\n" +
                    "  \"region\": \"us-east-1\",\n" +
                    "  \"secret\": \"secret\",\n" +
                    "  \"sourceType\": \"sourceType\",\n" +
                    "  \"source\": \"source\",\n" +
                    "  \"shortKey\": \"shortKey\",\n" +
                    "  \"isActiveDirectory\": false\n" +
                    "}");

    private MockHttpServletRequestBuilder updateCredentialRequest = put("/credentials/secret")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\n" +
                    "  \"account\": \"dev\",\n" +
                    "  \"application\": \"pet\",\n" +
                    "  \"membership\": \"us-east-1\",\n" +
                    "  \"environment\": \"dev\",\n" +
                    "  \"region\": \"us-east-1\",\n" +
                    "  \"secret\": \"secret\",\n" +
                    "  \"shortKey\": \"shortKey\",\n" +
                    "  \"isActiveDirectory\": false\n" +
                    "}");

    private MockHttpServletRequestBuilder getCredentialHistoryRequest = get("/credentials/history")
            .param("account", "dev")
            .param("region", "us-east-1")
            .param("application", "APP")
            .param("environment", "dev")
            .param("component", "component")
            .param("shortKey", "myKey");

    private MockHttpServletRequestBuilder getAllCredentialsRequest = get("/credentials/")
            .param("account", "dev")
            .param("region", "us-east-1")
            .param("application", "APP");

    private MockHttpServletRequestBuilder getCredentialRequest = get("/credentials/APP.dev.TestComponent.testKey/")
            .param("account", "dev")
            .param("region", "us-east-1")
            .param("application", "APP");


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @WithMockUser
    public void getAllCredentials() throws Exception {

        List<Credential> fakeData = new ArrayList<>();

        fakeData.add(new Credential("testKey", "APP.dev.TestComponent.testKey", "dev", null,"APP", "dev",
                "TestComponent", "Jon Snow", "2018-04-04T12:51:37.803Z"));
        fakeData.add(new Credential("testKey2", "APP.dev.testKey2", "dev", null,"APP","dev",
                null, "Ned Stark", "2018-04-04T12:51:37.803Z"));

        when(credentialsService.getAllCredentials(any(), any(), any(), any())).thenReturn(fakeData);
        mockMvc.perform(getAllCredentialsRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].shortKey", is("testKey")))
                .andExpect(jsonPath("$[0].longKey", is("APP.dev.TestComponent.testKey")))
                .andExpect(jsonPath("$[0].account", is("dev")))
                .andExpect(jsonPath("$[0].environment", is("dev")))
                .andExpect(jsonPath("$[0].component", is("TestComponent")))
                .andExpect(jsonPath("$[0].lastUpdatedBy", is("Jon Snow")))
                .andExpect(jsonPath("$[0].lastUpdatedDate", is("2018-04-04T12:51:37.803Z")))

                .andExpect(jsonPath("$[1].shortKey", is("testKey2")))
                .andExpect(jsonPath("$[1].longKey", is("APP.dev.testKey2")))
                .andExpect(jsonPath("$[1].account", is("dev")))
                .andExpect(jsonPath("$[1].environment", is("dev")))
                .andExpect(jsonPath("$[1].component", nullValue()))
                .andExpect(jsonPath("$[1].lastUpdatedBy", is("Ned Stark")))
                .andExpect(jsonPath("$[1].lastUpdatedDate", is("2018-04-04T12:51:37.803Z")));
    }

    @Test
    @WithMockUser
    public void getCredential() throws Exception {

        Credential fakeData = new Credential("testKey", "APP.dev.TestComponent.testKey", "dev", null,"APP", "dev",
                "TestComponent", "Jon Snow", "2018-04-04T12:51:37.803Z");

        when(credentialsService.getCredential(any(), any(), any(), any())).thenReturn(fakeData);
        mockMvc.perform(getCredentialRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("shortKey", is("testKey")))
                .andExpect(jsonPath("longKey", is("APP.dev.TestComponent.testKey")))
                .andExpect(jsonPath("account", is("dev")))
                .andExpect(jsonPath("environment", is("dev")))
                .andExpect(jsonPath("component", is("TestComponent")))
                .andExpect(jsonPath("lastUpdatedBy", is("Jon Snow")))
                .andExpect(jsonPath("lastUpdatedDate", is("2018-04-04T12:51:37.803Z")));
    }

    @Test
    @WithMockUser
    public void getCredentialHistory() throws Exception {

        List<HistoryEntry> fakeHistory = new ArrayList<>();
        fakeHistory.add(new HistoryEntry(1, "Obi Wan Kenobi", "2018-04-04T12:51:37.803Z"));
        fakeHistory.add(new HistoryEntry(2, "Anakin Skywalker", "2018-04-04T12:51:37.803Z"));
        fakeHistory.add(new HistoryEntry(3, "Obi Wan Kenobi", "2018-04-04T12:51:37.803Z"));
        fakeHistory.add(new HistoryEntry(4, "Anakin Skywalker", "2018-04-04T12:51:37.803Z"));
        fakeHistory.add(new HistoryEntry(5, "Obi Wan Kenobi", "2018-04-04T12:51:37.803Z"));

        when(credentialsService.getCredentialHistory(any(), any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(fakeHistory);
        mockMvc.perform(getCredentialHistoryRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0, 2, 4].updatedBy", is(Arrays.asList(new String[]{"Obi Wan Kenobi", "Obi Wan Kenobi", "Obi Wan Kenobi"}))))
                .andExpect(jsonPath("$[1, 3].updatedBy", is(Arrays.asList(new String[]{"Anakin Skywalker", "Anakin Skywalker"}))))
                .andExpect(jsonPath("$[0].revision", is(1)))
                .andExpect(jsonPath("$[4].revision", is(5)));
    }

    @Test
    @WithMockUser
    public void getAllCredentialsShouldReturnTimeoutCodeIfCredentialsServiceTimesOut() throws Exception {
        when(credentialsService.getAllCredentials(any(), any(), any(), any())).thenThrow(new FideliusException("Throttling rate exceeded!", HttpStatus.REQUEST_TIMEOUT));
        mockMvc.perform(getAllCredentialsRequest)
                .andExpect(status().isRequestTimeout())
                .andExpect(jsonPath("message", is("Throttling rate exceeded!")));
    }

    @Test
    @WithMockUser
    public void getCredentialHistoryShouldReturnTimeoutCodeIfCredentialsServiceTimesOut() throws Exception {
        when(credentialsService.getCredentialHistory(any(), any(), any(), any(), any(), any(), any(), anyBoolean())).thenThrow(new FideliusException("Throttling rate exceeded!", HttpStatus.REQUEST_TIMEOUT));
        mockMvc.perform(getCredentialHistoryRequest)
                .andExpect(status().isRequestTimeout())
                .andExpect(jsonPath("message", is("Throttling rate exceeded!")));
    }

    @Test
    @WithMockUser
    public void getAllCredentialsShouldReturn500CodeIfCredentialsServiceThrowsOtherExceptions() throws Exception {
        when(credentialsService.getAllCredentials(any(), any(), any(), any())).thenThrow(new FideliusException("Internal Error", HttpStatus.INTERNAL_SERVER_ERROR));
        mockMvc.perform(getAllCredentialsRequest)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("message", is("Internal Error")));
    }

    @Test
    @WithMockUser
    public void getCredentialHistoryShouldReturn500CodeIfCredentialsServiceThrowsOtherExceptions() throws Exception {
        when(credentialsService.getCredentialHistory(any(), any(), any(), any(), any(), any(), any(), anyBoolean())).thenThrow(new FideliusException("Internal Error", HttpStatus.INTERNAL_SERVER_ERROR));
        mockMvc.perform(getCredentialHistoryRequest)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("message", is("Internal Error")));
    }

    @Test
    @WithMockUser
    public void getAllCredentialsShouldReturnOkAndEmptyListIfCredentialsServiceReturnsNothing() throws Exception {
        when(credentialsService.getAllCredentials(any(), any(), any(), any())).thenReturn(new ArrayList<>());
        mockMvc.perform(getAllCredentialsRequest)
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void getCredentialHistoryShouldReturn404CodeIfCredentialsServiceReturnsNothing() throws Exception {
        when(credentialsService.getCredentialHistory(any(), any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(new ArrayList<>());
        mockMvc.perform(getCredentialHistoryRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    public void getSecretShouldReturnCredentialWithSecret() throws Exception {
        Credential response = new Credential("shortKey",null,"Dev", "us-east-1", "application","dev","component", null, null, "password");

        when(credentialsService.getCredentialSecret(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(getCredentialSecretRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("account", is("Dev")))
                .andExpect(jsonPath("secret", is("password")));

    }

    @Test
    @WithMockUser
    public void getMetadataShouldReturnMetadata() throws Exception {
        Metadata response = new Metadata("shortKey",null,"Dev", "us-east-1", "application","dev","sourceType", "source", "component", null, null);

        when(credentialsService.getMetadata(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(getMetadataRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("account", is("Dev")))
                .andExpect(jsonPath("sourceType", is("sourceType")))
                .andExpect(jsonPath("source", is("source")));
    }

    @Test
    @WithMockUser
    public void getSecretShouldReturn400ErrorWhenCredentialNotFound() throws Exception {
        when(credentialsService.getCredentialSecret(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(new Credential());

        mockMvc.perform(getCredentialSecretRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    public void getMetadataWhenMetadataNotFound() throws Exception {
        when(credentialsService.getMetadata(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(new Metadata());

        mockMvc.perform(getMetadataRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void getSecretShouldReturn401ErrorWhenUserNotIncluded() throws Exception {
        when(credentialsService.getCredentialSecret(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(new Credential());

        mockMvc.perform(getCredentialSecretRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void getMetadataShouldReturn401ErrorWhenUserNotIncluded() throws Exception {
        when(credentialsService.getMetadata(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(new Metadata());

        mockMvc.perform(getMetadataRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    public void createCredential() throws Exception {
        when(credentialsService.createCredential(any(Credential.class))).thenReturn(this.credential);

        mockMvc.perform(createCredentialRequest)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser
    public void createMetadata() throws Exception {
        when(credentialsService.createMetadata(any(Metadata.class))).thenReturn(this.metadata);

        mockMvc.perform(createMetadataRequest)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser
    public void createCredentialDoesNotGetCreated() throws Exception {
        when(credentialsService.putCredential(any(Credential.class))).thenReturn(null);

        mockMvc.perform(createCredentialRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    public void createMetadataDoesNotGetCreated() throws Exception {
        when(credentialsService.putMetadata(any(Metadata.class))).thenReturn(null);

        mockMvc.perform(createMetadataRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void createCredentialReturns401WhenNoUser() throws Exception {
        when(credentialsService.putCredential(any(Credential.class))).thenReturn(null);

        mockMvc.perform(createCredentialRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void createMetadataReturns401WhenNoUser() throws Exception {
        when(credentialsService.putMetadata(any(Metadata.class))).thenReturn(null);

        mockMvc.perform(createMetadataRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    public void updateCredential() throws Exception {
        when(credentialsService.putCredential(any(Credential.class))).thenReturn(this.credential);

        mockMvc.perform(updateCredentialRequest)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser()
    public void updateCredentialDoesNotGetUpdated() throws Exception {
        when(credentialsService.putCredential(any(Credential.class))).thenReturn(null);

        mockMvc.perform(updateCredentialRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateCredentialReturns401WhenUserNotFound() throws Exception {
        when(credentialsService.putCredential(any(Credential.class))).thenReturn(null);

        mockMvc.perform(updateCredentialRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    public void deleteCredential() throws Exception {
        when(credentialsService.deleteCredential(any(Credential.class))).thenReturn(this.credential);

        mockMvc.perform(deleteSecretRequest)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser
    public void deleteMetadata() throws Exception {
        when(credentialsService.deleteMetadata(any(Metadata.class))).thenReturn(this.metadata);

        mockMvc.perform(deleteMetadataRequest)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser
    public void deleteCredentialShouldReturn400ErrorWhenCredentialNotFound() throws Exception {
        when(credentialsService.deleteCredential(credential)).thenReturn(new Credential());

        mockMvc.perform(deleteSecretRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    public void deleteMetadataShouldReturn400ErrorWhenMetadataNotFound() throws Exception {
        when(credentialsService.deleteMetadata(metadata)).thenReturn(new Metadata());

        mockMvc.perform(deleteMetadataRequest)
                .andExpect(status().is4xxClientError());
    }


    @Test
    public void deleteCredentialShouldReturn401ErrorWhenUserNotFound() throws Exception {
        when(credentialsService.deleteCredential(credential)).thenReturn(new Credential());

        mockMvc.perform(deleteSecretRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void deleteMetadataShouldReturn401ErrorWhenUserNotFound() throws Exception {
        when(credentialsService.deleteMetadata(metadata)).thenReturn(new Metadata());

        mockMvc.perform(deleteMetadataRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    public void activeDirectoryShouldReturnDefaultRegularExpressionIfNoneSet() throws Exception {
        mockMvc.perform(getActiveDirectoryRegularExpressionRequest)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("{\"validActiveDirectoryRegularExpression\":\"(.*?)\",\"validActiveDirectoryDescription\":\"''\"}"));
    }

    @Test
    @WithMockUser
    public void shouldReturnAWSAccounts() throws Exception {
        Account account = new Account();
        account.setAccountId("342391");
        account.setAlias("NewAccount");
        account.setName("accountName");
        account.setSdlc("dev");

        List<Account> accounts = new ArrayList<>();
        accounts.add(account);

        when(accountsService.getAccounts()).thenReturn(accounts);

        mockMvc.perform(getAccountsRequest)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("[{\"accountId\":\"342391\",\"name\":\"accountName\",\"sdlc\":\"dev\",\"alias\":\"NewAccount\",\"regions\":null}]"));
    }

    @Test
    public void shouldReturn401ErrorIfUserNotFoundOnAWSAccounts() throws Exception {
        when(accountsService.getAccounts()).thenReturn(null);

        mockMvc.perform(getAccountsRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    public void shouldReturn404ErrorIfNoAWSAccounts() throws Exception {
        when(accountsService.getAccounts()).thenReturn(null);

        mockMvc.perform(getAccountsRequest)
                .andExpect(status().is4xxClientError());
    }
}
