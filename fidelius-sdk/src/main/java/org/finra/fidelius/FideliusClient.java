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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.*;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.util.EC2MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FideliusClient {
    private static final Logger logger = LoggerFactory.getLogger(FideliusClient.class);

    protected EnvConfig envConfig;
    protected JCredStash jCredStash;
    protected AWSSecurityTokenService awsSecurityTokenService;

    private AmazonEC2Client client;

    public FideliusClient() {
        this(null, new DefaultAWSCredentialsProviderChain());
    }

    public FideliusClient(String region) {
        this(null, new DefaultAWSCredentialsProviderChain(), region);
    }

    public FideliusClient(ClientConfiguration clientConf, AWSCredentialsProvider provider) {
        this(clientConf, provider, Regions.US_EAST_1.getName());
    }

    public FideliusClient(ClientConfiguration clientConf, AWSCredentialsProvider provider, String region) {

        envConfig = new EnvConfig();
        ClientConfiguration kmsEc2ClientConfiguration = clientConf;

        if(clientConf==null){
            clientConf = defaultClientConfiguration(envConfig);
            clientConf.setRetryPolicy(PredefinedRetryPolicies.DYNAMODB_DEFAULT);
            kmsEc2ClientConfiguration = defaultClientConfiguration(envConfig);
            kmsEc2ClientConfiguration.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(5));
        }

        AmazonDynamoDBClient ddb = new AmazonDynamoDBClient(provider, clientConf)
                .withRegion(Regions.fromName(region));

        AWSKMSClient kms = new AWSKMSClient(provider, kmsEc2ClientConfiguration)
                .withRegion(Regions.fromName(region));

        awsSecurityTokenService =  AWSSecurityTokenServiceClient
                .builder()
                .withClientConfiguration(clientConf)
                .withCredentials(provider)
                .withRegion(Regions.fromName(region))
                .build();

        client = new AmazonEC2Client(provider, kmsEc2ClientConfiguration);

        jCredStash = new JCredStash(ddb, kms, awsSecurityTokenService);
    }

    protected void setFideliusClient(AmazonDynamoDBClient ddb, AWSKMSClient kms) {
        jCredStash = new JCredStash(ddb, kms, awsSecurityTokenService);
    }

    protected ClientConfiguration defaultClientConfiguration(EnvConfig envConfig){
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        if(envConfig.hasProxyEnv()) {
            clientConfiguration.setProxyHost(envConfig.getProxy());
            clientConfiguration.setProxyPort(Integer.parseInt(envConfig.getPort()));
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

    protected  HashMap<String, String> getEC2Tags() {

        String instanceID = EC2MetadataUtils.getInstanceId();

        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceID);
        DescribeInstancesResult instancesResult = client.describeInstances(instancesRequest);

        // There should only be one Instance with identical instanceID
        List<Reservation> reservations = instancesResult.getReservations();
        if (reservations.size() > 1) {
            return null;
        }

        Reservation reservation = reservations.get(0);
        Instance instance = reservation.getInstances().get(0);
        List<Tag> tagList = instance.getTags();

        HashMap<String, String> tagMap = new HashMap<String, String>();
        for (Tag t : tagList) {
            if (t.getKey().equals(Constants.FID_CONTEXT_APPLICATION) || t.getKey().equals(Constants.FID_CONTEXT_SDLC) || t.getKey().equals(Constants.FID_CONTEXT_COMPONENT))
                tagMap.put(t.getKey(), t.getValue());
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
            logger.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    protected String getUserIdentity() throws Exception {
        return awsSecurityTokenService.getCallerIdentity(new GetCallerIdentityRequest()).getArn();
    }

    /**
     * The FID_CONTEXT_APPLICATION, FID_CONTEXT_SDLC, and (optionally) Component will be determined from the instance metadata (Does not work from local)
     * The table name defaults to "credential-store"
     *
     * @param name Base name of the credential to retrieve
     * @return The plaintext contents of the credential (most recent version)
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
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
     * @throws Exception
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
     * @param name                      Base name of the credential to delete
     * @param application   Nullable    FID_CONTEXT_APPLICATION name (not case-sensitive)
     * @param sdlc          Nullable    FID_CONTEXT_SDLC (dev/qa/prod) (not case-sensitive)
     * @param component     Nullable    Name of the associated component (not case-sensitive)
     * @param table         Nullable    Table where credential is stored; defaults to "credential-store"
     *
     * @return null
     * @throws Exception
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
     * @return null
     * @throws Exception
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
            throw new RuntimeException(e);
        }
    }
}
