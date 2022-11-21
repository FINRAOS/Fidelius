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

package org.finra.fidelius;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CredModelMapperTest {

    @Test
    public void testFromDynamo() {
        EncryptedCredential mappedCredential = CredModelMapper.fromDynamo(getTestDynamoRow());
        assertEquals(getTestCredential(), mappedCredential);
    }

    @Test
    public void testMetaFromDynamo() {
        Map<String, AttributeValue> dynamoRow = getTestDynamoRow();
        dynamoRow.put("name", AttributeValue.builder().s("META#" + NAME).build());
        dynamoRow.remove("contents");
        dynamoRow.remove("key");
        dynamoRow.remove("hmac");
        EncryptedCredential mappedCredential = CredModelMapper.fromDynamo(dynamoRow);

        EncryptedCredential expectedCredential = getTestCredential();
        expectedCredential.setFullName("META#" + NAME);
        expectedCredential.setCredential(null);
        expectedCredential.setDatakey(null);
        expectedCredential.setHmac(null);

        Assert.assertEquals(expectedCredential, mappedCredential);

    }

    @Test
    public void testToDynamo()  {
        Map<String, AttributeValue> mappedDynamoRow = CredModelMapper.toDynamo(getTestCredential());
        assertEquals(getTestDynamoRow(), mappedDynamoRow);
    }

    @Test
    public void testMetaToDynamo()  {
        EncryptedCredential metaCredential = getTestCredential();
        metaCredential.setFullName("META#" + NAME);
        metaCredential.setCredential(null);
        metaCredential.setDatakey(null);
        metaCredential.setHmac(null);

        Map<String, AttributeValue> mappedDynamoRow = CredModelMapper.toDynamo(metaCredential);
        Map<String, AttributeValue> expectedDynamoRow = getTestDynamoRow();
        expectedDynamoRow.put("name", AttributeValue.builder().s("META#" + NAME).build());
        expectedDynamoRow.remove("contents");
        expectedDynamoRow.remove("key");
        expectedDynamoRow.remove("hmac");
        assertEquals(expectedDynamoRow, mappedDynamoRow);
    }

    private Map<String, AttributeValue> getTestDynamoRow() {
        Map<String, AttributeValue> dynamoRow = new HashMap<>();
        dynamoRow.put("name", AttributeValue.builder().s(NAME).build());
        dynamoRow.put("version", AttributeValue.builder().s(VERSION).build());
        dynamoRow.put("key", AttributeValue.builder().s(KEY).build());
        dynamoRow.put("contents", AttributeValue.builder().s(CONTENTS).build());
        dynamoRow.put("hmac", AttributeValue.builder().s(HMAC).build());
        dynamoRow.put("updatedBy", AttributeValue.builder().s(UPDATED_BY).build());
        dynamoRow.put("updatedOn", AttributeValue.builder().s(UPDATED_ON).build());
        dynamoRow.put("sdlc", AttributeValue.builder().s(SDLC).build());
        dynamoRow.put("component", AttributeValue.builder().s(COMPONENT).build());

        return dynamoRow;
    }

    private EncryptedCredential getTestCredential() {
        return new EncryptedCredential()
                .setFullName(NAME)
                .setCredential(CONTENTS)
                .setVersion(VERSION)
                .setDatakey(KEY)
                .setHmac(HMAC)
                .setUpdateBy(UPDATED_BY)
                .setComponent(COMPONENT)
                .setUpdatedOn(UPDATED_ON)
                .setSdlc(SDLC);
    }

    private final String NAME = "AGS.component1.dev.credential";
    private final String VERSION = "0000000000000000001";
    private final String KEY = "wrapped_key";
    private final String CONTENTS = "encrypted_contents";
    private final String HMAC = "d23687d6f97d4a6e688f";
    private final String UPDATED_BY = "user_id";
    private final String UPDATED_ON = "2022-11-19T21:15:17.771Z";
    private final String SDLC = "dev";
    private final String COMPONENT = "component1";
}
