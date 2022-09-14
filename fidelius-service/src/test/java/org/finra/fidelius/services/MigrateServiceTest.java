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
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.kms.KmsClient;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Name of DynamoDb table that contains credentials
     */
    @Value("${fidelius.dynamoTable}")
    private String tableName;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(awsSessionService.getDynamoDBClient(any())).thenReturn(DynamoDbClient.builder().build());
        when(awsSessionService.getKmsClient(any())).thenReturn(KmsClient.builder().build());
    }

    @Test
    public void migrateCredentialWith3Fields() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev.key").build());

        doReturn("correct").when(fideliusService).getCredential("key", "APP", "dev", null, tableName, "FideliusMigrateTask");

        Map<String, AttributeValue> result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
    }

    @Test
    public void migrateCredentialWith3FieldsAndSpecialCharacter() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev.<user-id>").build());

        doReturn("correct").when(fideliusService).getCredential("<user-id>", "APP", "dev", null, tableName, "FideliusMigrateTask");

        Map<String, AttributeValue> result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
    }

    @Test
    public void guessCredentialWith3Fields() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev.key").build());

        Map<String, AttributeValue> result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
        Assert.assertEquals("key", CredentialsService.getShortKey(result));

    }

    @Test
    public void guessCredentialWith3FieldsAndSpecialCharacters() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev-int.<key#").build());

        Map<String, AttributeValue> result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev-int", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
        Assert.assertEquals("<key#", CredentialsService.getShortKey(result));
    }

    @Test
    public void migrateCredentialWith3FieldsShouldFail() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev.key").build());

        Map<String, AttributeValue> result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertNull(result.get("sdlc"));
        Assert.assertNull(result.get("component"));
    }

    @Test
    public void migrateCredentialWith4FieldsAndNoComponent() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev.secret.key").build());

        doReturn("correct").when(fideliusService).getCredential("secret.key", "APP", "dev", null, tableName, "FideliusMigrateTask");

        Map<String, AttributeValue> result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
    }

    @Test
    public void migrateCredentialWith4FieldsAndComponent() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.component.dev.key").build());

        doReturn("correct").when(fideliusService).getCredential("key", "APP", "dev", "component", tableName, "FideliusMigrateTask");

        Map<String, AttributeValue> result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.get("sdlc").s());
        Assert.assertEquals("component", result.get("component").s());
    }

    @Test
    public void migrateCredentialWith4FieldsAndComponentShouldBeNull() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.component.dev.key").build());

        Map<String, AttributeValue> result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertNull(result.get("sdlc"));
        Assert.assertNull(result.get("component"));
    }

    @Test
    public void guessCredentialWith4Fields() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.component.dev.key").build());

        Map<String, AttributeValue> result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("component", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
        Assert.assertEquals("dev.key", CredentialsService.getShortKey(result));

    }

    @Test
    public void guessCredentialWith4FieldsAndSpecialCharacters() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev-int.component.<key>").build());

        Map<String, AttributeValue> result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev-int", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
        Assert.assertEquals("component.<key>", CredentialsService.getShortKey(result));
    }

    @Test
    public void migrateCredentialWithMoreThan4FieldsAndNoComponent() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev.secret.long.key").build());

        doReturn("correct").when(fideliusService).getCredential("secret.long.key", "APP", "dev", null, tableName, "FideliusMigrateTask");

        Map<String, AttributeValue> result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
    }

    @Test
    public void migrateCredentialWithMoreThan4FieldsAndNoComponentSpecialCharacters() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev-int.secret.'long.<key*").build());

        doReturn("correct").when(fideliusService).getCredential("secret.'long.<key*", "APP", "dev-int", null, tableName, "FideliusMigrateTask");

        Map<String, AttributeValue> result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev-int", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
    }


    @Test
    public void migrateCredentialWithMoreThan4FieldsAndComponent() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.component.dev.secret.long.key").build());

        doReturn("correct").when(fideliusService).getCredential("secret.long.key", "APP", "dev", "component", tableName, "FideliusMigrateTask");

        Map<String, AttributeValue> result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev", result.get("sdlc").s());
        Assert.assertEquals("component", result.get("component").s());
    }

    @Test
    public void migrateCredentialWithMoreThan4FieldsAndComponentSpecialCharacters() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.component.dev-int.secret.long.<key>").build());

        doReturn("correct").when(fideliusService).getCredential("secret.long.<key>", "APP", "dev-int", "component", tableName, "FideliusMigrateTask");

        Map<String, AttributeValue> result = migrateService.migrateCredential(dbCredential, fideliusService);

        Assert.assertEquals("dev-int", result.get("sdlc").s());
        Assert.assertEquals("component", result.get("component").s());
    }

    @Test
    public void guessCredentialWithMoreThan4Fields() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev.secret.long.key").build());

        Map<String, AttributeValue> result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
        Assert.assertEquals("secret.long.key", CredentialsService.getShortKey(result));

    }

    @Test
    public void guessCredentialWithMoreThan4FieldsAndCharacters() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev-int.secret.long.<key*").build());

        Map<String, AttributeValue> result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev-int", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
        Assert.assertEquals("secret.long.<key*", CredentialsService.getShortKey(result));

    }

    @Test
    public void guessCredentialWith3FieldsAndSpecialCharactersOnSDLC() throws Exception {
        Map<String, AttributeValue> dbCredential = new HashMap<>();
        dbCredential.put("name", AttributeValue.builder().s("APP.dev-int*.secret").build());

        Map<String, AttributeValue> result = migrateService.guessCredentialProperties(dbCredential);

        Assert.assertEquals("dev-int*", result.get("sdlc").s());
        Assert.assertNull(result.get("component"));
        Assert.assertEquals("APP.dev-int*.secret", CredentialsService.getShortKey(result));

    }
}
