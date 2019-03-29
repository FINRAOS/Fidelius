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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import org.finra.fidelius.FideliusClient;
import org.finra.fidelius.exceptions.FideliusException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DynamoDBServiceTest {

    @Mock
    private AWSSessionService awsSessionService;

    @InjectMocks
    private DynamoDBService dynamoDBService;

    @Mock
    private DynamoDBMapper fakeMapper;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = Exception.class)
    public void createMapperShouldFailIfDBClientFailsToCreate() throws Exception {
        when(awsSessionService.getDynamoDBClient(any())).thenThrow(new Exception());
        dynamoDBService.createMapper("BAD_ACCOUNT_NAME", "bad_region", "table");
    }

    @Test
    public void createMapperShouldCompleteIfDBClientCreatesSuccessfully() throws Exception {
        when(awsSessionService.getDynamoDBClient(any())).thenReturn(new AmazonDynamoDBClient());
        dynamoDBService.createMapper("some_account", "some_region", "table");
    }

    @Test(expected = FideliusException.class)
    public void scanDynamoDBFailsAfterIntervalReaches60SecondsWhenRetryingOnThrottlingException() {
        when(fakeMapper.scan(any(), any())).thenThrow(new ProvisionedThroughputExceededException("test"));
        dynamoDBService.scanDynamoDB(new DynamoDBScanExpression(), Object.class, fakeMapper);
    }

    @Test(expected = FideliusException.class)
    public void createMapperShouldThrowFideliusExceptionIfCredentialAccessIsDenied() {
        when(awsSessionService.getDynamoDBClient(any())).thenThrow(new AWSSecurityTokenServiceException("Access Denied"));
        dynamoDBService.createMapper("some_account", "some_region", "table");
    }

    @Test(expected = FideliusException.class)
    public void queryDynamoDBFailsAfterIntervalReaches60SecondsWhenRetryingOnThrottlingException() {
        when(fakeMapper.query(any(), any())).thenThrow(new ProvisionedThroughputExceededException("test"));
        dynamoDBService.queryDynamoDB(new DynamoDBQueryExpression(), Object.class, fakeMapper);
    }

}