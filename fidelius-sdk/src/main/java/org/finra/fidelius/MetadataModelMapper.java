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


public class MetadataModelMapper {

    private enum DynamoAttributes{
        name, version, sourceType, source, updatedBy, updatedOn, sdlc, component
    }

    public static Map<String,AttributeValue> toDynamo(MetadataParameters metadataParameters){
        HashMap<String, AttributeValue> dynamoRow = new HashMap<>();
        dynamoRow.put(DynamoAttributes.name.name(), new AttributeValue(metadataParameters.getFullName()));
        dynamoRow.put(DynamoAttributes.version.name(), new AttributeValue(metadataParameters.getVersion()));
        dynamoRow.put(DynamoAttributes.sourceType.name(), new AttributeValue(metadataParameters.getSourceType()));
        dynamoRow.put(DynamoAttributes.source.name(), new AttributeValue(metadataParameters.getSource()));

        if(metadataParameters.getUpdateBy()!=null)
            dynamoRow.put(DynamoAttributes.updatedBy.name(), new AttributeValue(metadataParameters.getUpdateBy()));

        if(metadataParameters.getUpdateOn()!=null)
            dynamoRow.put(DynamoAttributes.updatedOn.name(), new AttributeValue(metadataParameters.getUpdateOn()));

        if(metadataParameters.getSdlc()!=null)
            dynamoRow.put(DynamoAttributes.sdlc.name(), new AttributeValue(metadataParameters.getSdlc()));

        if(metadataParameters.getComponent()!= null)
            dynamoRow.put(DynamoAttributes.component.name(), new AttributeValue(metadataParameters.getComponent()));

        return dynamoRow;
    }

    public static MetadataParameters fromDynamo(Map<String,AttributeValue> dynamoCred){
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
            return attributeValue.getS();
        }
        return null;
    }
}
