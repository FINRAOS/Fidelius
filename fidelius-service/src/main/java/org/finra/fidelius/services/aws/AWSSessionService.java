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
import java.util.HashMap;
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

    private static HashMap<AWSEnvironment, KmsClient> kmsClientsMap;
    private static HashMap<AWSEnvironment, RdsClient> rdsClientsMap;
    private static HashMap<AWSEnvironment, RedshiftClient> redshiftClientsMap;

    private Logger logger = LoggerFactory.getLogger(AWSSessionService.class);

    private LoadingCache<AWSEnvironment, StsAssumeRoleCredentialsProvider> stsAssumeRoleCredentialsProviderCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .concurrencyLevel(10)
            .refreshAfterWrite(360 * 1000, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<AWSEnvironment, StsAssumeRoleCredentialsProvider>() {
                @Override
                public StsAssumeRoleCredentialsProvider load(AWSEnvironment environment) throws Exception {
                    return getStsAssumeRoleCredentialsProvider(environment);
                }
            });

    private LoadingCache<AWSEnvironment, KmsClient> kmsClientCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .concurrencyLevel(10)
            .refreshAfterWrite(360 * 1000, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<AWSEnvironment, KmsClient>() {
                @Override
                public KmsClient load(AWSEnvironment environment) throws Exception {
                    return getKmsClient(environment);
                }
            });

    private LoadingCache<AWSEnvironment, RdsClient> rdsClientCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .concurrencyLevel(10)
            .refreshAfterWrite(360 * 1000, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<AWSEnvironment, RdsClient>() {
                @Override
                public RdsClient load(AWSEnvironment environment) throws Exception {
                    return getRdsClient(environment);
                }
            });

    private LoadingCache<AWSEnvironment, RedshiftClient> redshiftClientCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .concurrencyLevel(10)
            .refreshAfterWrite(360 * 1000, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<AWSEnvironment, RedshiftClient>() {
                @Override
                public RedshiftClient load(AWSEnvironment environment) throws Exception {
                    return getRedshiftClient(environment);
                }
            });

    private StsAssumeRoleCredentialsProvider getStsAssumeRoleCredentialsProvider(AWSEnvironment environment){
        try {
            String roleArn = awsSessionFactory.getRoleArn(environment.getAccount(), assumeRole);
            StsClient stsClient = awsSessionFactory.createSecurityTokenServiceClient();
            AssumeRoleRequest assumeRoleRequest = awsSessionFactory.formAssumeRoleRequest(roleArn);
            return StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(assumeRoleRequest)
                    .build();
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e.getCause());
        }
        return null;
    }

    public DynamoDbClient getDynamoDBClient(AWSEnvironment env) {
        return awsSessionFactory.getCachedDynamoDBClient(env);
    }

    public DynamoDbEnhancedClient getDynamoDBEnhancedClient(AWSEnvironment env) {
        return awsSessionFactory.createDynamoDBEnhancedClient(env);
    }

    public KmsClient getCachedKmsClient(AWSEnvironment env) {
        return kmsClientCache.getUnchecked(env);
    }

    private KmsClient getKmsClient(AWSEnvironment env) {
        instantiateKmsClientsMapIfNew();
        closeKmsClientIfExists(env);
        StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = stsAssumeRoleCredentialsProviderCache.getUnchecked(env);
        KmsClient kmsClient = KmsClient
                .builder()
                .credentialsProvider(stsAssumeRoleCredentialsProvider)
                .region(env.getRegion())
                .build();
        kmsClientsMap.put(env, kmsClient);
        return kmsClient;
    }

    private void instantiateKmsClientsMapIfNew() {
        if(kmsClientsMap == null) {
            kmsClientsMap = new HashMap<>();
        }
    }

    private void closeKmsClientIfExists(AWSEnvironment env) {
        if(kmsClientsMap.containsKey(env)) {
            kmsClientsMap.get(env).close();
            kmsClientsMap.remove(env);
        }
    }

    public RdsClient getCachedRdsClient(AWSEnvironment env) {
        return rdsClientCache.getUnchecked(env);
    }

    private RdsClient getRdsClient(AWSEnvironment env){
        instantiateRdsClientsMapIfNew();
        closeRdsClientIfExists(env);
        StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = stsAssumeRoleCredentialsProviderCache.getUnchecked(env);
        return RdsClient
                .builder()
                .credentialsProvider(stsAssumeRoleCredentialsProvider)
                .region(env.getRegion())
                .build();
    }

    private void instantiateRdsClientsMapIfNew() {
        if(rdsClientsMap == null) {
            rdsClientsMap = new HashMap<>();
        }
    }

    private void closeRdsClientIfExists(AWSEnvironment env) {
        if(rdsClientsMap.containsKey(env)) {
            rdsClientsMap.get(env).close();
            rdsClientsMap.remove(env);
        }
    }

    public RedshiftClient getCachedRedshiftClient(AWSEnvironment env) {
        return redshiftClientCache.getUnchecked(env);
    }

    private RedshiftClient getRedshiftClient(AWSEnvironment env){
        instantiateRedshiftClientsMapIfNew();
        closeRedshiftClientIfExists(env);
        StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = stsAssumeRoleCredentialsProviderCache.getUnchecked(env);
        return RedshiftClient
                .builder()
                .credentialsProvider(stsAssumeRoleCredentialsProvider)
                .region(env.getRegion())
                .build();
    }

    private void instantiateRedshiftClientsMapIfNew() {
        if(redshiftClientsMap == null) {
            redshiftClientsMap = new HashMap<>();
        }
    }

    private void closeRedshiftClientIfExists(AWSEnvironment env) {
        if(redshiftClientsMap.containsKey(env)) {
            redshiftClientsMap.get(env).close();
            redshiftClientsMap.remove(env);
        }
    }

}

