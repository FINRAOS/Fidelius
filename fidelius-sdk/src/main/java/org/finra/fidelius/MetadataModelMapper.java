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


public class MetadataModelMapper {

    private enum DynamoAttributes{
        name, version, sourceType, source, updatedBy, updatedOn, sdlc, component
    }

    public static Map<String,AttributeValue> toDynamo(MetadataParameters metadataParameters){
        HashMap<String, AttributeValue> dynamoRow = new HashMap<>();
        dynamoRow.put(DynamoAttributes.name.name(), AttributeValue.builder().s(metadataParameters.getFullName()).build());
        dynamoRow.put(DynamoAttributes.version.name(), AttributeValue.builder().s(metadataParameters.getVersion()).build());
        dynamoRow.put(DynamoAttributes.sourceType.name(), AttributeValue.builder().s(metadataParameters.getSourceType()).build());
        dynamoRow.put(DynamoAttributes.source.name(), AttributeValue.builder().s(metadataParameters.getSource()).build());

        if(metadataParameters.getUpdateBy()!=null)
            dynamoRow.put(DynamoAttributes.updatedBy.name(), AttributeValue.builder().s(metadataParameters.getUpdateBy()).build());

        if(metadataParameters.getUpdateOn()!=null)
            dynamoRow.put(DynamoAttributes.updatedOn.name(), AttributeValue.builder().s(metadataParameters.getUpdateOn()).build());

        if(metadataParameters.getSdlc()!=null)
            dynamoRow.put(DynamoAttributes.sdlc.name(), AttributeValue.builder().s(metadataParameters.getSdlc()).build());

        if(metadataParameters.getComponent()!= null)
            dynamoRow.put(DynamoAttributes.component.name(), AttributeValue.builder().s(metadataParameters.getComponent()).build());

        return dynamoRow;
    }

    public static MetadataParameters fromDynamo(Map<String, AttributeValue> dynamoCred){
        return new MetadataParameters()
                .setFullName(getAttributeValue(DynamoAttributes.name.name(), dynamoCred))
                .setVersion(getAttributeValue(DynamoAttributes.version.name(),dynamoCred))
                .setSourceType(getAttributeValue(DynamoAttributes.sourceType.name(),dynamoCred))
                .setSource(getAttributeValue(DynamoAttributes.source.name(),dynamoCred))
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
