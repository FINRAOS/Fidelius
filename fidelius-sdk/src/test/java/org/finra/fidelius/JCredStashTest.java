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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.BatchWriteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JCredStash.class})
@PowerMockIgnore( {"javax.management.*","javax.net.ssl.*"})
public class JCredStashTest {

    private QueryResult getMockQueryResult(int numberOfResults){

        Collection<Map<String,AttributeValue>> collection = new ArrayList<>();

        for (int i = 0; i < numberOfResults; i++) {
            Map<String, AttributeValue> map = new HashMap<>();
            map.put("updatedBy", new AttributeValue("arn:aws:sts::123456789876:assumed-role/private_aws_application_dev/L25000"));
            map.put("contents", new AttributeValue("BvmeuWljKK9oMFDSTKHW10HWyw=="));
            map.put("hmac", new AttributeValue("6416846cd12b6c45305fc3202092af143378809bf2c5378ab0a12a24d68ac19d=="));
            map.put("name", new AttributeValue("APP.dev.loadtesting3913=="));
            map.put("version", new AttributeValue("000000000000000000" + (i+1)));
            map.put("key", new AttributeValue("AQEBAHiR3vsV8dujB9GydJpKBtZhC3nKVikt90I4dcYRRv5e3wAAAKIwgZ8GCSqGSIb3DQEHBqCBkTCBjgIBADCBiAYJKoZIhvcNAQcBMB4GCWCGSAFlAwQBLjARBAw7QrBVksiA"));
            collection.add(map);
        }

        QueryResult queryResult = new QueryResult();
        queryResult.setItems(collection);
        queryResult.setCount(numberOfResults);
        queryResult.setScannedCount(numberOfResults);

        return queryResult;
    }

    @Test
    public void testDeletingCredentialWith2Versions() throws Exception {

        QueryResult queryResult = getMockQueryResult(2);
        AmazonDynamoDBClient amazonDynamoDBClient = spy(AmazonDynamoDBClient.class);
        DynamoDB dynamoDB = mock(DynamoDB.class);
        JCredStash jCredStash = new JCredStash();
        BatchWriteItemResult result = new BatchWriteItemResult().withUnprocessedItems(new HashMap<>());
        BatchWriteItemOutcome outcome = new BatchWriteItemOutcome(result);

        jCredStash.amazonDynamoDBClient = amazonDynamoDBClient;
        jCredStash.dynamoDB = dynamoDB;

        doReturn(queryResult).when(amazonDynamoDBClient).query(anyObject());
        doReturn(outcome).when(dynamoDB).batchWriteItem(any(TableWriteItems.class));

        jCredStash.deleteSecret("test", "secret");

        verify(dynamoDB,times(1)).batchWriteItem(any(TableWriteItems.class));
    }

    @Test
    public void testDeletingCredentialWith1Versions() throws Exception {

        QueryResult queryResult = getMockQueryResult(1);
        AmazonDynamoDBClient amazonDynamoDBClient = spy(AmazonDynamoDBClient.class);
        DynamoDB dynamoDB = mock(DynamoDB.class);
        JCredStash jCredStash = new JCredStash();
        BatchWriteItemResult result = new BatchWriteItemResult().withUnprocessedItems(new HashMap<>());
        BatchWriteItemOutcome outcome = new BatchWriteItemOutcome(result);

        jCredStash.amazonDynamoDBClient = amazonDynamoDBClient;
        jCredStash.dynamoDB = dynamoDB;

        doReturn(queryResult).when(amazonDynamoDBClient).query(anyObject());
        doReturn(outcome).when(dynamoDB).batchWriteItem(any(TableWriteItems.class));

        jCredStash.deleteSecret("test", "secret");

        verify(dynamoDB,times(1)).batchWriteItem(any(TableWriteItems.class));
    }

    @Test(expected = RuntimeException.class)
    public void testDeletingCredentialFailed() throws Exception, InterruptedException {

        QueryResult queryResult = getMockQueryResult(1);
        AmazonDynamoDBClient amazonDynamoDBClient = spy(AmazonDynamoDBClient.class);
        DynamoDB dynamoDB = mock(DynamoDB.class);
        JCredStash jCredStash = new JCredStash();
        //Add unprocessed item
        HashMap<String, List<WriteRequest>> unprocessedItem = new HashMap<>();
        unprocessedItem.put("secret", new ArrayList<>());
        BatchWriteItemResult result = new BatchWriteItemResult().withUnprocessedItems(unprocessedItem);
        BatchWriteItemOutcome outcome = new BatchWriteItemOutcome(result);

        jCredStash.amazonDynamoDBClient = amazonDynamoDBClient;
        jCredStash.dynamoDB = dynamoDB;
        PowerMockito.mockStatic(Thread.class);

        doReturn(queryResult).when(amazonDynamoDBClient).query(anyObject());
        doReturn(outcome).when(dynamoDB).batchWriteItem(any(TableWriteItems.class));

        jCredStash.deleteSecret("test", "secret");

        verify(dynamoDB,times(1)).batchWriteItem(any(TableWriteItems.class));
    }


    @Test(expected = RuntimeException.class)
    public void testDeletingCredentialNotFound() throws Exception {

        QueryResult queryResult = getMockQueryResult(0);
        AmazonDynamoDBClient amazonDynamoDBClient = spy(AmazonDynamoDBClient.class);
        JCredStash jCredStash = new JCredStash();
        jCredStash.amazonDynamoDBClient = amazonDynamoDBClient;

        doReturn(queryResult).when(amazonDynamoDBClient).query(anyObject());
        doReturn(new DeleteItemResult()).when(amazonDynamoDBClient).deleteItem(anyObject());

        jCredStash.deleteSecret("test", "secret");
    }

    @Test
    public void getUpdatedByShouldReturnCurrentIAMUser() throws Exception{

        JCredStash jCredStash = new JCredStash();
        AWSSecurityTokenService awsSecurityTokenService = spy(AWSSecurityTokenService.class);
        GetCallerIdentityResult callerIdentityResult = new GetCallerIdentityResult();
        callerIdentityResult.setArn("arn:aws:sts::123456789876:assumed-role/private_aws_application_dev/L25000");
        jCredStash.awsSecurityTokenService = awsSecurityTokenService;

        doReturn(callerIdentityResult).when(awsSecurityTokenService).getCallerIdentity(anyObject());

        String user = jCredStash.getUpdatedBy();

        assert(user).equals("arn:aws:sts::123456789876:assumed-role/private_aws_application_dev/L25000");
    }

    @Test(expected = RuntimeException.class)
    public void returnErrorGettingUser() throws Exception{

        JCredStash jCredStash = new JCredStash();
        AWSSecurityTokenService awsSecurityTokenService = spy(AWSSecurityTokenService.class);
        GetCallerIdentityResult callerIdentityResult = new GetCallerIdentityResult();
        callerIdentityResult.setArn("arn:aws:sts::123456789876:assumed-role/private_aws_application_dev/L25000");

        doReturn(callerIdentityResult).when(awsSecurityTokenService).getCallerIdentity(anyObject());

        String user = jCredStash.getUpdatedBy();

        assert(user).equals("errorGettingUser");
    }


}
