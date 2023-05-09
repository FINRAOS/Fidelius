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

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.finra.fidelius.factories.AWSSessionFactory;
import org.finra.fidelius.model.account.Account;
import org.finra.fidelius.model.aws.AWSEnvironment;
import org.finra.fidelius.services.account.AccountsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class AWSSessionService {

    @Inject
    private AWSSessionFactory awsSessionFactory;

    @Inject
    private AccountsService accountsService;

    @Value("${fidelius.assumeRole}")
    private String assumeRole;
    @Value("${fidelius.sessionTimeout}")
    private Integer sessionTimeout;
    @Value("${fidelius.sessionTimeoutPad}")
    private Integer sessionTimeoutPad;

    private Logger logger = LoggerFactory.getLogger(AWSSessionService.class);

    private LoadingCache<AWSEnvironment, Credentials> credentialCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .concurrencyLevel(10)
            .refreshAfterWrite(360 * 1000, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<AWSEnvironment, Credentials>() {
                @Override
                public Credentials load(AWSEnvironment environment) throws Exception {
                    return getFreshCredentials(environment);
                }
            });


    private Credentials getFreshCredentials(AWSEnvironment environment) throws Exception{

        String roleArn = getRoleArn(environment.getAccount(), assumeRole);
        logger.info("Assuming to role: " + roleArn + " for environment " + environment.getAccount() + " on region " + environment.getRegion()
                + " with timeout of " + (sessionTimeout / 1000) + " seconds (with " + (sessionTimeoutPad / 1000) + " padding.)");

        AssumeRoleRequest assumeRequest = AssumeRoleRequest.builder()
                .roleArn(roleArn)
                .durationSeconds((sessionTimeout + sessionTimeoutPad) / 1000)
                .roleSessionName("CREDSTSH_APP")
                .build();

        AssumeRoleResponse assumeRoleResponse = awsSessionFactory.createSecurityTokenServiceClient().assumeRole(assumeRequest);

        return assumeRoleResponse.credentials();

    }

    private StsAssumeRoleCredentialsProvider getStsAssumeRoleCredentialsProvider(AWSEnvironment environment){
        try {
            String roleArn = getRoleArn(environment.getAccount(), assumeRole);
            StsClient stsClient = awsSessionFactory.createSecurityTokenServiceClient();
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

    private AssumeRoleRequest formAssumeRoleRequest(String roleArn) {
        return AssumeRoleRequest.builder()
                .roleArn(roleArn)
                .durationSeconds((sessionTimeout + sessionTimeoutPad) / 1000)
                .roleSessionName("CREDSTSH_APP")
                .build();
    }

    private String getRoleArn(String alias, String role) throws Exception {
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

    public DynamoDbClient getDynamoDBClient(AWSEnvironment env) {
        StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = getStsAssumeRoleCredentialsProvider(env);
        return awsSessionFactory.createDynamoDBClient(stsAssumeRoleCredentialsProvider, env.getRegion());
    }

    public DynamoDbEnhancedClient getDynamoDBEnhancedClient(DynamoDbClient dynamoDbClient) {
        return awsSessionFactory.createDynamoDBEnhancedClient(dynamoDbClient);
    }

    public KmsClient getKmsClient(AWSEnvironment env) {
        StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = getStsAssumeRoleCredentialsProvider(env);
        return KmsClient
                .builder()
                .credentialsProvider(stsAssumeRoleCredentialsProvider)
                .region(env.getRegion())
                .build();
    }

    public RdsClient getRdsClient(AWSEnvironment env){
        StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = getStsAssumeRoleCredentialsProvider(env);
        return RdsClient
                .builder()
                .credentialsProvider(stsAssumeRoleCredentialsProvider)
                .region(env.getRegion())
                .build();
    }

    public RedshiftClient getRedshiftClient(AWSEnvironment env){
        StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = getStsAssumeRoleCredentialsProvider(env);
        return RedshiftClient
                .builder()
                .credentialsProvider(stsAssumeRoleCredentialsProvider)
                .region(env.getRegion())
                .build();
    }

}

