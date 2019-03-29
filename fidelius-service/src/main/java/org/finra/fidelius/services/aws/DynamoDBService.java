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
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import org.finra.fidelius.exceptions.FideliusException;
import org.finra.fidelius.model.aws.AWSEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class DynamoDBService {

    @Inject
    private AWSSessionService awsSessionService;

    private Logger logger = LoggerFactory.getLogger(DynamoDBService.class);

    public <T> List<T> scanDynamoDB(DynamoDBScanExpression scanExp, Class<T> clazz, DynamoDBMapper mapper) {
        logger.info("Scanning DynamoDB table...");
        List<T> queryResults = null;
        long startTime = System.currentTimeMillis();
        try {
                PaginatedScanList<T> scanResults = mapper.scan(clazz, scanExp);
                queryResults = new ArrayList<>(scanResults);
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
            logger.info(String.format("Scan completed in %.3f seconds", (System.currentTimeMillis() - startTime) / 1000.0));
        }

        return queryResults;
    }

    public <T>List<T> queryDynamoDB(DynamoDBQueryExpression queryRequest, Class<T> clazz, DynamoDBMapper dynamoDBMapper){
        List<T> queryResults = null;
        logger.info("Querying DynamoDB table...");
        long startTime = System.currentTimeMillis();
        try {
                queryResults = dynamoDBMapper.query(clazz, queryRequest);
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

        return queryResults;
    }

    // Creates a new DynamoDBMapper object
    public DynamoDBMapper createMapper(String account, String region, String tableName) {
        AWSEnvironment awsenv = new AWSEnvironment(account, region);

        AmazonDynamoDBClient dbclient;
        try {
            dbclient = awsSessionService.getDynamoDBClient(awsenv);
        } catch (AWSSecurityTokenServiceException ex) {
            String message = String.format("User not authorized to access credential table on account: %s in region: %s", account, region);
            logger.error(message, ex);
            throw new FideliusException(message, HttpStatus.FORBIDDEN);
        } catch (RuntimeException re) {
            String message = re.getMessage();
            logger.error(message, re);
            throw new FideliusException(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
                .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName))
                .build();
        return new DynamoDBMapper(dbclient, config);
    }
}
