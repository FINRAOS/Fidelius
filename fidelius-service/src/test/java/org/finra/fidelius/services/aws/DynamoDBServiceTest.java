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

package org.finra.fidelius.services.aws;

import org.finra.fidelius.exceptions.FideliusException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DynamoDBServiceTest {

    @Mock
    private AWSSessionService awsSessionService;

    @InjectMocks
    private DynamoDBService dynamoDBService;

    @Mock
    private DynamoDbClient dynamoDbClient;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = FideliusException.class)
    public void scanDynamoDBFailsAfterIntervalReaches60SecondsWhenRetryingOnThrottlingException() {
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenThrow(ProvisionedThroughputExceededException.builder().message("test").build());
        dynamoDBService.scanDynamoDB(ScanRequest.builder().build(), dynamoDbClient);
    }

    @Test(expected = FideliusException.class)
    public void queryDynamoDBFailsAfterIntervalReaches60SecondsWhenRetryingOnThrottlingException() {
        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(ProvisionedThroughputExceededException.builder().message("test").build());
        dynamoDBService.queryDynamoDB(QueryRequest.builder().build(), dynamoDbClient);
    }

}
