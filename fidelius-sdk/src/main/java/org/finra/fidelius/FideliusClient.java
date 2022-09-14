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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryPolicy;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.http.apache.internal.impl.ApacheSdkHttpClient;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

import javax.swing.plaf.synth.Region;


public class FideliusClient {
    private static final Logger logger = LoggerFactory.getLogger(FideliusClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected EnvConfig envConfig;
    protected ProxyConfiguration proxyConfig;
    protected JCredStash jCredStash;
    protected StsClient stsClient;

    private final Ec2Client ec2Client;
    private final LambdaClient lambdaClient;

    public FideliusClient() {
        this(null, AwsCredentialsProviderChain.builder().addCredentialsProvider(DefaultCredentialsProvider.create()).build());
    }

    public FideliusClient(String region) {
        this(null, AwsCredentialsProviderChain.builder().addCredentialsProvider(DefaultCredentialsProvider.create()).build(), region);
    }

    public FideliusClient(ClientOverrideConfiguration clientConf, AwsCredentialsProvider provider) {
        this(clientConf, provider, null);
    }

    public FideliusClient(ClientOverrideConfiguration clientConf, AwsCredentialsProvider provider, String region) {

        envConfig = new EnvConfig();
        ClientOverrideConfiguration kmsEc2ClientConfiguration = clientConf;

        if(clientConf==null){
            clientConf = defaultClientConfiguration(envConfig);
            kmsEc2ClientConfiguration = defaultClientConfiguration(envConfig);
        }

        DynamoDbClientBuilder dynamoDbBuilder = DynamoDbClient.builder()
                .credentialsProvider(provider)
                .overrideConfiguration(clientConf);

        KmsClientBuilder kmsBuilder = KmsClient.builder()
                .credentialsProvider(provider)
                .overrideConfiguration(kmsEc2ClientConfiguration);

        StsClientBuilder stsBuilder =  StsClient.builder()
                .overrideConfiguration(clientConf)
                .credentialsProvider(provider);

        Ec2ClientBuilder clientBuilder = Ec2Client.builder()
                .credentialsProvider(provider)
                .overrideConfiguration(kmsEc2ClientConfiguration);

        LambdaClientBuilder lambdaClientBuilder = LambdaClient.builder()
                .credentialsProvider(provider)
                .overrideConfiguration(clientConf);

        if(region != null){
            software.amazon.awssdk.regions.Region awsRegion = software.amazon.awssdk.regions.Region.of(region);
            dynamoDbBuilder = dynamoDbBuilder.region(awsRegion);
            kmsBuilder = kmsBuilder.region(awsRegion);
            stsBuilder = stsBuilder.region(awsRegion);
            clientBuilder = clientBuilder.region(awsRegion);
            lambdaClientBuilder = lambdaClientBuilder.region(awsRegion);
        }
        if(envConfig.hasProxyEnv()) {
            SdkHttpClient sdkHttpClient = ApacheHttpClient.builder()
                    .proxyConfiguration(proxyConfig).build();
            dynamoDbBuilder = dynamoDbBuilder.httpClient(sdkHttpClient);
            kmsBuilder = kmsBuilder.httpClient(sdkHttpClient);
            stsBuilder = stsBuilder.httpClient(sdkHttpClient);
            clientBuilder = clientBuilder.httpClient(sdkHttpClient);
            lambdaClientBuilder = lambdaClientBuilder.httpClient(sdkHttpClient);
        }
        lambdaClient = lambdaClientBuilder.build();
        ec2Client = clientBuilder.build();
        stsClient = stsBuilder.build();
        jCredStash = new JCredStash(dynamoDbBuilder.build(), kmsBuilder.build(), stsClient);
    }

    protected void setFideliusClient(DynamoDbClient ddb, KmsClient kms) {
        jCredStash = new JCredStash(ddb, kms, stsClient);
    }

    protected ClientOverrideConfiguration defaultClientConfiguration(EnvConfig envConfig){
        ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder().retryPolicy(AwsRetryPolicy.defaultRetryPolicy()).build();
        if(envConfig.hasProxyEnv()) {
            proxyConfig = ProxyConfiguration.builder().endpoint(URI.create(envConfig.getProxy() + ":" + envConfig.getPort())).build();
        }
        return clientConfiguration;
    }

    private  String getPrefixedName(String credentialName, Map tags) {
        if (tags.containsKey(Constants.FID_CONTEXT_APPLICATION) && tags.containsKey(Constants.FID_CONTEXT_SDLC)) {
            String prefixedName;
            if (tags.containsKey(Constants.FID_CONTEXT_COMPONENT)) {
                prefixedName = tags.get(Constants.FID_CONTEXT_APPLICATION) + "." + tags.get(Constants.FID_CONTEXT_COMPONENT) + "." + tags.get(Constants.FID_CONTEXT_SDLC) + "." + credentialName;
            } else {
                prefixedName = tags.get(Constants.FID_CONTEXT_APPLICATION) + "." + tags.get(Constants.FID_CONTEXT_SDLC) + "." + credentialName;
            }
            return prefixedName;
        } else {
            return null;
        }
    }

    private  String getPrefixedNameForMetadata(String credentialName, Map tags) {
        if (tags.containsKey(Constants.FID_CONTEXT_APPLICATION) && tags.containsKey(Constants.FID_CONTEXT_SDLC)) {
            String prefixedName;
            if (tags.containsKey(Constants.FID_CONTEXT_COMPONENT)) {
                prefixedName = "META#" + tags.get(Constants.FID_CONTEXT_APPLICATION) + "." + tags.get(Constants.FID_CONTEXT_COMPONENT) + "." + tags.get(Constants.FID_CONTEXT_SDLC) + "." + credentialName;
            } else {
                prefixedName = "META#" + tags.get(Constants.FID_CONTEXT_APPLICATION) + "." + tags.get(Constants.FID_CONTEXT_SDLC) + "." + credentialName;
            }
            return prefixedName;
        } else {
            return null;
        }
    }

    protected  HashMap<String, String> getEC2Tags() {

        String instanceID = EC2MetadataUtils.getInstanceId();

        DescribeInstancesRequest instancesRequest = DescribeInstancesRequest.builder().instanceIds(instanceID).build();
        DescribeInstancesResponse instancesResult = ec2Client.describeInstances(instancesRequest);

        // There should only be one Instance with identical instanceID
        List<Reservation> reservations = instancesResult.reservations();
        if (reservations.size() > 1) {
            return null;
        }

        Reservation reservation = reservations.get(0);
        Instance instance = reservation.instances().get(0);
        List<Tag> tagList = instance.tags();

        HashMap<String, String> tagMap = new HashMap<String, String>();
        for (Tag t : tagList) {
            if (t.key().equals(Constants.FID_CONTEXT_APPLICATION) || t.key().equals(Constants.FID_CONTEXT_SDLC) || t.key().equals(Constants.FID_CONTEXT_COMPONENT))
                tagMap.put(t.key(), t.value());
        }
        return tagMap;
    }

    private  HashMap<String, String> createContextMap(String application, String sdlc, String component) throws Exception {
        HashMap<String, String> context = new HashMap<String, String>();

        // First preference is for FID_CONTEXT_APPLICATION/FID_CONTEXT_SDLC values passed directly to the API.If they are null, then second check is for env variables. Last option is for EC2 tags.
        if (application == null || sdlc == null) {

            //check env values
            boolean valid = false;

            if (envConfig.hasAgsSdlcEnv()) {
                context.put(Constants.FID_CONTEXT_APPLICATION, envConfig.getApplication().toUpperCase());
                context.put(Constants.FID_CONTEXT_SDLC, envConfig.getSdlc().toLowerCase());
                if (envConfig.hasComponentEnv()) {
                    context.put(Constants.FID_CONTEXT_COMPONENT,
                            envConfig.getComponent().toLowerCase());
                }
                valid = true;
            }

            // check EC2 tags
            if(!valid ) {
                HashMap<String, String> tags = getEC2Tags();

                if (tags != null && tags.containsKey(Constants.FID_CONTEXT_APPLICATION) && tags.containsKey(Constants.FID_CONTEXT_SDLC)) {
                    context.put(Constants.FID_CONTEXT_APPLICATION, tags.get(Constants.FID_CONTEXT_APPLICATION).toUpperCase());
                    context.put(Constants.FID_CONTEXT_SDLC, tags.get(Constants.FID_CONTEXT_SDLC).toLowerCase());
                    if (tags.keySet().contains(Constants.FID_CONTEXT_COMPONENT)) {
                        context.put(Constants.FID_CONTEXT_COMPONENT, tags.get(Constants.FID_CONTEXT_COMPONENT).toLowerCase());
                    }
                    valid = true;
                }
            }

            if(!valid){
                logger.error(Constants.FID_CONTEXT_APPLICATION + " or " + Constants.FID_CONTEXT_SDLC + " not specified and cannot be retrieved from tags or environment.");
                throw new Exception(Constants.FID_CONTEXT_APPLICATION + " or " + Constants.FID_CONTEXT_SDLC + " not specified and cannot be retrieved from tags or environment.");
            }
        } else {
            context.put(Constants.FID_CONTEXT_APPLICATION, application.toUpperCase());
            context.put(Constants.FID_CONTEXT_SDLC, sdlc.toLowerCase());
            if (component != null && component.length() > 0) {
                context.put(Constants.FID_CONTEXT_COMPONENT, component.toLowerCase());
            }
        }
        return context;
    }

    protected String getUser() throws Exception {
        try {
            String userARN = getUserIdentity();
            String[] userTokens = userARN.split(":assumed-role/");
            if (userTokens.length > 1) {
                return userTokens[1];
            } else {
                return userTokens[0];
            }
        } catch(Exception e){
            logger.error("Failed to determine user: {}", ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    protected String getUserIdentity() throws Exception {
        return stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build()).arn();
    }

    /**
     * The FID_CONTEXT_APPLICATION, FID_CONTEXT_SDLC, and (optionally) Component will be determined from the instance metadata (Does not work from local)
     * The table name defaults to "credential-store"
     *
     * @param name Base name of the credential to retrieve
     * @return The plaintext contents of the credential (most recent version)
     * @throws Exception - if the credential cannot be retrieved
     */
    public String getCredential(String name) throws Exception {
        return getCredential(name, null, null, null, Constants.DEFAULT_TABLE, null, true);
    }

    /**
     * The FID_CONTEXT_APPLICATION, FID_CONTEXT_SDLC, and (optionally) Component will be determined from the instance metadata (Does not work from local)
     *
     * @param name Base name of the credential to retrieve
     * @param table The table where credential is stored
     * @return The plaintext contents of the credential (most recent version)
     * @throws Exception - if the credential cannot be retrieved
     */
    public String getCredential(String name, String table) throws Exception {
        return getCredential(name, null, null, null, table, null, true);
    }

    /**
     *
     * @param name                      Base name of the credential to retrieve
     * @param application   Nullable    FID_CONTEXT_APPLICATION name (not case-sensitive)
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod) (not case-sensitive)
     * @param component     Nullable    Name of the associated component (not case-sensitive)
     * @param table         Nullable    Table where credential is stored; defaults to "credential-store"
     *
     * @return The plaintext contents of the credential (most recent version)
     * @throws Exception - if the credential cannot be retrieved
     */
    public String getCredential(String name, String application,  String sdlc,  String component,
                                String table) throws Exception {
        return getCredential(name, application, sdlc, component, table, null, true);
    }

    /**
     *
     * @param name                  Base name of the credential to retrieve
     * @param application   Nullable    FID_CONTEXT_APPLICATION name (not case-sensitive)
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod) (not case-sensitive)
     * @param component     Nullable    Name of the associated component (not case-sensitive)
     * @param table         Nullable    Table where credential is stored; defaults to "credential-store"
     * @param user          Nullable    Name of user that requested to retrieve credential
     * @param retryForApplication    Nullable  Boolean that enables search retry by removing component to find FID_CONTEXT_APPLICATION specific credential
     *
     * @return The plaintext contents of the credential (most recent version)
     * @throws Exception - if the credential cannot be retrieved
     *
     */
    protected String getCredential(String name, String application,  String sdlc,  String component,
                                        String table, String user, Boolean retryForApplication) throws Exception {

        if (table == null || table.length() == 0)
            table = Constants.DEFAULT_TABLE;

        if (user == null || user.length() == 0)
            user = getUser();

        HashMap<String, String> context = createContextMap(application, sdlc, component);
        String prefixedName = getPrefixedName(name, context);

        String credential = null;
        try {
            credential = jCredStash.getSecret(table, prefixedName, context);
            logger.info("User "+ user + " retrieved contents of " + prefixedName);
        } catch (RuntimeException e) { // Credential not found
            logger.info("Credential " + prefixedName + " not found. ["+e.toString()+"] ");

            if(retryForApplication == null || retryForApplication == true) {
                // If component was specified
                if (context.containsKey(Constants.FID_CONTEXT_COMPONENT)) {
                    context.remove(Constants.FID_CONTEXT_COMPONENT);
                    prefixedName = getPrefixedName(name, context);
                    logger.info("Retrieving " + prefixedName + ": ");

                    // Attempt to get FID_CONTEXT_APPLICATION-specific credential
                    try {
                        credential = jCredStash.getSecret(table, prefixedName, context);
                        logger.info("User " + user + " retrieved contents of " + prefixedName);
                    } catch (RuntimeException ex) {
                        logger.error("Credential " + prefixedName + " not found. ");
                        logger.error(ex.toString());
                    }
                } else {
                    logger.error(e.toString());
                }
            }
        }
        return credential;
    }
    /**
     * The FID_CONTEXT_APPLICATION, FID_CONTEXT_SDLC, and (optionally) Component will be determined from the instance metadata (Does not work from local)
     *
     * @param name      Name of the secret
     * @param contents  Plaintext contents of the secret
     *
     * @return Version Padded version of credential created in String format
     * @throws Exception - if the credential cannot be stored
     */
    public String putCredential(String name, String contents) throws Exception {
        return putCredential(name, contents, null, null, null, null,null);
    }

    /**
     * The FID_CONTEXT_APPLICATION, FID_CONTEXT_SDLC, and (optionally) Component will be determined from the instance metadata (Does not work from local)
     *
     * @param name      Name of the secret
     * @param contents  Plaintext contents of the secret
     * @param table     Table where credential is stored; defaults to 'credential-store'
     * @param kmsKey    Name of the KMS key used for wrapping; defaults to 'alias/credstash'
     *
     * @return Version Padded version of credential created in String format
     * @throws Exception - if the credential cannot be stored
     */
    public String  putCredential(String name, String contents, String table, String kmsKey) throws Exception{
        return putCredential(name, contents, null, null, null, table,  kmsKey);
    }

    /**
     *
     * @param name                      Name of the secret
     * @param contents                  Plaintext contents of the secret
     * @param application   Nullable    FID_CONTEXT_APPLICATION Name
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod)
     * @param component     Nullable    Component name
     * @param table         Nullable    Table where credential is stored; defaults to 'credential-store'
     * @param kmsKey        Nullable    Name of the KMS key used for wrapping; defaults to 'alias/credstash'
     *
     * @return Version              Padded version of credential created in String format
     * @throws Exception - if the credential cannot be stored
     */
    public String putCredential(String name, String contents, String application, String sdlc, String component,
                              String table, String kmsKey) throws Exception {
        return putCredential(name, contents, application, sdlc, component, table, null, kmsKey);
    }
    /**
     *
     * @param name                      Name of the secret
     * @param contents                  Plaintext contents of the secret
     * @param application   Nullable    FID_CONTEXT_APPLICATION Name
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod)
     * @param component     Nullable    Component name
     * @param table         Nullable    Table where credential is stored; defaults to 'credential-store'
     * @param user          Nullable    User that created Credential; defaults to IAM user
     * @param kmsKey        Nullable    Name of the KMS key used for wrapping; defaults to 'alias/credstash'
     *
     * @return Version                   Padded version of credential created in String format
     * @throws Exception - if the credential cannot be stored
     */
    protected String putCredential(String name, String contents, String application, String sdlc, String component,
                              String table, String user, String kmsKey) throws Exception {
        if (table == null || table.length() == 0)
            table = Constants.DEFAULT_TABLE;

        if(user == null ) {
            user = getUser();
        }

        HashMap<String, String> context = createContextMap(application, sdlc, component);
        String prefixedName = getPrefixedName(name, context);

        String versionString;
        int nextVersion = jCredStash.getHighestVersion(prefixedName, table) + 1;
        versionString = String.format("%019d", nextVersion);

        jCredStash.putSecret(table, prefixedName, contents, versionString, user, kmsKey, context);

        logger.info("Version " + versionString + " of " + prefixedName + " stored in " + table + " by User " + user);

        return versionString;
    }
    /**
     *
     * @param name                      Name of the secret
     * @param contents                  Plaintext contents of the secret
     * @param application   Nullable    FID_CONTEXT_APPLICATION Name
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod)
     * @param component     Nullable    Component name
     * @param sourceType                Source type
     * @param source                    Source name
     * @param table         Nullable    Table where credential is stored; defaults to 'credential-store'
     * @param user          Nullable    User that created Credential; defaults to IAM user
     * @param kmsKey        Nullable    Name of the KMS key used for wrapping; defaults to 'alias/credstash'
     *
     * @return Version                   Padded version of credential created in String format
     * @throws Exception - if the credential cannot be stored
     */
    public String putCredentialWithMetadata(String name, String contents, String application, String sdlc, String component,
                                               String source, String sourceType, String table, String user, String kmsKey) throws Exception {
        String version = putCredential(name, contents, application, sdlc, component, table, user, kmsKey);
        putMetadata(name, application, sdlc, component, sourceType, source, table, user, kmsKey);
        return version;
    }

    /**
     *
     * @param name                      Base name of the credential to delete
     * @param application   Nullable    FID_CONTEXT_APPLICATION name (not case-sensitive)
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod) (not case-sensitive)
     * @param component     Nullable    Name of the associated component (not case-sensitive)
     * @param table         Nullable    Table where credential is stored; defaults to "credential-store"
     *
     * @throws Exception - if the credential cannot be deleted
     */
    public void deleteCredential(String name, String application,  String sdlc,  String component,
                                 String table) throws Exception {
        deleteCredential(name, application, sdlc, component,  table, null);
    }

    /**
     *
     * @param name                      Base name of the credential to delete
     * @param application   Nullable    FID_CONTEXT_APPLICATION name (not case-sensitive)
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod) (not case-sensitive)
     * @param component     Nullable    Name of the associated component (not case-sensitive)
     * @param table         Nullable    Table where credential is stored; defaults to "credential-store"
     * @param user          Nullable    Name of the user that deleted component
     *
     * @throws Exception - if the credential cannot be deleted
     */
    public void deleteCredentialWithMetadata(String name, String application,  String sdlc,  String component, String table,
                                    String user) throws Exception {
        deleteCredential(name, application, sdlc, component,  table, user);
        deleteMetadata(name, application, sdlc, component, table, user);
    }

    /**
     *
     * @param name                      Base name of the credential to delete
     * @param application   Nullable    FID_CONTEXT_APPLICATION name (not case-sensitive)
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod) (not case-sensitive)
     * @param component     Nullable    Name of the associated component (not case-sensitive)
     * @param table         Nullable    Table where credential is stored; defaults to "credential-store"
     * @param user          Nullable    Name of the user that deleted component
     *
     * @throws Exception - if the credential cannot be deleted
     */
    protected void deleteCredential(String name, String application,  String sdlc,  String component, String table,
                                    String user) throws Exception {

        if (table == null || table.length() == 0)
            table = Constants.DEFAULT_TABLE;

        if (user == null || user.length() == 0)
            user = getUser();

        HashMap<String, String> context = createContextMap(application, sdlc, component);
        String prefixedName = getPrefixedName(name, context);

        try {
            jCredStash.deleteSecret(table, prefixedName);
            logger.info("User "+ user + " deleted credential " + prefixedName);
        } catch (RuntimeException e) { // Credential not found
            logger.info("Credential " + prefixedName + " not found. [" + e.toString() + "] ");
            for(StackTraceElement ste : e.getStackTrace()) {
                logger.error(ste.toString());
            }
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param name                  Base name of the credential to retrieve
     * @param application   Nullable    FID_CONTEXT_APPLICATION name (not case-sensitive)
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod) (not case-sensitive)
     * @param component     Nullable    Name of the associated component (not case-sensitive)
     * @param table         Nullable    Table where credential is stored; defaults to "credential-store"
     * @param user          Nullable    Name of user that requested to retrieve credential
     * @param retryForApplication    Nullable  Boolean that enables search retry by removing component to find FID_CONTEXT_APPLICATION specific credential
     *
     * @return metadata - Information about metadata (most recent version)
     * @throws Exception - if the credential cannot be retrieved
     *
     */
    protected MetadataParameters getMetadata(String name, String application, String sdlc, String component,
                                             String table, String user, Boolean retryForApplication) throws Exception {

        if (table == null || table.length() == 0)
            table = Constants.DEFAULT_TABLE;

        if (user == null || user.length() == 0)
            user = getUser();

        HashMap<String, String> context = createContextMap(application, sdlc, component);
        String prefixedName = getPrefixedNameForMetadata(name, context);

        MetadataParameters metadataParameters = null;
        try {
            metadataParameters = jCredStash.getMetadata(table, prefixedName, context);
            logger.info("User "+ user + " retrieved contents of " + prefixedName);
        } catch (RuntimeException e) {    // MetadataParameters not found
            logger.info("Metadata " + prefixedName + " not found. ["+e.toString()+"] ");

            if(retryForApplication == null || retryForApplication == true) {
                // If component was specified
                if (context.containsKey(Constants.FID_CONTEXT_COMPONENT)) {
                    context.remove(Constants.FID_CONTEXT_COMPONENT);
                    prefixedName = getPrefixedNameForMetadata(name, context);
                    logger.info("Retrieving " + prefixedName + ": ");

                    // Attempt to get FID_CONTEXT_APPLICATION-specific credential
                    try {
                        metadataParameters = jCredStash.getMetadata(table, prefixedName, context);
                        logger.info("User " + user + " retrieved contents of " + prefixedName);
                    } catch (RuntimeException ex) {
                        logger.error("Metadata " + prefixedName + " not found. ");
                        logger.error(ex.toString());
                    }
                } else {
                    logger.error(e.toString());
                }
            }
        }
        return metadataParameters;
    }

    /**
     *
     * @param name                      Name of the metadata
     * @param application   Nullable    FID_CONTEXT_APPLICATION Name
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod)
     * @param component     Nullable    Component name
     * @param sourceType                Source type
     * @param source                    Source name
     * @param table         Nullable    Table where credential is stored; defaults to 'credential-store'
     * @param user          Nullable    User that created Credential; defaults to IAM user
     * @param kmsKey        Nullable    Name of the KMS key used for wrapping; defaults to 'alias/credstash'
     *
     * @return Version                   Padded version of credential created in String format
     * @throws Exception - if the credential cannot be stored
     */
    protected String putMetadata(String name, String application, String sdlc, String component, String sourceType,
                                 String source, String table, String user, String kmsKey) throws Exception {
        if (table == null || table.length() == 0)
            table = Constants.DEFAULT_TABLE;

        if(user == null ) {
            user = getUser();
        }

        HashMap<String, String> context = createContextMap(application, sdlc, component);
        String prefixedName = getPrefixedNameForMetadata(name, context);

        String versionString;
        int nextVersion = jCredStash.getHighestVersion(prefixedName, table) + 1;
        versionString = String.format("%019d", nextVersion);

        jCredStash.putMetadata(table, prefixedName, versionString, sourceType, source, user, kmsKey, context);

        logger.info("Version " + versionString + " of " + prefixedName + " stored in " + table + " by User " + user);

        return versionString;
    }

    /**
     *
     * @param name                      Base name of the credential to delete
     * @param application   Nullable    FID_CONTEXT_APPLICATION name (not case-sensitive)
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod) (not case-sensitive)
     * @param component     Nullable    Name of the associated component (not case-sensitive)
     * @param table         Nullable    Table where credential is stored; defaults to "credential-store"
     * @param user          Nullable    Name of the user that deleted component
     *
     * @throws Exception - if the credential cannot be deleted
     */
    protected void deleteMetadata(String name, String application,  String sdlc,  String component, String table,
                                    String user) throws Exception {

        if (table == null || table.length() == 0)
            table = Constants.DEFAULT_TABLE;

        if (user == null || user.length() == 0)
            user = getUser();

        HashMap<String, String> context = createContextMap(application, sdlc, component);
        String prefixedName = getPrefixedNameForMetadata(name, context);

        try {
            jCredStash.deleteSecret(table, prefixedName);
            logger.info("User "+ user + " deleted metadata " + prefixedName);
        } catch (RuntimeException e) { // Credential not found
            logger.info("Metadata with name: " + prefixedName + " not found. [" + e.toString() + "] ");
            throw new RuntimeException(e);
        }
    }
}
