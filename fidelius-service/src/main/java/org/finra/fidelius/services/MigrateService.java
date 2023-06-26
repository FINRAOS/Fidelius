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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MigrateService {

    /**
     * Name of DynamoDb table that contains credentials
     */
    @Value("${fidelius.dynamoTable}")
    private String tableName;

    /**
     * AGS.SDLC.KEY
     */
    private Pattern threeFieldsPattern = Pattern.compile("([-\\w]+)\\.([-\\w]+)\\.(\\S+)");

    /**
     * AGS.(COMPONENT).SDLC.(KEY or KEY + Extra field)
     */
    private Pattern fourFieldsPattern = Pattern.compile("([-\\w]+)\\.([-\\w]+)\\.([-\\w]+)\\.([-\\w]+)");

    /**
     * AGS.(COMPONENT).SDLC.(KEY or KEY + Extra fields)
     */
    private Pattern extraFieldsPattern = Pattern.compile("([-\\w]+)\\.([-\\w]+)\\.([-\\w]+)\\.(.*)");

    protected FideliusService fideliusService;

    private Logger logger = LoggerFactory.getLogger(MigrateService.class);

    public Map<String, AttributeValue> migrateCredential(Map<String, AttributeValue> dbCredential, FideliusService fideliusService) {
        this.fideliusService = fideliusService;
        Matcher threeFieldsMatcher = threeFieldsPattern.matcher(dbCredential.get(CredentialsService.NAME).s());
        Matcher fourFieldsMatcher = fourFieldsPattern.matcher(dbCredential.get(CredentialsService.NAME).s());
        Matcher extraFieldsMatcher = extraFieldsPattern.matcher(dbCredential.get(CredentialsService.NAME).s());

        if (threeFieldsMatcher.matches()) {
            logger.info("3 Fields: " + dbCredential.get(CredentialsService.NAME).s());
            try {
                String key = threeFieldsMatcher.group(3);
                String ags = threeFieldsMatcher.group(1);
                String sdlc = threeFieldsMatcher.group(2);
                migrate(ags, sdlc, null, key, dbCredential);
            } catch (Exception e) {
                logger.error("Error migrating " + dbCredential.get(CredentialsService.NAME).s());
            }
        }

        if (fourFieldsMatcher.matches() && dbCredential.get(CredentialsService.SDLC) == null) {
            logger.info("4 Fields: " + dbCredential.get(CredentialsService.NAME).s());
            migrate(fourFieldsMatcher, dbCredential);
        }

        if (extraFieldsMatcher.matches() && dbCredential.get(CredentialsService.SDLC) == null) {
            logger.info("More than 4 Fields: " + dbCredential.get(CredentialsService.NAME).s());
            migrate(extraFieldsMatcher, dbCredential);
        }

        if(dbCredential.get(CredentialsService.SDLC) != null)
            logger.info("Successfully retrieved " + dbCredential.get(CredentialsService.NAME).s());
        else {
            logger.error("Failed to migrate: " + dbCredential.get(CredentialsService.NAME).s());
        }

        return dbCredential;
    }

    private void migrate(String ags, String sdlc, String component, String key, Map<String, AttributeValue> dbCredential) throws Exception{
        String user = "FideliusMigrateTask";

        String credentialSecret = fideliusService.getCredential(key, ags, sdlc, component, null, tableName, user);

        if(credentialSecret == null)
            throw new Exception("Error retrieving key");
        else {
            logger.info(dbCredential.get(CredentialsService.NAME).s() + " retrieved");
            dbCredential.put(CredentialsService.SDLC, AttributeValue.builder().s(sdlc).build());
            if(component != null)
                dbCredential.put(CredentialsService.COMPONENT, AttributeValue.builder().s(component).build());
        }
    }

    private void migrate(Matcher matcher, Map<String, AttributeValue> dbCredential){
        try {
            String key = matcher.group(4);
            String ags = matcher.group(1);
            String sdlc = matcher.group(3);
            String component = matcher.group(2);
            migrate(ags, sdlc, component, key, dbCredential);
        } catch(Exception e){
            logger.error("Error retrieving " + dbCredential.get(CredentialsService.NAME).s(), e.getMessage());
            try {
                String key = matcher.group(3)+"."+matcher.group(4);
                String ags = matcher.group(1);
                String sdlc = matcher.group(2);
                migrate(ags, sdlc, null, key, dbCredential);
            } catch(Exception e1){
                logger.error("Error retrieving " + dbCredential.get(CredentialsService.NAME).s(), e.getMessage());
            }
        }
    }

    public Map<String, AttributeValue> guessCredentialProperties(Map<String, AttributeValue> dbCredential) {
        Matcher threeFieldsMatcher = threeFieldsPattern.matcher(dbCredential.get("name").s());
        Map<String, AttributeValue> updatedDbCredential = new HashMap<>(dbCredential);
        String[] nameSplit = dbCredential.get("name").s().split("\\.");

        if (nameSplit.length == 3) {
            logger.info("Parsing " + dbCredential.get("name").s());
            String sdlc = nameSplit[1];
            updatedDbCredential.put("sdlc", AttributeValue.builder().s(sdlc).build());
        } else{
            try {
                String component = nameSplit[1];
                String sdlc = nameSplit[2];
                updatedDbCredential.put("component", AttributeValue.builder().s(component).build());
                updatedDbCredential.put("sdlc", AttributeValue.builder().s(sdlc).build());
            } catch (Exception e) {
                logger.error("Error parsing key " + dbCredential.get("name").s());
            }
        }

        return updatedDbCredential;
    }
}

