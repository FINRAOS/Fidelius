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
import org.finra.fidelius.services.account.AccountsService;
import org.finra.fidelius.services.auth.FideliusRoleService;
import org.finra.fidelius.services.aws.AWSSessionService;
import org.finra.fidelius.services.aws.DynamoDBService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.sts.model.StsException;

import javax.inject.Inject;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    @Value(("${fidelius.sourceTypes}"))
    private String sourceTypes;

    @Value("${fidelius.rotate.url:}")
    private Optional<String> rotateUrl;

    @Value("${fidelius.rotate.uri:}")
    private Optional<String> rotateUri;

    @Value("${fidelius.rotate.oauth.tokenUrl:}")
    private Optional<String> tokenUrl;

    @Value("${fidelius.rotate.oauth.tokenUri:}")
    private Optional<String> tokenUri;

    @Value("${fidelius.rotate.oauth.clientId:}")
    private Optional<String> clientId;

    @Value("${fidelius.rotate.oauth.clientSecret:}")
    private Optional<String> clientSecret;

    private final static String RDS = "rds";
    private final static String AURORA = "aurora";

    public final static String NAME = "name";
    public final static String VERSION = "version";
    public final static String UPDATED_BY = "updatedBy";
    public final static String UPDATED_ON = "updatedOn";
    public final static String SDLC = "sdlc";
    public final static String COMPONENT = "component";

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
        DynamoDbClient dynamoDBClient;
        try {
            dynamoDBClient = awsSessionService.getDynamoDBClient(awsEnvironment);
        } catch (StsException ex) {
            String message = String.format("Not authorized to access credential table on account: %s in region: %s", account, region);
            logger.error(message, ex);
            throw new FideliusException(message, HttpStatus.FORBIDDEN);
        } catch (RuntimeException re) {
            String message = re.getMessage();
            logger.error(message, re);
            throw new FideliusException(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        KmsClient kmsClient = awsSessionService.getKmsClient(awsEnvironment);
        fideliusService.setFideliusClient(dynamoDBClient, kmsClient);
    }

    /**
     * Sets RDS Client for given AWS Account and AWS Region
     *
     * @param account AWS account
     * @param region  AWS Region
     */
    protected RdsClient setRDSClient(String account, String region) {
        AWSEnvironment awsEnvironment = new AWSEnvironment(account, region);
        RdsClient rdsClient;
        try {
            rdsClient = awsSessionService.getRdsClient(awsEnvironment);
        } catch (StsException ex) {
            String message = String.format("Not authorized to access rds on account: %s in region: %s", account, region);
            logger.error(message, ex);
            throw new FideliusException(message, HttpStatus.FORBIDDEN);
        } catch (RuntimeException re) {
            String message = re.getMessage();
            logger.error(message, re);
            throw new FideliusException(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return rdsClient;
    }

    @PreAuthorize("@fideliusRoleService.isAuthorized(#application, #account, \"LIST_CREDENTIALS\")")
    public List<Credential> getAllCredentials(String tableName, String account, String region, String application) throws FideliusException{
        logger.info(String.format("Getting all credentials for app %s using account %s and region %s.", application, account, region));
        AWSEnvironment awsEnvironment = new AWSEnvironment(account, region);
        List<Credential> results = new ArrayList<>();
        DynamoDbClient dynamoDbClient = awsSessionService.getDynamoDBClient(awsEnvironment);
        DynamoDbEnhancedClient dynamoDbEnhancedClient = awsSessionService.getDynamoDBEnhancedClient(dynamoDbClient);

        setFideliusEnvironment(account, region);

//        Map<String, String> ean = new HashMap<>();
//        ean.put("#tempname", NAME);
//
//        Map<String, AttributeValue> eav = new HashMap<>();
//        eav.put(":key", AttributeValue.builder().s(application + ".").build());
//
//        ScanRequest scanRequest = ScanRequest.builder()
//                .tableName(tableName)
//                .filterExpression("begins_with (" + NAME + ", " + application + ".)")
//                .expressionAttributeNames(ean)
//                .expressionAttributeValues(eav)
//                .build();



        List<Map<String, AttributeValue>> queryResults = dynamoDBService.scanDynamoDB(dynamoDbEnhancedClient, tableName, application);

        // Gets only latest version of each credential
        Map<String, Map<String, AttributeValue>> credentials = getLatestCredentialVersion(queryResults);

        for (Map<String, AttributeValue> dbCredential : credentials.values()) {
            if(dbCredential.get(SDLC) == null){
                logger.info(String.format("Credential %s missing attributes.  Attempting to add missing attributes: ", dbCredential.get(NAME)));
                dbCredential = migrateService.guessCredentialProperties(dbCredential);
            }

            try {
                Credential credential = new Credential(getShortKey(dbCredential), dbCredential.get(NAME), account, region, application,
                        dbCredential.get(SDLC), dbCredential.get(COMPONENT), splitRoleARN(dbCredential.get(UPDATED_BY)),
                        dbCredential.get(UPDATED_ON));

                if(credential.getEnvironment() != null)
                    results.add(credential);

            }catch (Exception e){
                logger.error("Error parsing key " + dbCredential.get(NAME), e);
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
        AWSEnvironment awsEnvironment = new AWSEnvironment(account, region);
        DynamoDbClient dynamoDbClient = awsSessionService.getDynamoDBClient(awsEnvironment);
        setFideliusEnvironment(account, region);

        Map<String, String> ean = new HashMap<>();
        ean.put("#tempname", NAME);

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":key", AttributeValue.builder().s(longKey).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .expressionAttributeNames(ean)
                .keyConditionExpression("#tempname = :key")
                .expressionAttributeValues(eav)
                .build();
        List<Map<String, AttributeValue>> queryResults = dynamoDBService.queryDynamoDB(queryRequest, dynamoDbClient);

        // Gets only latest version of each credential
        Map<String, Map<String, AttributeValue>> credentials = getLatestCredentialVersion(queryResults);

        try {
            Map<String, AttributeValue> dbCredential = credentials.values().stream().findFirst().get();
            if(dbCredential.get(SDLC) == null) {
                dbCredential = migrateService.migrateCredential(dbCredential, fideliusService);
            }

            try {
                return (new Credential(getShortKey(dbCredential), dbCredential.get(NAME), account, region, application,
                        dbCredential.get(CredentialsService.SDLC), dbCredential.get(CredentialsService.COMPONENT), splitRoleARN(dbCredential.get(CredentialsService.UPDATED_BY)),
                        dbCredential.get(CredentialsService.UPDATED_ON)));
            }catch (Exception e){
                logger.error("Error parsing key " + dbCredential.get(CredentialsService.NAME).s(), e);
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
        AWSEnvironment awsEnvironment = new AWSEnvironment(account, region);
        DynamoDbClient dynamoDbClient = awsSessionService.getDynamoDBClient(awsEnvironment);
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
        ean.put("#tempname", NAME);

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":key", AttributeValue.builder().s(fullKeyBuilder.toString()).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .expressionAttributeNames(ean)
                .keyConditionExpression("#tempname = :key")
                .expressionAttributeValues(eav)
                .build();

        logger.info(String.format("Retrieving history of credential/metadata %s using account %s and region %s", fullKeyBuilder, account, region));
        List<Map<String, AttributeValue>> queryResults = dynamoDBService.queryDynamoDB(queryRequest, dynamoDbClient);

        for (Map<String, AttributeValue> dbCred : queryResults) {
            String updatedOn = null;
            if(dbCred.get(UPDATED_ON) != null && dbCred.get(UPDATED_ON).s() != null) {
                updatedOn = dbCred.get(UPDATED_ON).s();
            }
            results.add(new HistoryEntry(Integer.parseInt(dbCred.get(VERSION).s()), splitRoleARN(dbCred.get(UPDATED_BY)), updatedOn));
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
    public ResponseEntity rotateCredential(String account, String sourceType, String source, String region, String application, String environment,
                                       String component, String shortKey) {
        setFideliusEnvironment(account, region);
        restTemplate = new RestTemplate();
        String user = fideliusRoleService.getUserProfile().getUserId();
        String accountId = fideliusRoleService.fetchAwsAccountId(account);
        String oAuth2Header = "";
        if(!rotateUrl.isPresent() || rotateUrl.get().isEmpty()) {
            this.logger.error("Password rotation URL not provided. Please ensure that fidelius.rotate.url is set.");
            return new ResponseEntity<>("Password rotation URL not provided.", HttpStatus.BAD_REQUEST);
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
                    String errorMessageFromResponse;
                    try{
                        errorMessageFromResponse = extractErrorMessageFromResponse(e.getResponseBodyAsString());
                    } catch(Exception exception) {
                        this.logger.info("Failed to parse error message from response object.");
                        errorMessageFromResponse = e.getStatusText();
                    }
                    return new ResponseEntity<>(errorMessageFromResponse, e.getStatusCode());
                }
                return new ResponseEntity<String>(HttpStatus.OK);

            } else {
                this.logger.info("Credential not rotated. Source Name or SourceType is null.");
                return new ResponseEntity<>("Credential not rotated. Source Name or SourceType is null.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            this.logger.info("Credential not rotated " + e.toString());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
            Metadata metadata = getMetadata(credential.getAccount(), credential.getRegion(), credential.getApplication(), credential.getEnvironment(), credential.getComponent(), credential.getShortKey());
            if(metadata.getSource() != null && metadata.getSourceType() != null ) {
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

        String metadataValidation = isValidMetadata(metadata);
        if(!metadataValidation.isEmpty()){
            throw new FideliusException("Metadata source is invalid! " + metadataValidation, HttpStatus.BAD_REQUEST);
        }

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
    private String isValidMetadata(Metadata metadata){
        String sourceType = metadata.getSourceType();
        if(sourceType.contains("Service Account")){
            sourceType = "Service Account";
        }
        switch (sourceType){
            case "RDS":
            case "Aurora":
                if(!metadata.getSource().startsWith(metadata.getApplication().toLowerCase())){
                    return metadata.getSourceType() + " sources must start with \"" + metadata.getApplication().toLowerCase() + "\"";
                }
                break;
            case "Service Account":
                if(!metadata.getSource().startsWith("svc_"+metadata.getApplication().toLowerCase())){
                    return "Service Account sources must start with \"svc_" + metadata.getApplication().toLowerCase() + "\"";
                }
                String accountSuffix = accountsService.getAccountByAlias(metadata.getAccount()).getSdlc().toLowerCase().substring(0,1);
                if(!metadata.getSource().endsWith("_"+accountSuffix)){
                    return "Service Account sources must end with \"_" + accountSuffix + "\" in " + metadata.getAccount();
                }
                break;
            default:
                return metadata.getSourceType() + " is an unsupported metadata source type.";
        }
        return "";
    }

    private String splitRoleARN(AttributeValue roleARN) {
        if (roleARN == null || roleARN.s() == null) return null;

        String[] roleTokens = roleARN.s().split(":assumed-role/");
        if (roleTokens.length > 1){
            return roleTokens[1];
        } else {
            return roleTokens[0];
        }
    }

    private Map<String, Map<String, AttributeValue>> getLatestCredentialVersion(List<Map<String, AttributeValue>> queryResults) {
        Map<String, Map<String, AttributeValue>> credentials = new HashMap<>();
        for (Map<String, AttributeValue> dbCredential : queryResults) {
            if (!credentials.containsKey(dbCredential.get("name").s())) {
                credentials.put(dbCredential.get("name").s(), dbCredential);
            }
            else if (Integer.parseInt(credentials.get(dbCredential.get("name").s()).get("version").s()) < Integer.parseInt(dbCredential.get("version").s())) {
                credentials.replace(dbCredential.get("name").s(), dbCredential);
            }
        }

        return credentials;
    }

    private List<String> getAllRDS(String account, String region, String application) throws FideliusException {

        logger.info(String.format("Getting all RDS for account %s and region %s.", account, region));
        List<String> results = new ArrayList<>();

        RdsClient rdsClient = setRDSClient(account, region);
        Filter rdsEngineFilter = Filter.builder().name("engine").values("postgres", "mysql", "oracle-se2", "oracle-ee", "custom-oracle-ee","oracle-ee-cdb", "oracle-se2-cdb").build();
        DescribeDbInstancesResponse response = rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder().filters(rdsEngineFilter).build());
        List<DBInstance> dbList = response.dbInstances();

        for(DBInstance db: dbList) {
            if(db.dbInstanceIdentifier().startsWith(application.toLowerCase())){
                results.add(db.dbInstanceIdentifier());
            }
        }

        while(response.marker() != null){
            response = rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder().marker(response.marker()).filters(rdsEngineFilter).build());
            dbList = response.dbInstances();
            for(DBInstance db: dbList) {
                if(db.dbInstanceIdentifier().startsWith(application.toLowerCase())){
                    results.add(db.dbInstanceIdentifier());
                }
            }
        }

        while(response.marker() != null){
            response = rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder().marker(response.marker()).filters(rdsEngineFilter).build());
            dbList = response.dbInstances();
            for(DBInstance db: dbList) {
                results.add(db.dbInstanceIdentifier());
            }
        }

        return results;
    }

    private List<String> getAllAuroraRegionalCluster(String account, String region, String application) throws FideliusException {

        logger.info(String.format("Getting all Aurora clusters for account %s and region %s.", account, region));
        List<String> results = new ArrayList<>();

        RdsClient amazonRDSClient = setRDSClient(account, region);

        DescribeDbClustersResponse response = amazonRDSClient.describeDBClusters();
        List<DBCluster> dbClusterList = response.dbClusters();

        for(DBCluster cluster: dbClusterList) {
            if(cluster.dbClusterIdentifier().startsWith(application.toLowerCase())){
                results.add(cluster.dbClusterIdentifier());
            }
        }

        while(response.marker() != null){
            response = amazonRDSClient.describeDBClusters(DescribeDbClustersRequest.builder().marker(response.marker()).build());
            dbClusterList = response.dbClusters();
            for(DBCluster cluster: dbClusterList) {
                if(cluster.dbClusterIdentifier().startsWith(application.toLowerCase())){
                    results.add(cluster.dbClusterIdentifier());
                }
            }
        }

        return results;
    }

    public List<String> getMetadataInfo(String account, String region, String sourceType, String application) throws Exception {
        logger.info("Source type: " + sourceType);
        switch (sourceType) {
            case RDS:
                return getAllRDS(account, region, application);
            case AURORA:
                return getAllAuroraRegionalCluster(account, region, application);
            default:
                logger.info("No source names to return for source type: " + sourceType);
                return new ArrayList<>();
        }
    }
    public List<String> getSourceTypes(){
        return Arrays.asList(sourceTypes.split(","));
    }

    public static String getShortKey(Map<String, AttributeValue> secret) {
        if(secret.get("component") != null && secret.get("component").s() != null) {
            Pattern p = Pattern.compile("([-\\w]+)\\.([-\\w]+)\\.([-\\w]+)\\.(\\S+)");
            Matcher m = p.matcher(secret.get("name").s());
            if(m.matches())
                return m.group(4);
            return secret.get("name").s();
        } else {
            Pattern p = Pattern.compile("([-\\w]+)\\.([-\\w]+)\\.(\\S+)");
            Matcher m = p.matcher(secret.get("name").s());
            if(m.matches())
                return m.group(3);
            return secret.get("name").s();
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

    private String extractErrorMessageFromResponse(String responseString) throws ParseException {
        String responseStringCleaned = responseString.trim();
        if(responseStringCleaned.endsWith("\"\"")) {
            responseStringCleaned = responseStringCleaned.substring(0, responseStringCleaned.length()-2);
        }
        JSONObject responseJson = convertStringToJSON(responseStringCleaned.trim());
        String outputString = (String) responseJson.get("output");
        JSONObject outputJson = convertStringToJSON(outputString);
        String errorMessage = (String) outputJson.get("errorMessage");
        logger.info("Error message: " + errorMessage);
        return errorMessage;
    }

    private JSONObject convertStringToJSON(String originalString) throws ParseException {
        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(originalString);
    }

}
