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

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.finra.fidelius.model.account.Account;
import org.finra.fidelius.model.aws.AWSEnvironment;
import org.finra.fidelius.services.account.AccountsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Component
public class AWSSessionFactory {

    private Logger logger = LoggerFactory.getLogger(AWSSessionFactory.class);

    @Inject
    private ClientOverrideConfiguration clientConfiguration;

    @Inject
    private AccountsService accountsService;

    @Value("${fidelius.assumeRole}")
    private String assumeRole;
    @Value("${fidelius.sessionTimeout}")
    private Integer sessionTimeout;
    @Value("${fidelius.sessionTimeoutPad}")
    private Integer sessionTimeoutPad;

    private static HashMap<AWSEnvironment, DynamoDbClient> dynamoDbClientsMap;
    private static StsClient stsClient;

    private LoadingCache<AWSEnvironment, DynamoDbClient> dynamoDBClientCache = CacheBuilder.newBuilder()
            .maximumSize(1000L)
            .concurrencyLevel(10)
            .expireAfterWrite(60L, TimeUnit.MINUTES)
            .build(new CacheLoader<AWSEnvironment, DynamoDbClient>() {
                public DynamoDbClient load(AWSEnvironment awsEnvironment) throws Exception {
                    return createDynamoDBClient(awsEnvironment);
                }
            });

    public DynamoDbClient getCachedDynamoDBClient(AWSEnvironment env) {
        return dynamoDBClientCache.getUnchecked(env);
    }

    private DynamoDbClient createDynamoDBClient(AWSEnvironment awsEnvironment) {
        instantiateDynamoDbClientsMapIfNew();
        closeDynamoDbClientIfExists(awsEnvironment);
        StsAssumeRoleCredentialsProvider awsCredentialsProvider = getStsAssumeRoleCredentialsProvider(awsEnvironment);
        Region region = awsEnvironment.getRegion();
        FullJitterBackoffStrategy backoffStrategy = FullJitterBackoffStrategy.builder().baseDelay(Duration.ofMillis(100)).maxBackoffTime(Duration.ofMillis(1000)).build();
        RetryPolicy retryPolicy = RetryPolicy.builder().numRetries(5).backoffStrategy(backoffStrategy).throttlingBackoffStrategy(backoffStrategy).build();
        ClientOverrideConfiguration clientOverrideConfiguration = ClientOverrideConfiguration.builder().retryPolicy(retryPolicy).build();
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(region)
                .overrideConfiguration(clientOverrideConfiguration)
                .build();
        dynamoDbClientsMap.put(awsEnvironment, dynamoDbClient);
        return dynamoDbClient;
    }

    private void instantiateDynamoDbClientsMapIfNew() {
        if(dynamoDbClientsMap == null) {
            dynamoDbClientsMap = new HashMap<>();
        }
    }

    private void closeDynamoDbClientIfExists(AWSEnvironment env) {
        if(dynamoDbClientsMap.containsKey(env)) {
            dynamoDbClientsMap.get(env).close();
            dynamoDbClientsMap.remove(env);
        }
    }

    public DynamoDbEnhancedClient createDynamoDBEnhancedClient(AWSEnvironment env) {
        DynamoDbClient dynamoDbClient = dynamoDBClientCache.getUnchecked(env);
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    public StsClient createSecurityTokenServiceClient() {
        if(stsClient != null) {
            return stsClient;
        }
        stsClient = StsClient.builder()
                .overrideConfiguration(clientConfiguration)
                .build();
        return stsClient;
    }

    private StsAssumeRoleCredentialsProvider getStsAssumeRoleCredentialsProvider(AWSEnvironment environment){
        try {
            String roleArn = getRoleArn(environment.getAccount(), assumeRole);
            StsClient stsClient = createSecurityTokenServiceClient();
            AssumeRoleRequest assumeRoleRequest = formAssumeRoleRequest(roleArn);
            return StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(assumeRoleRequest)
                    .build();
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e.getCause());
        }
        return null;
    }

    public AssumeRoleRequest formAssumeRoleRequest(String roleArn) {
        return AssumeRoleRequest.builder()
                .roleArn(roleArn)
                .durationSeconds((sessionTimeout + sessionTimeoutPad) / 1000)
                .roleSessionName("CREDSTSH_APP")
                .build();
    }

    public String getRoleArn(String alias, String role) throws Exception {
        Account account = accountsService.getAccountByAlias(alias);

        if (account == null) {
            logger.error("No account found with alias: " + alias);
            throw new Exception("No account found with alias: " + alias);
        }

        StringBuffer sb = new StringBuffer();
        sb.append("arn:aws:iam::");
        sb.append(account.getAccountId());
        sb.append(":role/");
        sb.append(role);
        return sb.toString();
    }
}
