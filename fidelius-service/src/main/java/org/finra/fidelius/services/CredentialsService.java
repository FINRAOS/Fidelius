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

package org.finra.fidelius.services;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.*;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.dmfs.httpessentials.client.HttpRequestExecutor;
import org.dmfs.httpessentials.httpurlconnection.HttpUrlConnectionExecutor;
import org.dmfs.oauth2.client.*;
import org.dmfs.oauth2.client.grants.ClientCredentialsGrant;
import org.dmfs.oauth2.client.scope.BasicScope;
import org.dmfs.rfc3986.encoding.Precoded;
import org.dmfs.rfc3986.uris.LazyUri;
import org.dmfs.rfc5545.Duration;
import org.finra.fidelius.MetadataParameters;
import org.finra.fidelius.exceptions.FideliusException;
import org.finra.fidelius.model.Credential;
import org.finra.fidelius.model.HistoryEntry;
import org.finra.fidelius.model.Metadata;
import org.finra.fidelius.model.rotate.RotateRequest;
import org.finra.fidelius.model.aws.AWSEnvironment;
import org.finra.fidelius.model.db.DBCredential;
import org.finra.fidelius.services.account.AccountsService;
import org.finra.fidelius.services.auth.FideliusRoleService;
import org.finra.fidelius.services.aws.AWSSessionService;
import org.finra.fidelius.services.aws.DynamoDBService;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CredentialsService {


    @Value("${fidelius.dynamoTable}")
    private String tableName;

    @Inject
    protected FideliusService fideliusService;

    @Inject
    protected FideliusRoleService fideliusRoleService;

    @Inject
    private AWSSessionService awsSessionService;

    @Inject
    private AccountsService accountsService;

    @Inject
    private DynamoDBService dynamoDBService;

    @Inject
    private MigrateService migrateService;

    @Value("${fidelius.kmsKey}")
    private String kmsKey;

    @Value("${fidelius.rotate.url:}")
    private Optional<String> rotateUrl;

    @Value("${fidelius.rotate.uri:}")
    private Optional<String> rotateUri;

    @Value("${fidelius.auth.oauth.tokenUrl:}")
    private Optional<String> tokenUrl;

    @Value("${fidelius.auth.oauth.tokenUri:}")
    private Optional<String> tokenUri;

    @Value("${fidelius.auth.oauth.clientId:}")
    private Optional<String> clientId;

    @Value("${fidelius.auth.oauth.clientSecret:}")
    private Optional<String> clientSecret;

    private final static String RDS = "rds";
    private final static String AURORA = "aurora";

    private Logger logger = LoggerFactory.getLogger(CredentialsService.class);
    private RestTemplate restTemplate;

    private LoadingCache<String, Optional<String>> userOAuth2TokenCache = CacheBuilder.newBuilder()
            .maximumSize(1000L)
            .concurrencyLevel(10)
            .expireAfterWrite(60L, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<String>>() {
                public Optional<String> load(String user) throws Exception {
                    return Optional.ofNullable(getOAuth2Header(clientId.get(), clientSecret.get()));
                }
            });

    /**
     * Sets Fidelius environment for given AWS Account and AWS Region
     *
     * @param account AWS account to set Fidelius
     * @param region  AWS Region to set Fidelius
     */
    protected void setFideliusEnvironment(String account, String region) {
        AWSEnvironment awsEnvironment = new AWSEnvironment(account, region);
        AmazonDynamoDBClient dynamoDBClient;
        try {
            dynamoDBClient = awsSessionService.getDynamoDBClient(awsEnvironment);
        } catch (AWSSecurityTokenServiceException ex) {
            String message = String.format("Not authorized to access credential table on account: %s in region: %s", account, region);
            logger.error(message, ex);
            throw new FideliusException(message, HttpStatus.FORBIDDEN);
        } catch (RuntimeException re) {
            String message = re.getMessage();
            logger.error(message, re);
            throw new FideliusException(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        AWSKMSClient awskmsClient = awsSessionService.getKmsClient(awsEnvironment);
        fideliusService.setFideliusClient(dynamoDBClient, awskmsClient);
    }

    /**
     * Sets RDS Client for given AWS Account and AWS Region
     *
     * @param account AWS account
     * @param region  AWS Region
     */
    protected AmazonRDSClient setRDSClient(String account, String region) {
        AWSEnvironment awsEnvironment = new AWSEnvironment(account, region);
        AmazonRDSClient amazonRDSClient;
        try {
            amazonRDSClient = awsSessionService.getRdsClient(awsEnvironment);
        } catch (AWSSecurityTokenServiceException ex) {
            String message = String.format("Not authorized to access rds on account: %s in region: %s", account, region);
            logger.error(message, ex);
            throw new FideliusException(message, HttpStatus.FORBIDDEN);
        } catch (RuntimeException re) {
            String message = re.getMessage();
            logger.error(message, re);
            throw new FideliusException(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return amazonRDSClient;
    }

    @PreAuthorize("@fideliusRoleService.isAuthorized(#application, #account, \"LIST_CREDENTIALS\")")
    public List<Credential> getAllCredentials(String tableName, String account, String region, String application) throws FideliusException{
        logger.info(String.format("Getting all credentials for app %s using account %s and region %s.", application, account, region));
        List<Credential> results = new ArrayList<>();

        DynamoDBMapper mapper = dynamoDBService.createMapper(account, region, tableName);
        setFideliusEnvironment(account, region);

        Map<String, String> ean = new HashMap<>();
        ean.put("#tempname", "name");

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":key", new AttributeValue().withS(application + "."));

        StringBuilder sb = new StringBuilder();
        sb.append("begins_with (#tempname, :key)");

        DynamoDBScanExpression queryExp = new DynamoDBScanExpression()
                .withFilterExpression(sb.toString())
                .withExpressionAttributeValues(eav)
                .withExpressionAttributeNames(ean);

        List<DBCredential> queryResults = dynamoDBService.scanDynamoDB(queryExp, DBCredential.class, mapper);

        // Gets only latest version of each credential
        Map<String, DBCredential> credentials = getLatestCredentialVersion(queryResults);

        for (DBCredential dbCredential : credentials.values()) {
            if(dbCredential.getSdlc() == null){
                logger.info(String.format("Credential %s missing attributes.  Attempting to add missing attributes: ", dbCredential.getName()));
                dbCredential = migrateService.guessCredentialProperties(dbCredential);
            }

            try {
                Credential credential = new Credential(dbCredential.getShortKey(), dbCredential.getName(), account, region, application,
                        dbCredential.getSdlc(), dbCredential.getComponent(), splitRoleARN(dbCredential.getUpdatedBy()),
                        dbCredential.getUpdatedDate());

                if(credential.getEnvironment() != null)
                    results.add(credential);

            }catch (Exception e){
                logger.error("Error parsing key " + dbCredential.getName(), e);
          }
        }
        logger.info(String.format("%2d credentials for application %s successfully retrieved.",results.size(), application));

        return results
                .stream()
                .sorted(Comparator.comparing(Credential::getLastUpdatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

    }

    @PreAuthorize("@fideliusRoleService.isAuthorized(#application, #account, \"LIST_CREDENTIALS\")")
    public Credential getCredential(String account, String region, String application, String longKey) throws FideliusException {
        DynamoDBMapper mapper = dynamoDBService.createMapper(account, region, tableName);
        setFideliusEnvironment(account, region);

        Map<String, String> ean = new HashMap<>();
        ean.put("#tempname", "name");

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":key", new AttributeValue().withS(longKey));

        DynamoDBQueryExpression<DBCredential> queryExpression = new DynamoDBQueryExpression<DBCredential>()
                .withExpressionAttributeNames(ean)
                .withKeyConditionExpression("#tempname = :key")
                .withExpressionAttributeValues(eav);
        List<DBCredential> queryResults = dynamoDBService.queryDynamoDB(queryExpression, DBCredential.class, mapper);

        // Gets only latest version of each credential
        Map<String, DBCredential> credentials = getLatestCredentialVersion(queryResults);

        try {
            DBCredential dbCredential = credentials.values().stream().findFirst().get();
            if(dbCredential.getSdlc() == null) {
                dbCredential = migrateService.migrateCredential(dbCredential, fideliusService);
            }

            try {
                return (new Credential(dbCredential.getShortKey(), dbCredential.getName(), account, region, application,
                        dbCredential.getSdlc(), dbCredential.getComponent(), splitRoleARN(dbCredential.getUpdatedBy()),
                        dbCredential.getUpdatedDate()));
            }catch (Exception e){
                logger.error("Error parsing key " + dbCredential.getName(), e);
            }
        } catch (NoSuchElementException e) {
            logger.error("Credential " + longKey + " not found" , e);
            return null;
        }

        return null;
    }

    @PreAuthorize("@fideliusRoleService.isAuthorized(#application, #account, \"LIST_CREDENTIALS\")")
    public List<HistoryEntry> getCredentialHistory(String tableName, String account, String region, String application,
                                                   String environment, String component, String key, boolean isMetadata) throws FideliusException {
        List<HistoryEntry> results = new ArrayList<>();
        DynamoDBMapper mapper = dynamoDBService.createMapper(account, region, tableName);
        setFideliusEnvironment(account, region);

        StringBuilder fullKeyBuilder = new StringBuilder();
        if(isMetadata) {
            fullKeyBuilder.append("META#");
        }
        fullKeyBuilder.append(String.format("%s", application.toUpperCase()));
        if (component != null && !component.equals("null")) {
            fullKeyBuilder.append(String.format(".%s", component));
        }
        fullKeyBuilder.append(String.format(".%s", environment));
        fullKeyBuilder.append(String.format(".%s", key));

        Map<String, String> ean = new HashMap<>();
        ean.put("#tempname", "name");

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":key", new AttributeValue().withS(fullKeyBuilder.toString()));

        DynamoDBQueryExpression<DBCredential> queryExpression = new DynamoDBQueryExpression<DBCredential>()
                .withExpressionAttributeNames(ean)
                .withKeyConditionExpression("#tempname = :key")
                .withExpressionAttributeValues(eav);

        logger.info(String.format("Retrieving history of credential/metadata %s using account %s and region %s", fullKeyBuilder, account, region));
        List<DBCredential> queryResults = dynamoDBService.queryDynamoDB(queryExpression, DBCredential.class, mapper);

        for (DBCredential dbCred : queryResults) {
            results.add(new HistoryEntry(new Integer(dbCred.getVersion()), splitRoleARN(dbCred.getUpdatedBy()), dbCred.getUpdatedDate()));
        }

        logger.info(String.format("Found %d entries for credential/metadata %s.", results.size(), fullKeyBuilder));
        return results;
    }

    /**
     * Get latest secret from specified credential
     *
     * @param account     AWS Account alias used to look for account information
     * @param region      AWS Region associated with AWS Account
     * @param application Key representing membership section of key
     * @param environment Key representing the environment in which the credential is associated with
     * @param component   Optional component associated with credential
     * @param shortKey    Short key or name associated with credential
     * @return Secret
     */
    @PreAuthorize("@fideliusRoleService.isAuthorized(#application, #account)")
    public Credential getCredentialSecret(String account, String region, String application, String environment,
                                      String component, String shortKey) {
        setFideliusEnvironment(account, region);
        String user = fideliusRoleService.getUserProfile().getUserId();

        try {
            if (component != null && (component.isEmpty() || component.equals("null"))) {
                component = null;
            }
            String credentialSecret = fideliusService.getCredential(shortKey, application, environment, component,
                    tableName, user);

            return new Credential(shortKey,null, account, region, application, environment, component,null,null, credentialSecret);
        } catch (Exception e) {
            this.logger.error("Credential not found " + e.toString());
            return null;
        }
    }

    /**
     * Add new Credential or Update an existing Credential
     *
     * @param credential Credential to be created
     * @return Credential created
     */
    @PreAuthorize("@fideliusRoleService.isAuthorized(#credential.application, #credential.account)")
    public Credential putCredential(Credential credential) {
        setFideliusEnvironment(credential.getAccount(), credential.getRegion());
        String user = fideliusRoleService.getUserProfile().getUserId();

        try {
            if(credential.getSource() != null && credential.getSourceType() != null) {
                fideliusService.putCredentialWithMetadata(credential.getShortKey(), credential.getSecret(),
                        credential.getApplication(), credential.getEnvironment(), credential.getComponent(),
                        credential.getSource(), credential.getSourceType(), tableName, user, kmsKey);

            } else {
                fideliusService.putCredential(credential.getShortKey(), credential.getSecret(),
                        credential.getApplication(), credential.getEnvironment(), credential.getComponent(), tableName, user, kmsKey);

            }
            credential.setLastUpdatedBy(user);
            credential.setSecret(null);
        } catch (Exception e) {
            this.logger.info("Credential not created " + e.toString());
            return null;
        }

        return credential;
    }

    /**
     * Wrapper for putCredential used for creating new Credentials
     *
     * @param credential Credential to be created
     * @return Credential created
     */
    @PreAuthorize("@fideliusRoleService.isAuthorized(#credential.application, #credential.account)")
    public Credential createCredential(Credential credential) {

        // Check if the credential to be created already has a history
        List<HistoryEntry> existingCredentials = getCredentialHistory(tableName,
                credential.getAccount(), credential.getRegion(), credential.getApplication(), credential.getEnvironment(),
                credential.getComponent(), credential.getShortKey(), false);
        if (!existingCredentials.isEmpty()) {
            throw new FideliusException("Credential already exists!", HttpStatus.BAD_REQUEST);
        }

        // Add the new credential
        return putCredential(credential);
    }
    /**
     * Get calls the rotation endpoint for a credential
     *
     * @param account     AWS Account alias used to look for account information
     * @param sourceType  Source type of the credential
     * @param source      Source of the credential
     * @param shortKey    Short key or name associated with credential
     * @param component   Optional component associated with credential
     * @param region      AWS Region associated with AWS Account
     * @param application Key representing membership section of key
     * @param environment Key representing the environment in which the credential is associated with
     * @return Secret
     */
    @PreAuthorize("@fideliusRoleService.isAuthorized(#application, #account)")
    public String rotateCredential(String account, String sourceType, String source, String region, String application, String environment,
                                       String component, String shortKey) {
        setFideliusEnvironment(account, region);
        restTemplate = new RestTemplate();
        String user = fideliusRoleService.getUserProfile().getUserId();
        String accountId = fideliusRoleService.fetchAwsAccountId(account);
        String oAuth2Header = "";
        if(!rotateUrl.isPresent() || rotateUrl.get().isEmpty()) {
            this.logger.error("Password rotation URL not provided. Please ensure that fidelius.rotate.url is set.");
            return "500";
        }
        if(oAuthTokenEndpointProvided()) {
            oAuth2Header = userOAuth2TokenCache.getUnchecked(user).get();
        }

        try {
            if(source != null && sourceType != null || !rotateUrl.isPresent() || rotateUrl.get().isEmpty()) {
                logger.info("Credential Rotation of " + shortKey + " triggered by User " + user);
                RotateRequest rotateRequest = new RotateRequest(
                        accountId,
                        sourceType,
                        source,
                        shortKey,
                        application,
                        environment,
                        component
                );
                JSONObject requestBody = rotateRequest.getJsonObject();
                String rotateFullURL;
                if(rotateUri.isPresent() && !rotateUri.get().isEmpty()) {
                    rotateFullURL = rotateUrl.get() + "/" + rotateUri.get();
                } else {
                    rotateFullURL = rotateUrl.get();
                }
                ResponseEntity<JSONObject> response;
                try {
                    response = restTemplate.exchange(
                            rotateFullURL,
                            HttpMethod.POST,
                            buildRequest(requestBody, oAuth2Header),
                            JSONObject.class
                    );
                } catch(HttpStatusCodeException e) {
                    this.logger.info("Credential not rotated " + e.toString());
                    return String.valueOf(e.getRawStatusCode());
                }
                return String.valueOf(response.getStatusCodeValue());

            } else {
                this.logger.info("Credential not rotated Source or SourceType is null");
                return "500";
            }
        } catch (Exception e) {
            this.logger.info("Credential not rotated " + e.toString());
            return "500";
        }

    }

    /**
     * Deletes an existing Credential.
     *
     * @param credential Credential to be deleted
     * @return Credential deleted
     */
    @PreAuthorize("@fideliusRoleService.isAuthorizedToDelete(#credential.getApplication(), #credential.getAccount())")
    public Credential deleteCredential(Credential credential) {
        setFideliusEnvironment(credential.getAccount(), credential.getRegion());
        String user = fideliusRoleService.getUserProfile().getUserId();

        try {
            if (credential.getComponent() == null || credential.getComponent().equals("null")) {
                credential.setComponent(null);
            }

            if(credential.getSource() != null && credential.getSourceType() != null ) {
                fideliusService.deleteCredentialWithMetadata(credential.getShortKey(), credential.getApplication(),
                        credential.getEnvironment(), credential.getComponent(), tableName, user);
            } else {
                fideliusService.deleteCredential(credential.getShortKey(), credential.getApplication(),
                        credential.getEnvironment(), credential.getComponent(), tableName, user);
            }
        } catch (Exception e) {
            this.logger.info("Credential not deleted " + e.toString());
            return null;
        }

        return credential;
    }

    /**
     * Get latest metadata from specified credential
     *
     * @param account     AWS Account alias used to look for account information
     * @param region      AWS Region associated with AWS Account
     * @param application Key representing membership section of key
     * @param environment Key representing the environment in which the credential is associated with
     * @param component   Optional component associated with credential
     * @param shortKey    Short key or name associated with credential
     * @return Secret
     */
    @PreAuthorize("@fideliusRoleService.isAuthorized(#application, #account)")
    public Metadata getMetadata(String account, String region, String application, String environment,
                                String component, String shortKey) {
        setFideliusEnvironment(account, region);
        String user = fideliusRoleService.getUserProfile().getUserId();

        try {
            if (component != null && (component.isEmpty() || component.equals("null"))) {
                component = null;
            }
            MetadataParameters metadata = fideliusService.getMetadata(shortKey, application, environment, component,
                    tableName, user);

            if(metadata == null) {
                return new Metadata(shortKey,null, account, region, application, environment,
                        null, null, component,null,null);
            } else {
                return new Metadata(shortKey, null, account, region, application, environment,
                        metadata.getSourceType(), metadata.getSource(), component, null, null);
            }
        } catch (Exception e) {
            this.logger.error("Metadata not found " + e.toString());
            return null;
        }
    }

    /**
     * Deletes an existing metadata.
     *
     * @param metadata metadata object
     * @return Credential deleted
     */
    @PreAuthorize("@fideliusRoleService.isAuthorizedToDelete(#metadata.getApplication(), #metadata.getAccount())")
    public Metadata deleteMetadata(Metadata metadata) {
        setFideliusEnvironment(metadata.getAccount(), metadata.getRegion());
        String user = fideliusRoleService.getUserProfile().getUserId();

        try {
            if (metadata.getComponent() == null || metadata.getComponent().equals("null")) {
                metadata.setComponent(null);
            }
            fideliusService.deleteMetadata(metadata.getShortKey(), metadata.getApplication(),
                    metadata.getEnvironment(), metadata.getComponent(), tableName, user);
        } catch (Exception e) {
            this.logger.info("Metadata not deleted " + e.toString());
            return null;
        }

        return metadata;
    }

    /**
     * Add new metdata or Update an existing metadata
     *
     * @param metadata Credential to be created
     * @return Credential created
     */
    @PreAuthorize("@fideliusRoleService.isAuthorized(#metadata.application, #metadata.account)")
    public Metadata putMetadata(Metadata metadata) {
        setFideliusEnvironment(metadata.getAccount(), metadata.getRegion());
        String user = fideliusRoleService.getUserProfile().getUserId();

        try {
            String version = fideliusService.putMetadata(metadata.getShortKey(), metadata.getApplication(),
                    metadata.getEnvironment(), metadata.getComponent(), metadata.getSourceType(),
                    metadata.getSource(), tableName, user, kmsKey);
            metadata.setLastUpdatedBy(user);
        } catch (Exception e) {
            this.logger.info("Metadata not created " + e.toString());
            return null;
        }

        return metadata;
    }

    /**
     * Wrapper for putMetdata used for creating new Metdata
     *
     * @param metadata Metadata to be created
     * @return Metadata created
     */
    @PreAuthorize("@fideliusRoleService.isAuthorized(#metadata.application, #metadata.account)")
    public Metadata createMetadata(Metadata metadata) {

        // Check if the credential to be created already has a history
        List<HistoryEntry> existingCredentials = getCredentialHistory(tableName,
                metadata.getAccount(), metadata.getRegion(), metadata.getApplication(), metadata.getEnvironment(),
                metadata.getComponent(), metadata.getShortKey(), true);
        if (!existingCredentials.isEmpty()) {
            throw new FideliusException("Metadata already exists!", HttpStatus.BAD_REQUEST);
        }

        // Add the new credential
        return putMetadata(metadata);
    }


    private String splitRoleARN(String roleARN) {
        if (roleARN == null) return null;

        String[] roleTokens = roleARN.split(":assumed-role/");
        if (roleTokens.length > 1){
            return roleTokens[1];
        } else {
            return roleTokens[0];
        }
    }

    private Map<String, DBCredential> getLatestCredentialVersion(List<DBCredential> queryResults) {
        Map<String, DBCredential> credentials = new HashMap<>();
        for (DBCredential dbCredential : queryResults) {
            if (!credentials.containsKey(dbCredential.getName())) {
                credentials.put(dbCredential.getName(), dbCredential);
            }
            else if (credentials.get(dbCredential.getName()).getVersion().compareTo(dbCredential.getVersion()) < 1) {
                credentials.replace(dbCredential.getName(), dbCredential);
            }
        }

        return credentials;
    }

    private List<String> getAllRDS(String account, String region) throws FideliusException {

        logger.info(String.format("Getting all RDS for account %s and region %s.", account, region));
        List<String> results = new ArrayList<>();

        AmazonRDSClient amazonRDSClient = setRDSClient(account, region);
        Filter rdsEngineFilter = new Filter().withName("engine").withValues("postgres", "mysql", "oracle-se2", "oracle-ee");
        DescribeDBInstancesResult response = amazonRDSClient.describeDBInstances(new DescribeDBInstancesRequest().withFilters(rdsEngineFilter));
        List<DBInstance> dbList = response.getDBInstances();

        for(DBInstance db: dbList) {
            results.add(db.getDBInstanceIdentifier());
        }

        while(response.getMarker() != null){
            response = amazonRDSClient.describeDBInstances(new DescribeDBInstancesRequest().withMarker(response.getMarker()).withFilters(rdsEngineFilter));
            dbList = response.getDBInstances();
            for(DBInstance db: dbList) {
                results.add(db.getDBInstanceIdentifier());
            }
        }

        return results;
    }

    private List<String> getAllAuroraRegionalCluster(String account, String region) throws FideliusException {

        logger.info(String.format("Getting all Aurora clusters for account %s and region %s.", account, region));
        List<String> results = new ArrayList<>();

        AmazonRDSClient amazonRDSClient = setRDSClient(account, region);

        DescribeDBClustersResult response = amazonRDSClient.describeDBClusters();
        List<DBCluster> dbClusterList = response.getDBClusters();

        for(DBCluster cluster: dbClusterList) {
            results.add(cluster.getDBClusterIdentifier());
        }

        while(response.getMarker() != null){
            response = amazonRDSClient.describeDBClusters(new DescribeDBClustersRequest().withMarker(response.getMarker()));
            dbClusterList = response.getDBClusters();
            for(DBCluster cluster: dbClusterList) {
                results.add(cluster.getDBClusterIdentifier());
            }
        }

        return results;
    }

    public List<String> getMetadataInfo(String account, String region, String sourceType) throws Exception {
        switch (sourceType) {
            case RDS:
                return getAllRDS(account, region);
            case AURORA:
                return getAllAuroraRegionalCluster(account, region);
            default:
                throw new Exception("Please pass supported values for sourceType");
        }
    }

    private String getOAuth2Header(String username, String password) {
        String token = getOAuth2Token(username, password);
        if(token.isEmpty()) {
            logger.error("Unable to fetch access token.");
            return "";
        }
        return String.format("Bearer %s", token);
    }

    private String getOAuth2Token(String username, String password) {
        if(!tokenUrl.isPresent() || !tokenUri.isPresent()) {
            throw new RuntimeException("Token URL and URI not provided. Skipping OAuth step.");
        }
        HttpRequestExecutor executor = new HttpUrlConnectionExecutor();
        // Create OAuth2 provider
        OAuth2AuthorizationProvider provider = new BasicOAuth2AuthorizationProvider(
                URI.create(tokenUrl.get() + "/" + tokenUri.get()),
                URI.create(tokenUrl.get() + "/" + tokenUri.get()),
                new Duration(1,0,600)           //Default expiration time if server does not respond
        );
        // Create OAuth2 client credentials
        OAuth2ClientCredentials credentials = new BasicOAuth2ClientCredentials(username, password);
        //Create OAuth2 client
        OAuth2Client client = new BasicOAuth2Client(
                provider,
                credentials,
                new LazyUri(new Precoded("http://localhost"))
        );
        try {
            OAuth2AccessToken token = new ClientCredentialsGrant(client, new BasicScope("scope")).accessToken(executor);
            return token.accessToken().toString();
        } catch(Exception e) {
            logger.error("Exception occurred while fetching access token.");
        }
        return "";
    }

    private boolean oAuthTokenEndpointProvided() {
        return tokenUrl.isPresent() && tokenUri.isPresent() && clientId.isPresent() && clientSecret.isPresent();
    }

    private <T> HttpEntity<T> buildRequest(T body, String authHeader) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Authorization", authHeader);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if(body == null) {
            return new HttpEntity<>(httpHeaders);
        }
        return new HttpEntity<>(body, httpHeaders);
    }

}
