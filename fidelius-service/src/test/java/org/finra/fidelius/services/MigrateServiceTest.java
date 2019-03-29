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
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.kms.AWSKMSClient;
import org.finra.fidelius.model.db.DBCredential;
import org.finra.fidelius.services.aws.AWSSessionService;
import org.finra.fidelius.services.aws.DynamoDBService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Value;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MigrateServiceTest {

    @InjectMocks
    private MigrateService migrateService;

    @Mock
    private FideliusService fideliusService;

    @Mock
    private AWSSessionService awsSessionService;

    @Mock
    private DynamoDBService dynamoDBService;

    @Mock
    private DynamoDBMapper mapper;

    /**
     * Name of DynamoDb table that contains credentials
     */
    @Value("${fidelius.dynamoTable}")
    private String tableName;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(awsSessionService.getDynamoDBClient(any())).thenReturn(new AmazonDynamoDBClient());
        when(awsSessionService.getKmsClient(any())).thenReturn(new AWSKMSClient());
    }

    @Test
    public void migrateCredentialWith3Fields() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev.key");

        doReturn("correct").when(fideliusService).getCredential("key", "APP", "dev", null, tableName, "FideliusMigrateTask");

        DBCredential result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
    }

    @Test
    public void migrateCredentialWith3FieldsAndSpecialCharacter() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev.<user-id>");

        doReturn("correct").when(fideliusService).getCredential("<user-id>", "APP", "dev", null, tableName, "FideliusMigrateTask");

        DBCredential result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
    }

    @Test
    public void guessCredentialWith3Fields() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev.key");

        DBCredential result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
        Assert.assertEquals("key", result.getShortKey());

    }

    @Test
    public void guessCredentialWith3FieldsAndSpecialCharacters() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev-int.<key#");

        DBCredential result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev-int", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
        Assert.assertEquals("<key#", result.getShortKey());
    }

    @Test
    public void migrateCredentialWith3FieldsShouldFail() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev.key");

        DBCredential result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals(null, result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
    }

    @Test
    public void migrateCredentialWith4FieldsAndNoComponent() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev.secret.key");

        doReturn("correct").when(fideliusService).getCredential("secret.key", "APP", "dev", null, tableName, "FideliusMigrateTask");

        DBCredential result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
    }

    @Test
    public void migrateCredentialWith4FieldsAndComponent() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.component.dev.key");

        doReturn("correct").when(fideliusService).getCredential("key", "APP", "dev", "component", tableName, "FideliusMigrateTask");

        DBCredential result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.getSdlc());
        Assert.assertEquals("component", result.getComponent());
    }

    @Test
    public void migrateCredentialWith4FieldsAndComponentShouldBeNull() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.component.dev.key");

        DBCredential result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals(null, result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
    }

    @Test
    public void guessCredentialWith4Fields() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.component.dev.key");

        DBCredential result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("component", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
        Assert.assertEquals("dev.key", result.getShortKey());

    }

    @Test
    public void guessCredentialWith4FieldsAndSpecialCharacters() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev-int.component.<key>");

        DBCredential result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev-int", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
        Assert.assertEquals("component.<key>", result.getShortKey());

    }

    @Test
    public void migrateCredentialWithMoreThan4FieldsAndNoComponent() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev.secret.long.key");

        doReturn("correct").when(fideliusService).getCredential("secret.long.key", "APP", "dev", null, tableName, "FideliusMigrateTask");

        DBCredential result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
    }

    @Test
    public void migrateCredentialWithMoreThan4FieldsAndNoComponentSpecialCharacters() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev-int.secret.'long.<key*");

        doReturn("correct").when(fideliusService).getCredential("secret.'long.<key*", "APP", "dev-int", null, tableName, "FideliusMigrateTask");

        DBCredential result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev-int", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
    }


    @Test
    public void migrateCredentialWithMoreThan4FieldsAndComponent() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.component.dev.secret.long.key");

        doReturn("correct").when(fideliusService).getCredential("secret.long.key", "APP", "dev", "component", tableName, "FideliusMigrateTask");

        DBCredential result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.getSdlc());
        Assert.assertEquals("component", result.getComponent());
    }

    @Test
    public void migrateCredentialWithMoreThan4FieldsAndComponentSpecialCharacters() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.component.dev-int.secret.long.<key>");

        doReturn("correct").when(fideliusService).getCredential("secret.long.<key>", "APP", "dev-int", "component", tableName, "FideliusMigrateTask");

        DBCredential result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev-int", result.getSdlc());
        Assert.assertEquals("component", result.getComponent());
    }

    @Test
    public void guessCredentialWithMoreThan4Fields() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev.secret.long.key");

        DBCredential result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
        Assert.assertEquals("secret.long.key", result.getShortKey());

    }

    @Test
    public void guessCredentialWithMoreThan4FieldsAndCharacters() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev-int.secret.long.<key*");

        DBCredential result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev-int", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
        Assert.assertEquals("secret.long.<key*", result.getShortKey());

    }

    @Test
    public void guessCredentialWith3FieldsAndSpecialCharactersOnSDLC() throws Exception {
        DBCredential dbCredential = new DBCredential();
        dbCredential.setName("APP.dev-int*.secret");

        DBCredential result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev-int*", result.getSdlc());
        Assert.assertEquals(null, result.getComponent());
        Assert.assertEquals("APP.dev-int*.secret", result.getShortKey());

    }
}