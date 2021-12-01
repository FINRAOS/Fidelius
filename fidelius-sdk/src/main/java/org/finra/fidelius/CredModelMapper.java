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

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;


public class CredModelMapper {

    private enum DynamoAttributes{
        name, version, key, contents, hmac, updatedBy, updatedOn, sdlc, component, source, sourceType
    }

    public static Map<String,AttributeValue> toDynamo(EncryptedCredential encryptedCredential){
        HashMap<String, AttributeValue> dynamoRow = new HashMap<>();
        dynamoRow.put(DynamoAttributes.name.name(), new AttributeValue(encryptedCredential.getFullName()));
        dynamoRow.put(DynamoAttributes.version.name(), new AttributeValue(encryptedCredential.getVersion()));
        dynamoRow.put(DynamoAttributes.key.name(), new AttributeValue(encryptedCredential.getDatakey()));
        dynamoRow.put(DynamoAttributes.contents.name(), new AttributeValue(encryptedCredential.getCredential()));
        dynamoRow.put(DynamoAttributes.hmac.name(), new AttributeValue(encryptedCredential.getHmac()));

        if(encryptedCredential.getUpdateBy()!=null)
            dynamoRow.put(DynamoAttributes.updatedBy.name(), new AttributeValue(encryptedCredential.getUpdateBy()));

        if(encryptedCredential.getUpdateOn()!=null)
            dynamoRow.put(DynamoAttributes.updatedOn.name(), new AttributeValue(encryptedCredential.getUpdateOn()));

        if(encryptedCredential.getSdlc()!=null)
            dynamoRow.put(DynamoAttributes.sdlc.name(), new AttributeValue(encryptedCredential.getSdlc()));

        if(encryptedCredential.getComponent()!= null)
            dynamoRow.put(DynamoAttributes.component.name(), new AttributeValue(encryptedCredential.getComponent()));

        return dynamoRow;
    }

    public static EncryptedCredential fromDynamo(Map<String,AttributeValue> dynamoCred){
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
            return attributeValue.getS();
        }
        return null;
    }
}
