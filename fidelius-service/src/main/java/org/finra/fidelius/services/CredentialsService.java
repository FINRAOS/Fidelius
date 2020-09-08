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
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import org.finra.fidelius.exceptions.FideliusException;
import org.finra.fidelius.model.Credential;
import org.finra.fidelius.model.HistoryEntry;
import org.finra.fidelius.model.aws.AWSEnvironment;
import org.finra.fidelius.model.db.DBCredential;
import org.finra.fidelius.services.auth.FideliusRoleService;
import org.finra.fidelius.services.aws.AWSSessionService;
import org.finra.fidelius.services.aws.DynamoDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
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
    private DynamoDBService dynamoDBService;

    @Inject
    private MigrateService migrateService;

    @Value("${fidelius.kmsKey}")
    private String kmsKey;

    private Logger logger = LoggerFactory.getLogger(CredentialsService.class);

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
    public List<HistoryEntry> getCredentialHistory(String tableName, String account, String region, String application, String environment, String component, String key) throws FideliusException {
        List<HistoryEntry> results = new ArrayList<>();
        DynamoDBMapper mapper = dynamoDBService.createMapper(account, region, tableName);
        setFideliusEnvironment(account, region);

        StringBuilder fullKeyBuilder = new StringBuilder();
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

        logger.info(String.format("Retrieving history of credential %s using account %s and region %s", fullKeyBuilder, account, region));
        List<DBCredential> queryResults = dynamoDBService.queryDynamoDB(queryExpression, DBCredential.class, mapper);

        for (DBCredential dbCred : queryResults) {
            results.add(new HistoryEntry(new Integer(dbCred.getVersion()), splitRoleARN(dbCred.getUpdatedBy()), dbCred.getUpdatedDate()));
        }

        logger.info(String.format("Found %d entries for credential %s.", results.size(), fullKeyBuilder));
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
            String version = fideliusService.putCredential(credential.getShortKey(), credential.getSecret(),
                    credential.getApplication(), credential.getEnvironment(), credential.getComponent(), tableName, user, kmsKey);
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
                credential.getComponent(), credential.getShortKey());
        if (!existingCredentials.isEmpty()) {
            throw new FideliusException("Credential already exists!", HttpStatus.BAD_REQUEST);
        }

        // Add the new credential
        return putCredential(credential);
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
            fideliusService.deleteCredential(credential.getShortKey(), credential.getApplication(),
                    credential.getEnvironment(), credential.getComponent(), tableName, user);
        } catch (Exception e) {
            this.logger.info("Credential not deleted " + e.toString());
            return null;
        }

        return credential;
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
}
