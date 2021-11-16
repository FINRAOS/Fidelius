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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kms.AWSKMSClient;
import org.finra.fidelius.FideliusClient;
import org.finra.fidelius.authfilter.parser.FideliusUserProfile;
import org.finra.fidelius.exceptions.FideliusException;
import org.finra.fidelius.model.Credential;
import org.finra.fidelius.model.HistoryEntry;
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

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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
        when(fideliusService.getCredential(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("Secret");
        when(awsSessionService.getDynamoDBClient(any())).thenReturn(new AmazonDynamoDBClient());
        when(awsSessionService.getKmsClient(any())).thenReturn(new AWSKMSClient());
        FideliusUserEntry profile = new FideliusUserEntry("name", "test", "email@email.com", "John Johnson");
        when(fideliusRoleService.getUserProfile()).thenReturn(profile);

    }

    @Test
    public void getAllCredentialsShouldBeAbleToObtainCredentialsWithAndWithoutComponents() {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred1 = new DBCredential();
        fakeCred1.setName("APP.TestComponent.dev.testKey");
        fakeCred1.setSdlc("dev");
        fakeCred1.setComponent("TestComponent");
        fakeCred1.setVersion("0001");
        fakeCred1.setUpdatedBy("Jon Snow");
        fakeCred1.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred2 = new DBCredential();
        fakeCred2.setName("APP.dev.testKey2");
        fakeCred2.setVersion("0001");
        fakeCred2.setUpdatedBy("Ned Stark");
        fakeCred2.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred3 = fakeCred2;
        fakeCred3.setSdlc("dev");

        fakeData.add(fakeCred1);
        fakeData.add(fakeCred2);

        when(dynamoDBService.scanDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);
        when(migrateService.migrateCredential(any(), any())).thenReturn(fakeCred3);

        List<Credential> expectedCreds = new ArrayList<>();
        expectedCreds.add(new Credential("testKey2", "APP.dev.testKey2", "some-account","region", "APP", "dev",
                null, "Ned Stark", "2018-04-04T12:51:37.803Z"));
        expectedCreds.add(new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "Jon Snow", "2018-04-04T12:51:37.803Z"));

        assertTrue(credentialsService.getAllCredentials("table", "some-account", "region", "APP").equals(expectedCreds));
    }

    @Test
    public void getAllCredentialsShouldBeAbleToHandleLegacyCredentialEntries() {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred1 = new DBCredential();
        fakeCred1.setName("APP.TestComponent.dev.testKey");
        fakeCred1.setSdlc("dev");
        fakeCred1.setComponent("TestComponent");
        fakeCred1.setVersion("0001");

        fakeData.add(fakeCred1);

        when(dynamoDBService.scanDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);
        credentialsService.getAllCredentials("table", "dev", "us-east-1", "APP");
    }

    @Test
    public void getCredentialHistoryShouldBeAbleToHandleLegacyCredentialEntries() {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred1 = new DBCredential();
        fakeCred1.setName("APP.TestComponent.dev.testKey");
        fakeCred1.setSdlc("dev");
        fakeCred1.setComponent("TestComponent");
        fakeCred1.setVersion("0001");

        fakeData.add(fakeCred1);

        when(dynamoDBService.scanDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);
        credentialsService.getCredentialHistory("table", "dev", "us-east-1", "APP",
                "dev", "TestComponent", "testKey", false);
    }

    @Test
    public void getAllCredentialsShouldBeAbleToMigrateCredentialsWithEmptyComponent() {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred1 = new DBCredential();
        fakeCred1.setName("APP.TestComponent.dev.testKey");
        fakeCred1.setComponent("");
        fakeCred1.setVersion("0001");
        fakeCred1.setUpdatedBy("Jon Snow");
        fakeCred1.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred2 = new DBCredential();
        fakeCred2.setName("APP.dev.testKey2");
        fakeCred1.setComponent("");
        fakeCred1.setSdlc("");
        fakeCred2.setVersion("0001");
        fakeCred2.setUpdatedBy("Ned Stark");
        fakeCred2.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred3 = new DBCredential();
        fakeCred3.setName("APP.dev.testKey3.extra");
        fakeCred3.setVersion("0001");
        fakeCred3.setUpdatedBy("");
        fakeCred3.setUpdatedDate("");

        DBCredential fakeCred4 = new DBCredential();
        fakeCred4.setName("APP.TestComponent.dev.testKey");
        fakeCred4.setComponent("TestComponent");
        fakeCred4.setSdlc("dev");
        fakeCred4.setVersion("0001");
        fakeCred4.setUpdatedBy("Ned Stark");
        fakeCred4.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred5 = new DBCredential();
        fakeCred5.setName("APP.dev.testKey2");
        fakeCred5.setSdlc("dev");
        fakeCred5.setVersion("0001");
        fakeCred5.setUpdatedBy("Ned Stark");
        fakeCred5.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred6 = new DBCredential();
        fakeCred6.setName("APP.dev.testKey3.extra");
        fakeCred6.setSdlc("testKey3");
        fakeCred6.setComponent("dev");
        fakeCred6.setVersion("0001");
        fakeCred6.setUpdatedBy("Ned Stark");
        fakeCred6.setUpdatedDate("2018-04-04T12:51:37.803Z");

        fakeData.add(fakeCred1);
        fakeData.add(fakeCred2);
        fakeData.add(fakeCred3);

        when(dynamoDBService.scanDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);
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
                null,
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

        assertEquals(results.get(0), expectedCreds.get(0));
    }

    @Test
    public void getAllCredentialsShouldBeAbleToMigrateCredentialsWithoutSDLC() {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred1 = new DBCredential();
        fakeCred1.setName("APP.TestComponent.dev.testKey");
        fakeCred1.setVersion("0001");
        fakeCred1.setUpdatedBy("Jon Snow");
        fakeCred1.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred2 = new DBCredential();
        fakeCred2.setName("APP.dev.testKey2");
        fakeCred2.setVersion("0001");
        fakeCred2.setUpdatedBy("Ned Stark");
        fakeCred2.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred3 = new DBCredential();
        fakeCred3.setName("APP.dev.testKey3.extra");
        fakeCred3.setVersion("0001");
        fakeCred3.setUpdatedBy("Ned Stark");
        fakeCred3.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred4 = new DBCredential();
        fakeCred4.setName("APP.TestComponent.dev.testKey");
        fakeCred4.setComponent("TestComponent");
        fakeCred4.setSdlc("dev");
        fakeCred4.setVersion("0001");
        fakeCred4.setUpdatedBy("Ned Stark");
        fakeCred4.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred5 = new DBCredential();
        fakeCred5.setName("APP.dev.testKey2");
        fakeCred5.setSdlc("dev");
        fakeCred5.setVersion("0001");
        fakeCred5.setUpdatedBy("Ned Stark");
        fakeCred5.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred6 = new DBCredential();
        fakeCred6.setName("APP.dev.testKey3.extra");
        fakeCred6.setSdlc("testKey3");
        fakeCred6.setComponent("dev");
        fakeCred6.setVersion("0001");
        fakeCred6.setUpdatedBy("Ned Stark");
        fakeCred6.setUpdatedDate("2018-04-04T12:51:37.803Z");

        fakeData.add(fakeCred1);
        fakeData.add(fakeCred2);
        fakeData.add(fakeCred3);

        when(dynamoDBService.scanDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);
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

        assertEquals(results, expectedCreds);
    }

    @Test
    public void getCredential() {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred1 = new DBCredential();
        fakeCred1.setName("APP.TestComponent.dev.testKey");
        fakeCred1.setVersion("0001");
        fakeCred1.setUpdatedBy("Jon Snow");
        fakeCred1.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred4 = new DBCredential();
        fakeCred4.setName("APP.TestComponent.dev.testKey");
        fakeCred4.setComponent("TestComponent");
        fakeCred4.setSdlc("dev");
        fakeCred4.setVersion("0001");
        fakeCred4.setUpdatedBy("Ned Stark");
        fakeCred4.setUpdatedDate("2018-04-04T12:51:37.803Z");

        fakeData.add(fakeCred1);

        when(dynamoDBService.queryDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);
        when(migrateService.migrateCredential(any(), any())).thenReturn(fakeCred4);

        Credential expectedCreds = new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "Ned Stark", "2018-04-04T12:51:37.803Z");

        Credential result = credentialsService.getCredential("some-account", "region", "APP", "APP.TestComponent.dev.testKey");

        assertEquals(result, expectedCreds);
    }

    @Test(expected = Exception.class)
    public void getCredentialFaliureToMigrate()throws Exception {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred1 = new DBCredential();
        fakeCred1.setName("APP.TestComponent.dev.testKey");
        fakeCred1.setVersion("0001");
        fakeCred1.setUpdatedBy("Jon Snow");
        fakeCred1.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred4 = new DBCredential();
        fakeCred4.setName("APP.TestComponent.dev.testKey");
        fakeCred4.setVersion("0001");
        fakeCred4.setUpdatedBy("Ned Stark");
        fakeCred4.setUpdatedDate("2018-04-04T12:51:37.803Z");

        fakeData.add(fakeCred1);

        when(dynamoDBService.queryDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);
        when(migrateService.migrateCredential(any(), any())).thenReturn(null);

        Credential expectedCreds = new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "Ned Stark", "2018-04-04T12:51:37.803Z");

        Credential result = credentialsService.getCredential("some-account", "region", "APP", "APP.TestComponent.dev.testKey");

        assertEquals(null, result);
    }


    @Test
    public void getCredentialFaliureToFindCredential() {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred1 = new DBCredential();
        fakeCred1.setName("APP.TestComponent.dev.testKey");
        fakeCred1.setVersion("0001");
        fakeCred1.setUpdatedBy("Jon Snow");
        fakeCred1.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred4 = new DBCredential();
        fakeCred4.setName("APP.TestComponent.dev.testKey");
        fakeCred4.setVersion("0001");
        fakeCred4.setUpdatedBy("Ned Stark");
        fakeCred4.setUpdatedDate("2018-04-04T12:51:37.803Z");

        fakeData.add(fakeCred1);

        when(dynamoDBService.scanDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);
        when(migrateService.migrateCredential(any(), any())).thenThrow(NoSuchElementException.class);

        Credential expectedCreds = new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "Ned Stark", "2018-04-04T12:51:37.803Z");

        Credential result = credentialsService.getCredential("some-account", "region", "APP", "APP.TestComponent.dev.testKey");

        assertEquals(result, null);
    }



    @Test
    public void getAllCredentialsShouldOnlyReturnLatestVersionOfCredentials() {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred1 = new DBCredential();
        fakeCred1.setName("APP.TestComponent.dev.testKey");
        fakeCred1.setVersion("0001");
        fakeCred1.setUpdatedBy("Jon Snow");
        fakeCred1.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred2 = new DBCredential();
        fakeCred2.setName("APP.TestComponent.dev.testKey");
        fakeCred2.setVersion("0002");
        fakeCred2.setUpdatedBy("Ned Stark");
        fakeCred2.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred3 = fakeCred2;
        fakeCred3.setSdlc("dev");
        fakeCred3.setComponent("TestComponent");

        fakeData.add(fakeCred1);
        fakeData.add(fakeCred2);

        when(dynamoDBService.scanDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);

        List<Credential> expectedCreds = new ArrayList<>();
        expectedCreds.add(new Credential("testKey", "APP.TestComponent.dev.testKey", "my_account","region", "APP","dev",
                "TestComponent", "Ned Stark", "2018-04-04T12:51:37.803Z"));

        assertTrue(credentialsService.getAllCredentials("table", "my_account", "region", "APP")
                .equals(expectedCreds));

    }

    @Test
    public void getCredentialHistoryShouldBeAbleToCorrectlyCreateAHistoryObjectFromObtainedData() {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred = new DBCredential();
        fakeCred.setName("APP.dev.TestComponent.testKey");
        fakeCred.setVersion("0001");
        fakeCred.setUpdatedBy("Jon Snow");
        fakeCred.setUpdatedDate("2018-04-04T12:51:37.803Z");

        fakeData.add(fakeCred);

        when(dynamoDBService.queryDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);

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
        Credential actual = credentialsService.getCredentialSecret(account, region, application, environment, component, shortKey);

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

        doThrow(new Exception("Not found.")).when(fideliusService).getCredential(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        Credential actual = credentialsService.getCredentialSecret(account, region, application, environment, component, shortKey);

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

    @Test(expected = FideliusException.class)
    public void createCredentialShouldNotCreateDuplicateCredentials() throws FideliusException {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred = new DBCredential();
        fakeCred.setName("APP.dev.testComponent.testKey");
        fakeCred.setVersion("0001");
        fakeCred.setUpdatedBy("Jon Snow");
        fakeCred.setUpdatedDate("2018-04-04T12:51:37.803Z");

        fakeData.add(fakeCred);

        Credential credential = new Credential();
        credential.setAccount("dev");
        credential.setRegion("us-east-1");
        credential.setApplication("APP");
        credential.setEnvironment("dev");
        credential.setComponent("testComponent");
        credential.setShortKey("testKey");
        credential.setSecret("secretPassword");

        when(dynamoDBService.queryDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);
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
    public void getAllCredentialsShortensFullIAMRoleARNs() {
        List<DBCredential> fakeData = new ArrayList<>();

        DBCredential fakeCred1 = new DBCredential();
        fakeCred1.setName("APP.TestComponent.dev.testKey");
        fakeCred1.setSdlc("dev");
        fakeCred1.setComponent("TestComponent");
        fakeCred1.setVersion("0001");
        fakeCred1.setUpdatedBy("arn:aws:sts::1234567890:assumed-role/private_aws_somerole_d/L25000");
        fakeCred1.setUpdatedDate("2018-04-04T12:51:37.803Z");

        DBCredential fakeCred2 = new DBCredential();
        fakeCred2.setName("APP.dev.testKey2");
        fakeCred2.setSdlc("dev");
        fakeCred2.setVersion("0001");
        fakeCred2.setUpdatedBy("Ned Stark");
        fakeCred2.setUpdatedDate("2018-04-04T12:51:37.803Z");

        fakeData.add(fakeCred1);
        fakeData.add(fakeCred2);

        when(dynamoDBService.scanDynamoDB(any(), eq(DBCredential.class), any())).thenReturn(fakeData);

        List<Credential> expectedCreds = new ArrayList<>();
        expectedCreds.add(new Credential("testKey2", "APP.dev.testKey2", "some-account","region", "APP", "dev",
                null, "Ned Stark", "2018-04-04T12:51:37.803Z"));
        expectedCreds.add(new Credential("testKey", "APP.TestComponent.dev.testKey",  "some-account", "region", "APP", "dev",
                "TestComponent", "private_aws_somerole_d/L25000", "2018-04-04T12:51:37.803Z"));

        assertTrue(credentialsService.getAllCredentials("table", "some-account", "region", "APP").equals(expectedCreds));
    }
}