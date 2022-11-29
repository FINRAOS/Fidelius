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

package org.finra.fidelius.factories;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sts.StsClient;

import javax.inject.Inject;
import java.time.Duration;

@Component
public class AWSSessionFactory {

    @Inject
    private ClientOverrideConfiguration clientConfiguration;

    public DynamoDbClient createDynamoDBClient(AwsCredentialsProvider awsCredentialsProvider, Region region) {
        FullJitterBackoffStrategy backoffStrategy = FullJitterBackoffStrategy.builder().baseDelay(Duration.ofMillis(100)).maxBackoffTime(Duration.ofMillis(1000)).build();
        RetryPolicy retryPolicy = RetryPolicy.builder().numRetries(5).backoffStrategy(backoffStrategy).throttlingBackoffStrategy(backoffStrategy).build();
        ClientOverrideConfiguration clientOverrideConfiguration = ClientOverrideConfiguration.builder().retryPolicy(retryPolicy).build();
        return DynamoDbClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(region)
                .overrideConfiguration(clientOverrideConfiguration)
                .build();
    }

    public DynamoDbEnhancedClient createDynamoDBEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    public StsClient createSecurityTokenServiceClient() {
        return StsClient.builder()
                .overrideConfiguration(clientConfiguration)
                .build();
    }
}
