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

import org.finra.fidelius.FideliusClient;
import org.finra.fidelius.authfilter.parser.FideliusUserProfile;
import org.finra.fidelius.exceptions.FideliusException;
import org.finra.fidelius.model.Credential;
import org.finra.fidelius.model.HistoryEntry;
import org.finra.fidelius.model.Metadata;
import org.finra.fidelius.model.MetadataTest;
import org.finra.fidelius.model.aws.AWSEnvironment;
import org.finra.fidelius.model.db.DBCredential;
import org.finra.fidelius.services.auth.FideliusRoleService;
import org.finra.fidelius.services.aws.AWSSessionService;
import org.finra.fidelius.services.aws.DynamoDBService;
import org.finra.fidelius.services.user.model.FideliusUserEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.kms.KmsClient;

import javax.inject.Inject;
import java.util.*;

import static org.junit.Assert.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CredentialsServiceTest {

    @InjectMocks
    private CredentialsService credentialsService;

    @Mock
    private DynamoDBService dynamoDBService;

    @Mock
    private FideliusService fideliusService;

    @Mock
    private FideliusRoleService fideliusRoleService;

    @Mock
    private AWSSessionService awsSessionService;

    @Mock
    private FideliusClient fideliusClient;

    @Mock
    private MigrateService migrateService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(fideliusService.getCredential(anyString(), anyString(), anyString(), anyString(), isNull(Integer.class), anyString(), anyString())).thenReturn("Secret");
        when(awsSessionService.getDynamoDBClient(any())).thenReturn(DynamoDbClient.builder().build());
        when(awsSessionService.getKmsClient(any())).thenReturn(KmsClient.builder().build());
        FideliusUserEntry profile = new FideliusUserEntry("name", "test", "email@email.com", "John Johnson");
        when(fideliusRoleService.getUserProfile()).thenReturn(profile);

    }

    @Test
    public void getAllCredentialsShouldBeAbleToObtainCredentialsWithAndWithoutComponents() {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred1 = new HashMap<>();
        fakeCred1.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred1.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred1.put(CredentialsService.COMPONENT, AttributeValue.builder().s("TestComponent").build());
        fakeCred1.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred1.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Jon Snow").build());
        fakeCred1.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred2 = new HashMap<>();
        fakeCred2.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testKey2").build());
        fakeCred2.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred2.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred2.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred3 = fakeCred2;
        fakeCred3.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());

        fakeData.add(fakeCred1);
        fakeData.add(fakeCred2);

        when(dynamoDBService.scanDynamoDB(any(), any(), any())).thenReturn(fakeData);
        when(migrateService.migrateCredential(any(), any())).thenReturn(fakeCred3);

        List<Credential> expectedCreds = new ArrayList<>();
        expectedCreds.add(new Credential("testKey2", "APP.dev.testKey2", "some-account","region", "APP", "dev",
                null, "Ned Stark", "2018-04-04T12:51:37.803Z"));
        expectedCreds.add(new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "Jon Snow", "2018-04-04T12:51:37.803Z"));

        assertEquals(expectedCreds, credentialsService.getAllCredentials("table", "some-account", "region", "APP"));
    }

    @Test
    public void getAllCredentialsShouldBeAbleToHandleLegacyCredentialEntries() {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred1 = new HashMap<>();
        fakeCred1.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred1.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred1.put(CredentialsService.COMPONENT, AttributeValue.builder().s("TestComponent").build());
        fakeCred1.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());

        fakeData.add(fakeCred1);

        when(dynamoDBService.scanDynamoDB(any(), any(), any())).thenReturn(fakeData);
        credentialsService.getAllCredentials("table", "dev", "us-east-1", "APP");
    }

    @Test
    public void getCredentialHistoryShouldBeAbleToHandleLegacyCredentialEntries() {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred1 = new HashMap<>();
        fakeCred1.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred1.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred1.put(CredentialsService.COMPONENT, AttributeValue.builder().s("TestComponent").build());
        fakeCred1.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());

        fakeData.add(fakeCred1);

        when(dynamoDBService.scanDynamoDB(any(), any(), any())).thenReturn(fakeData);
        credentialsService.getCredentialHistory("table", "dev", "us-east-1", "APP",
                "dev", "TestComponent", "testKey", false);
    }

    @Test
    public void getAllCredentialsShouldBeAbleToMigrateCredentialsWithEmptyComponent() {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred1 = new HashMap<>();
        fakeCred1.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred1.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred1.put(CredentialsService.COMPONENT, AttributeValue.builder().s("").build());
        fakeCred1.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred1.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Jon Snow").build());
        fakeCred1.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred2 = new HashMap<>();
        fakeCred2.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testKey2").build());
        fakeCred2.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred2.put(CredentialsService.COMPONENT, AttributeValue.builder().s("").build());
        fakeCred2.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred2.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred2.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred3 = new HashMap<>();
        fakeCred3.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testKey3.extra").build());
        fakeCred3.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred3.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("").build());
        fakeCred3.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("").build());

        Map<String, AttributeValue> fakeCred4 = new HashMap<>();
        fakeCred4.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred4.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred4.put(CredentialsService.COMPONENT, AttributeValue.builder().s("TestComponent").build());
        fakeCred4.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred4.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred4.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred5 = new HashMap<>();
        fakeCred5.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testKey2").build());
        fakeCred5.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred5.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred5.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred5.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred6 = new HashMap<>();
        fakeCred6.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testKey3.extra").build());
        fakeCred6.put(CredentialsService.SDLC, AttributeValue.builder().s("testKey3").build());
        fakeCred6.put(CredentialsService.COMPONENT, AttributeValue.builder().s("dev").build());
        fakeCred6.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred6.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred6.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        fakeData.add(fakeCred1);
        fakeData.add(fakeCred2);
        fakeData.add(fakeCred3);

        when(dynamoDBService.scanDynamoDB(any(), any(), any())).thenReturn(fakeData);
        when(migrateService.guessCredentialProperties(fakeCred1)).thenReturn(fakeCred4);
        when(migrateService.guessCredentialProperties(fakeCred2)).thenReturn(fakeCred5);
        when(migrateService.guessCredentialProperties(fakeCred3)).thenReturn(fakeCred6);

        List<Credential> expectedCreds = new ArrayList<>();

        expectedCreds.add(new Credential("testKey2",
                "APP.dev.testKey2",
                "some-account",
                "region",
                "APP",
                "dev",
                "",
                "Ned Stark",
                "2018-04-04T12:51:37.803Z"));

        expectedCreds.add(new Credential("testKey",
                "APP.TestComponent.dev.testKey",
                "some-account",
                "region",
                "APP",
                "dev",
                "TestComponent",
                "Ned Stark",
                "2018-04-04T12:51:37.803Z"));

        expectedCreds.add(new Credential("extra",
                "APP.dev.testKey3.extra",
                "some-account",
                "region",
                "APP",
                "testKey3",
                "dev",
                "Ned Stark",
                "2018-04-04T12:51:37.803Z"));

        List<Credential> results = credentialsService.getAllCredentials("table", "some-account", "region", "APP");

        assertEquals(expectedCreds.get(0), results.get(0));
    }

    @Test
    public void getAllCredentialsShouldBeAbleToMigrateCredentialsWithoutSDLC() {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred1 = new HashMap<>();
        fakeCred1.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred1.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred1.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Jon Snow").build());
        fakeCred1.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred2 = new HashMap<>();
        fakeCred2.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testKey2").build());
        fakeCred2.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred2.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred2.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred3 = new HashMap<>();
        fakeCred3.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testKey3.extra").build());
        fakeCred3.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred3.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred3.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred4 = new HashMap<>();
        fakeCred4.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred4.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred4.put(CredentialsService.COMPONENT, AttributeValue.builder().s("TestComponent").build());
        fakeCred4.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred4.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred4.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred5 = new HashMap<>();
        fakeCred5.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testKey2").build());
        fakeCred5.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred5.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred5.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred5.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred6 = new HashMap<>();
        fakeCred6.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testKey3.extra").build());
        fakeCred6.put(CredentialsService.SDLC, AttributeValue.builder().s("testKey3").build());
        fakeCred6.put(CredentialsService.COMPONENT, AttributeValue.builder().s("dev").build());
        fakeCred6.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred6.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred6.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        fakeData.add(fakeCred1);
        fakeData.add(fakeCred2);
        fakeData.add(fakeCred3);

        when(dynamoDBService.scanDynamoDB(any(), any(), any())).thenReturn(fakeData);
        when(migrateService.guessCredentialProperties(fakeCred1)).thenReturn(fakeCred4);
        when(migrateService.guessCredentialProperties(fakeCred2)).thenReturn(fakeCred5);
        when(migrateService.guessCredentialProperties(fakeCred3)).thenReturn(fakeCred6);

        List<Credential> expectedCreds = new ArrayList<>();

        expectedCreds.add(new Credential("testKey2", "APP.dev.testKey2", "some-account","region", "APP", "dev",
                null, "Ned Stark", "2018-04-04T12:51:37.803Z"));

        expectedCreds.add(new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "Ned Stark", "2018-04-04T12:51:37.803Z"));

        expectedCreds.add(new Credential("extra", "APP.dev.testKey3.extra",  "some-account", "region", "APP", "testKey3",
                "dev", "Ned Stark", "2018-04-04T12:51:37.803Z"));

        List<Credential> results = credentialsService.getAllCredentials("table", "some-account", "region", "APP");

        assertEquals(expectedCreds, results);
    }

    @Test
    public void getCredential() {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred1 = new HashMap<>();
        fakeCred1.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred1.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred1.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Jon Snow").build());
        fakeCred1.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred4 = new HashMap<>();
        fakeCred4.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred4.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred4.put(CredentialsService.COMPONENT, AttributeValue.builder().s("TestComponent").build());
        fakeCred4.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred4.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred4.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        fakeData.add(fakeCred1);

        when(dynamoDBService.queryDynamoDB(any(), any())).thenReturn(fakeData);
        when(migrateService.migrateCredential(any(), any())).thenReturn(fakeCred4);
        when(migrateService.guessCredentialProperties(any())).thenReturn(fakeCred4);

        Credential expectedCreds = new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "Ned Stark", "2018-04-04T12:51:37.803Z");

        Credential result = credentialsService.getCredential("some-account", "region", "APP", "APP.TestComponent.dev.testKey");

        assertEquals(result, expectedCreds);
    }

    @Test(expected = Exception.class)
    public void getCredentialFaliureToMigrate()throws Exception {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred1 = new HashMap<>();
        fakeCred1.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred1.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred1.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Jon Snow").build());
        fakeCred1.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred4 = new HashMap<>();
        fakeCred4.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred4.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred4.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred4.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        fakeData.add(fakeCred1);

        when(dynamoDBService.queryDynamoDB(any(), any())).thenReturn(fakeData);
        when(migrateService.migrateCredential(any(), any())).thenReturn(null);

        Credential expectedCreds = new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "Ned Stark", "2018-04-04T12:51:37.803Z");

        Credential result = credentialsService.getCredential("some-account", "region", "APP", "APP.TestComponent.dev.testKey");

        assertEquals(null, result);
    }


    @Test
    public void getCredentialFaliureToFindCredential() {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred1 = new HashMap<>();
        fakeCred1.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred1.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred1.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Jon Snow").build());
        fakeCred1.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred4 = new HashMap<>();
        fakeCred4.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred4.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred4.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred4.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        fakeData.add(fakeCred1);

        when(dynamoDBService.scanDynamoDB(any(), any(), any())).thenReturn(fakeData);
        when(migrateService.migrateCredential(any(), any())).thenThrow(NoSuchElementException.class);

        Credential expectedCreds = new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "Ned Stark", "2018-04-04T12:51:37.803Z");

        Credential result = credentialsService.getCredential("some-account", "region", "APP", "APP.TestComponent.dev.testKey");

        assertEquals(result, null);
    }



    @Test
    public void getAllCredentialsShouldOnlyReturnLatestVersionOfCredentials() {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred1 = new HashMap<>();
        fakeCred1.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred1.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred1.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Jon Snow").build());
        fakeCred1.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred2 = new HashMap<>();
        fakeCred2.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred2.put(CredentialsService.VERSION, AttributeValue.builder().s("0002").build());
        fakeCred2.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred2.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred3 = fakeCred2;
        fakeCred3.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred3.put(CredentialsService.COMPONENT, AttributeValue.builder().s("TestComponent").build());

        fakeData.add(fakeCred1);
        fakeData.add(fakeCred2);

        when(dynamoDBService.scanDynamoDB(any(), any(), any())).thenReturn(fakeData);

        List<Credential> expectedCreds = new ArrayList<>();
        expectedCreds.add(new Credential("testKey", "APP.TestComponent.dev.testKey", "my_account","region", "APP","dev",
                "TestComponent", "Ned Stark", "2018-04-04T12:51:37.803Z"));

        assertTrue(credentialsService.getAllCredentials("table", "my_account", "region", "APP")
                .equals(expectedCreds));

    }

    @Test
    public void getCredentialHistoryShouldBeAbleToCorrectlyCreateAHistoryObjectFromObtainedData() {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred = new HashMap<>();
        fakeCred.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Jon Snow").build());
        fakeCred.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        fakeData.add(fakeCred);

        when(dynamoDBService.queryDynamoDB(any(), any())).thenReturn(fakeData);

        List<HistoryEntry> expectedHistory = new ArrayList<>();
        HistoryEntry expectedEntry = new HistoryEntry(1, "Jon Snow", "2018-04-04T12:51:37.803Z");
        expectedHistory.add(expectedEntry);

        assertTrue(credentialsService.getCredentialHistory("table", "account", "region", "APP", "dev", "comp", "key", false)
                .equals(expectedHistory));
    }

    @Test
    public void getCredentialSecret() throws Exception {
        String account = "dev";
        String region = "us-east-1";
        String application = "membership";
        String environment = "environment";
        String component = "testComponent";
        String shortKey = "shortKey";

        Credential expected = new Credential(shortKey,null,account, region, application, environment, component, null, null,"Secret");
        Credential actual = credentialsService.getCredentialSecret(account, region, application, environment, component, shortKey, null);

        assertEquals(expected.getSecret(), actual.getSecret());
    }


    @Test
    public void getCredentialSecretNotFound() throws Exception {
        String account = "dev";
        String region = "us-east-1";
        String application = "membership";
        String environment = "environment";
        String component = "testComponent";
        String shortKey = "shortKey";

        doThrow(new Exception("Not found.")).when(fideliusService).getCredential(anyString(), anyString(), anyString(), anyString(), isNull(Integer.class), anyString(), anyString());

        Credential actual = credentialsService.getCredentialSecret(account, region, application, environment, component, shortKey, null);

        assertNull(actual);
    }


    @Test
    public void putCredentialDoesNotGetCreated() throws Exception {
        Credential credential = new Credential();
        credential.setAccount("dev");
        credential.setRegion("us-east-1");
        credential.setApplication("membership");
        credential.setEnvironment("environment");
        credential.setComponent("testComponent");
        credential.setShortKey("shortKey");
        credential.setSecret("secretPassword");

        doThrow(new Exception("Error Created Credential.")).when(fideliusService).putCredential(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        Credential actual = credentialsService.putCredential(credential);

        assertNull(actual);
    }

    @Test
    public void putMetadataDoesNotGetCreated() throws Exception {
        Metadata metadata = new Metadata();
        metadata.setAccount("dev");
        metadata.setRegion("us-east-1");
        metadata.setApplication("membership");
        metadata.setEnvironment("environment");
        metadata.setComponent("testComponent");
        metadata.setShortKey("shortKey");
        metadata.setSourceType("RDS");
        metadata.setSource("membership");

        doThrow(new Exception("Error Created Credential.")).when(fideliusService).putMetadata(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        Metadata actual = credentialsService.putMetadata(metadata);

        assertNull(actual);
    }

    @Test
    public void putCredential() throws Exception {
        Credential credential = new Credential();
        credential.setAccount("dev");
        credential.setRegion("us-east-1");
        credential.setApplication("membership");
        credential.setEnvironment("environment");
        credential.setComponent("testComponent");
        credential.setShortKey("shortKey");
        credential.setSecret("secretPassword");

        Mockito.doReturn("000000000000000001").when(fideliusService).putCredential(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        Credential actual = credentialsService.putCredential(credential);

        assertEquals(credential, actual);
    }

    @Test
    public void putMetadata() throws Exception {
        Metadata metadata = new Metadata();
        metadata.setAccount("dev");
        metadata.setRegion("us-east-1");
        metadata.setApplication("membership");
        metadata.setEnvironment("environment");
        metadata.setComponent("testComponent");
        metadata.setShortKey("shortKey");
        metadata.setSourceType("RDS");
        metadata.setSource("membership");

        Mockito.doReturn("000000000000000001").when(fideliusService).putMetadata(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        Metadata actual = credentialsService.putMetadata(metadata);

        assertEquals(metadata, actual);
    }

    @Test(expected = FideliusException.class)
    public void createCredentialShouldNotCreateDuplicateCredentials() throws FideliusException {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred = new HashMap<>();
        fakeCred.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testComponent.testKey").build());
        fakeCred.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Jon Snow").build());
        fakeCred.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        fakeData.add(fakeCred);

        Credential credential = new Credential();
        credential.setAccount("dev");
        credential.setRegion("us-east-1");
        credential.setApplication("APP");
        credential.setEnvironment("dev");
        credential.setComponent("testComponent");
        credential.setShortKey("testKey");
        credential.setSecret("secretPassword");

        when(dynamoDBService.queryDynamoDB(any(), any())).thenReturn(fakeData);
        Credential result = credentialsService.createCredential(credential);
    }

    @Test
    public void deleteCredentialDoesNotDelete() throws Exception {
        Credential credential = new Credential();
        credential.setAccount("dev");
        credential.setRegion("us-east-1");
        credential.setApplication("membership");
        credential.setEnvironment("environment");
        credential.setComponent("testComponent");
        credential.setShortKey("shortKey");

        doThrow(new Exception("Credential not found")).when(fideliusService).deleteCredential(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        Credential actual = credentialsService.deleteCredential(credential);

        assertNull(actual);
    }

    @Test
    public void deleteCredential() throws Exception {
        Credential credential = new Credential();
        credential.setAccount("dev");
        credential.setRegion("us-east-1");
        credential.setApplication("membership");
        credential.setEnvironment("environment");
        credential.setComponent("testComponent");
        credential.setShortKey("shortKey");

        doNothing().when(fideliusService).deleteCredential(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        Credential actual = credentialsService.deleteCredential(credential);

        assertEquals(credential, actual);
    }

    @Test
    public void deleteMetadataDoesNotDelete() throws Exception {
        Metadata metadata = new Metadata();
        metadata.setAccount("dev");
        metadata.setRegion("us-east-1");
        metadata.setApplication("membership");
        metadata.setEnvironment("environment");
        metadata.setComponent("testComponent");
        metadata.setShortKey("shortKey");

        doThrow(new Exception("Metadata not found")).when(fideliusService).deleteMetadata(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        Metadata actual = credentialsService.deleteMetadata(metadata);

        assertNull(actual);
    }

    @Test
    public void deleteMetadata() throws Exception {
        Metadata metadata = new Metadata();
        metadata.setAccount("dev");
        metadata.setRegion("us-east-1");
        metadata.setApplication("membership");
        metadata.setEnvironment("environment");
        metadata.setComponent("testComponent");
        metadata.setShortKey("shortKey");

        doNothing().when(fideliusService).deleteMetadata(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        Metadata actual = credentialsService.deleteMetadata(metadata);

        assertEquals(metadata, actual);
    }

    @Test
    public void getAllCredentialsShortensFullIAMRoleARNs() {
        List<Map<String, AttributeValue>> fakeData = new ArrayList<>();

        Map<String, AttributeValue> fakeCred1 = new HashMap<>();
        fakeCred1.put(CredentialsService.NAME, AttributeValue.builder().s("APP.TestComponent.dev.testKey").build());
        fakeCred1.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred1.put(CredentialsService.COMPONENT, AttributeValue.builder().s("TestComponent").build());
        fakeCred1.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred1.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("arn:aws:sts::1234567890:assumed-role/private_aws_somerole_d/L25000").build());
        fakeCred1.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        Map<String, AttributeValue> fakeCred2 = new HashMap<>();
        fakeCred2.put(CredentialsService.NAME, AttributeValue.builder().s("APP.dev.testKey2").build());
        fakeCred2.put(CredentialsService.SDLC, AttributeValue.builder().s("dev").build());
        fakeCred2.put(CredentialsService.VERSION, AttributeValue.builder().s("0001").build());
        fakeCred2.put(CredentialsService.UPDATED_BY, AttributeValue.builder().s("Ned Stark").build());
        fakeCred2.put(CredentialsService.UPDATED_ON, AttributeValue.builder().s("2018-04-04T12:51:37.803Z").build());

        fakeData.add(fakeCred1);
        fakeData.add(fakeCred2);

        when(dynamoDBService.scanDynamoDB(any(), any(), any())).thenReturn(fakeData);

        List<Credential> expectedCreds = new ArrayList<>();
        expectedCreds.add(new Credential("testKey2", "APP.dev.testKey2", "some-account","region", "APP", "dev",
                null, "Ned Stark", "2018-04-04T12:51:37.803Z"));
        expectedCreds.add(new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "private_aws_somerole_d/L25000", "2018-04-04T12:51:37.803Z"));

        assertEquals(expectedCreds, credentialsService.getAllCredentials("table", "some-account", "region", "APP"));
    }
}
