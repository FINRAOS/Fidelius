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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JCredStash.class})
@PowerMockIgnore( {"javax.management.*","javax.net.ssl.*",
        "javax.crypto.*", "jdk.xml.internal.*"})
public class JCredStashTest {

    private QueryResponse getMockQueryResult(int numberOfResults){

        Collection<Map<String, AttributeValue>> collection = new ArrayList<>();

        for (int i = 0; i < numberOfResults; i++) {
            Map<String, AttributeValue> map = new HashMap<>();
            map.put("updatedBy", AttributeValue.builder().s("arn:aws:sts::123456789876:assumed-role/private_aws_application_dev/L25000").build());
            map.put("contents", AttributeValue.builder().s("BvmeuWljKK9oMFDSTKHW10HWyw==").build());
            map.put("hmac", AttributeValue.builder().s("6416846cd12b6c45305fc3202092af143378809bf2c5378ab0a12a24d68ac19d==").build());
            map.put("name", AttributeValue.builder().s("APP.dev.loadtesting3913==").build());
            map.put("version", AttributeValue.builder().s("000000000000000000" + (i+1)).build());
            map.put("key", AttributeValue.builder().s("AQEBAHiR3vsV8dujB9GydJpKBtZhC3nKVikt90I4dcYRRv5e3wAAAKIwgZ8GCSqGSIb3DQEHBqCBkTCBjgIBADCBiAYJKoZIhvcNAQcBMB4GCWCGSAFlAwQBLjARBAw7QrBVksiA").build());
            collection.add(map);
        }

        QueryResponse queryResult = QueryResponse.builder()
                .items(collection)
                .count(numberOfResults)
                .scannedCount(numberOfResults)
                .build();

        return queryResult;
    }

    @Test
    public void testDeletingCredentialWith2Versions() throws Exception {

        QueryResponse queryResult = getMockQueryResult(2);
        DynamoDbClient amazonDynamoDBClient = spy(DynamoDbClient.class);
        JCredStash jCredStash = new JCredStash();
        BatchWriteItemResponse result = BatchWriteItemResponse.builder().unprocessedItems(new HashMap<>()).build();

        jCredStash.dynamoDbClient = amazonDynamoDBClient;

        doReturn(queryResult).when(amazonDynamoDBClient).query(any(QueryRequest.class));
        doReturn(result).when(amazonDynamoDBClient).batchWriteItem(any(BatchWriteItemRequest.class));

        jCredStash.deleteSecret("test", "secret");

        verify(amazonDynamoDBClient,times(1)).batchWriteItem(any(BatchWriteItemRequest.class));
    }

    @Test
    public void testDeletingCredentialWith1Versions() throws Exception {

        QueryResponse queryResult = getMockQueryResult(1);
        DynamoDbClient amazonDynamoDBClient = spy(DynamoDbClient.class);
        JCredStash jCredStash = new JCredStash();
        BatchWriteItemResponse result = BatchWriteItemResponse.builder().unprocessedItems(new HashMap<>()).build();

        jCredStash.dynamoDbClient = amazonDynamoDBClient;

        doReturn(queryResult).when(amazonDynamoDBClient).query(any(QueryRequest.class));
        doReturn(result).when(amazonDynamoDBClient).batchWriteItem(any(BatchWriteItemRequest.class));

        jCredStash.deleteSecret("test", "secret");

        verify(amazonDynamoDBClient,times(1)).batchWriteItem(any(BatchWriteItemRequest.class));
    }

    @Test(expected = RuntimeException.class)
    public void testDeletingCredentialFailed() throws Exception, InterruptedException {

        QueryResponse queryResult = getMockQueryResult(1);
        DynamoDbClient amazonDynamoDBClient = spy(DynamoDbClient.class);
        DynamoDbClient dynamoDB = mock(DynamoDbClient.class);
        JCredStash jCredStash = new JCredStash();
        //Add unprocessed item
        HashMap<String, List<WriteRequest>> unprocessedItem = new HashMap<>();
        unprocessedItem.put("secret", new ArrayList<>());
        BatchWriteItemResponse result = BatchWriteItemResponse.builder().unprocessedItems(unprocessedItem).build();

        jCredStash.dynamoDbClient = amazonDynamoDBClient;
        PowerMockito.mockStatic(Thread.class);

        doReturn(queryResult).when(amazonDynamoDBClient).query(any(QueryRequest.class));
        doReturn(result).when(dynamoDB).batchWriteItem(any(BatchWriteItemRequest.class));

        jCredStash.deleteSecret("test", "secret");

        verify(dynamoDB,times(1)).batchWriteItem(any(BatchWriteItemRequest.class));
    }


    @Test(expected = RuntimeException.class)
    public void testDeletingCredentialNotFound() throws Exception {

        QueryResponse queryResult = getMockQueryResult(0);
        DynamoDbClient amazonDynamoDBClient = spy(DynamoDbClient.class);
        JCredStash jCredStash = new JCredStash();
        jCredStash.dynamoDbClient = amazonDynamoDBClient;

        doReturn(queryResult).when(amazonDynamoDBClient).query(any(QueryRequest.class));
        doReturn(DeleteItemResponse.builder().build()).when(amazonDynamoDBClient).deleteItem(any(DeleteItemRequest.class));

        jCredStash.deleteSecret("test", "secret");
    }

    @Test
    public void getUpdatedByShouldReturnCurrentIAMUser() throws Exception{

        JCredStash jCredStash = new JCredStash();
        StsClient awsSecurityTokenService = spy(StsClient.class);
        GetCallerIdentityResponse callerIdentityResult = GetCallerIdentityResponse.builder()
                .arn("arn:aws:sts::123456789876:assumed-role/private_aws_application_dev/L25000")
                .build();
        jCredStash.stsClient = awsSecurityTokenService;

        doReturn(callerIdentityResult).when(awsSecurityTokenService).getCallerIdentity(any(GetCallerIdentityRequest.class));

        String user = jCredStash.getUpdatedBy();

        assert(user).equals("arn:aws:sts::123456789876:assumed-role/private_aws_application_dev/L25000");
    }

    @Test(expected = RuntimeException.class)
    public void returnErrorGettingUser() throws Exception{

        JCredStash jCredStash = new JCredStash();
        StsClient awsSecurityTokenService = spy(StsClient.class);
        GetCallerIdentityResponse callerIdentityResult = GetCallerIdentityResponse.builder()
                .arn("arn:aws:sts::123456789876:assumed-role/private_aws_application_dev/L25000")
                .build();
        jCredStash.stsClient = awsSecurityTokenService;

        doThrow(RuntimeException.class).when(awsSecurityTokenService).getCallerIdentity(any(GetCallerIdentityRequest.class));

        String user = jCredStash.getUpdatedBy();

        assert(user).equals("errorGettingUser");
    }


}
