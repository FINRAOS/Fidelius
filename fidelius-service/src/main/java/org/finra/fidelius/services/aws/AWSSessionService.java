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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
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

    private LoadingCache<AWSEnvironment, BasicSessionCredentials> credentialCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .concurrencyLevel(10)
            .refreshAfterWrite(360 * 1000, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<AWSEnvironment, BasicSessionCredentials>() {
                @Override
                public BasicSessionCredentials load(AWSEnvironment environment) throws Exception {
                    return getFreshCredentials(environment);
                }
            });


    private BasicSessionCredentials getFreshCredentials(AWSEnvironment environment) throws Exception{

        String roleArn = getRoleArn(environment.getAccount(), assumeRole);
        logger.info("Assuming to role: " + roleArn + " for environment " + environment.getAccount() + " on region " + environment.getRegion()
                + " with timeout of " + (sessionTimeout / 1000) + " seconds (with " + (sessionTimeoutPad / 1000) + " padding.)");

        AssumeRoleRequest assumeRequest = new AssumeRoleRequest()
                .withRoleArn(roleArn)
                .withDurationSeconds((sessionTimeout + sessionTimeoutPad) / 1000)
                .withRoleSessionName("CREDSTSH_APP");

        AssumeRoleResult assumeResult = awsSessionFactory.createSecurityTokenServiceClient().assumeRole(assumeRequest);

        return new BasicSessionCredentials(
                assumeResult.getCredentials().getAccessKeyId(),
                assumeResult.getCredentials().getSecretAccessKey(),
                assumeResult.getCredentials().getSessionToken());

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

    public AmazonDynamoDBClient getDynamoDBClient(AWSEnvironment env) {
        BasicSessionCredentials creds = null;
        try {
            creds = credentialCache.getUnchecked(env);
        } catch (UncheckedExecutionException ue) {
            Throwables.throwIfUnchecked(ue.getCause());
        }
        AmazonDynamoDBClient dynamoClient = awsSessionFactory.createDynamoDBClient(creds);
        dynamoClient.setRegion(Region.getRegion(Regions.fromName(env.getRegion())));
        return dynamoClient;
    }

    public AWSKMSClient getKmsClient(AWSEnvironment environment) {
        BasicSessionCredentials credentials = credentialCache.getUnchecked(environment);
        AWSKMSClient awsKmsClient = (AWSKMSClient) AWSKMSClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(environment.getRegion())
                .build();

        return awsKmsClient;
    }

    public AmazonRDSClient getRdsClient(AWSEnvironment environment){
        BasicSessionCredentials credentials = credentialCache.getUnchecked(environment);

        return (AmazonRDSClient) AmazonRDSClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(environment.getRegion())
                .build();
    }

}

