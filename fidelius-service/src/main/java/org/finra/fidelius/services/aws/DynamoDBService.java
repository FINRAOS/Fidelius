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
import org.finra.fidelius.model.aws.AWSEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sts.model.StsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DynamoDBService {

    @Inject
    private AWSSessionService awsSessionService;

    private Logger logger = LoggerFactory.getLogger(DynamoDBService.class);

    public List<Map<String, AttributeValue>> scanDynamoDB(ScanRequest scanRequest, DynamoDbClient dynamoDbClient) {
        logger.info("Scanning DynamoDB table...");
        List<Map<String, AttributeValue>> queryResults = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        Map<String, AttributeValue> lastEvaluatedKey = null;
        do {
            try {
                ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
                queryResults.addAll(scanResponse.items());
                lastEvaluatedKey = scanResponse.lastEvaluatedKey();
                scanRequest = ScanRequest.builder()
                        .tableName(scanRequest.tableName())
                        .filterExpression(scanRequest.filterExpression())
                        .exclusiveStartKey(scanResponse.lastEvaluatedKey())
                        .expressionAttributeNames(scanRequest.expressionAttributeNames())
                        .expressionAttributeValues(scanRequest.expressionAttributeValues())
                        .build();
            } catch (ProvisionedThroughputExceededException pte) {
                logger.error("Provisioned Throughput Exceeded. ", pte);
            } catch (ResourceNotFoundException rnf) {
                String message = "Credential table not found!";
                logger.error(message, rnf);
                throw new FideliusException(message, HttpStatus.NOT_FOUND);
            }
        } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

        logger.info(String.format("Scan completed in %.3f seconds", (System.currentTimeMillis() - startTime) / 1000.0));
        return queryResults;
    }

    public List<Map<String, AttributeValue>> queryDynamoDB(QueryRequest queryRequest, DynamoDbClient dynamoDbClient){
        QueryResponse queryResults = null;
        logger.info("Querying DynamoDB table...");
        long startTime = System.currentTimeMillis();
        try {
                queryResults = dynamoDbClient.query(queryRequest);
            } catch (ProvisionedThroughputExceededException pte) {
                logger.error("Provisioned Throughput Exceeded. ", pte);
            } catch (ResourceNotFoundException rnf) {
                String message = "Credential table not found!";
                logger.error(message, rnf);
                throw new FideliusException(message, HttpStatus.NOT_FOUND);
            }

        if (queryResults == null) {
            logger.error("Throttling rate exceeded!");
            throw new FideliusException("Throttling rate exceeded!", HttpStatus.REQUEST_TIMEOUT);
        } else {
            logger.info(String.format("Query completed in %.3f seconds", (System.currentTimeMillis() - startTime) / 1000.0));
        }

        return queryResults.items();
    }
}
