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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;


public class CredModelMapper {

    private enum DynamoAttributes{
        name, version, key, contents, hmac, updatedBy, updatedOn, sdlc, component, source, sourceType
    }

    public static Map<String,AttributeValue> toDynamo(EncryptedCredential encryptedCredential){
        HashMap<String, AttributeValue> dynamoRow = new HashMap<>();
        dynamoRow.put(DynamoAttributes.name.name(), AttributeValue.builder().s(encryptedCredential.getFullName()).build());
        dynamoRow.put(DynamoAttributes.version.name(), AttributeValue.builder().s(encryptedCredential.getVersion()).build());

        if(encryptedCredential.getDatakey()!=null)
            dynamoRow.put(DynamoAttributes.key.name(), AttributeValue.builder().s(encryptedCredential.getDatakey()).build());

        if(encryptedCredential.getCredential()!=null)
            dynamoRow.put(DynamoAttributes.contents.name(), AttributeValue.builder().s(encryptedCredential.getCredential()).build());

        if(encryptedCredential.getHmac()!=null)
            dynamoRow.put(DynamoAttributes.hmac.name(), AttributeValue.builder().s(encryptedCredential.getHmac()).build());

        if(encryptedCredential.getUpdateBy()!=null)
            dynamoRow.put(DynamoAttributes.updatedBy.name(), AttributeValue.builder().s(encryptedCredential.getUpdateBy()).build());

        if(encryptedCredential.getUpdateOn()!=null)
            dynamoRow.put(DynamoAttributes.updatedOn.name(), AttributeValue.builder().s(encryptedCredential.getUpdateOn()).build());

        if(encryptedCredential.getSdlc()!=null)
            dynamoRow.put(DynamoAttributes.sdlc.name(), AttributeValue.builder().s(encryptedCredential.getSdlc()).build());

        if(encryptedCredential.getComponent()!= null)
            dynamoRow.put(DynamoAttributes.component.name(), AttributeValue.builder().s(encryptedCredential.getComponent()).build());

        return dynamoRow;
    }

    public static EncryptedCredential fromDynamo(Map<String, AttributeValue> dynamoCred){
        return new EncryptedCredential()
                                .setFullName(getAttributeValue(DynamoAttributes.name.name(), dynamoCred))
                                .setCredential(getAttributeValue(DynamoAttributes.contents.name(),dynamoCred))
                                .setVersion(getAttributeValue(DynamoAttributes.version.name(),dynamoCred))
                                .setDatakey(getAttributeValue(DynamoAttributes.key.name(),dynamoCred))
                                .setHmac(getAttributeValue(DynamoAttributes.hmac.name(),dynamoCred))
                                .setUpdateBy(getAttributeValue(DynamoAttributes.updatedBy.name(),dynamoCred))
                                .setComponent(getAttributeValue(DynamoAttributes.component.name(),dynamoCred))
                                .setUpdatedOn(getAttributeValue(DynamoAttributes.updatedOn.name(),dynamoCred))
                                .setSdlc(getAttributeValue(DynamoAttributes.sdlc.name(),dynamoCred));
    }

    private static String getAttributeValue(String name, Map<String,AttributeValue> dynamoCred){
        AttributeValue attributeValue = dynamoCred.get(name);
        if(attributeValue!=null){
            return attributeValue.s();
        }
        return null;
    }
}
