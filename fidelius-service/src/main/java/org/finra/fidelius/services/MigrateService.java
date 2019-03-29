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

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.finra.fidelius.model.db.DBCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    public DBCredential migrateCredential(DBCredential dbCredential, FideliusService fideliusService) {
        this.fideliusService = fideliusService;
        Matcher threeFieldsMatcher = threeFieldsPattern.matcher(dbCredential.getName());
        Matcher fourFieldsMatcher = fourFieldsPattern.matcher(dbCredential.getName());
        Matcher extraFieldsMatcher = extraFieldsPattern.matcher(dbCredential.getName());

        if (threeFieldsMatcher.matches()) {
            logger.info("3 Fields: " + dbCredential.getName());
            try {
                String key = threeFieldsMatcher.group(3);
                String ags = threeFieldsMatcher.group(1);
                String sdlc = threeFieldsMatcher.group(2);
                migrate(ags, sdlc, null, key, dbCredential);
            } catch (Exception e) {
                logger.error("Error migrating " + dbCredential.getName());
            }
        }

        if (fourFieldsMatcher.matches() && dbCredential.getSdlc() == null) {
            logger.info("4 Fields: " + dbCredential.getName());
            migrate(fourFieldsMatcher, dbCredential);
        }

        if (extraFieldsMatcher.matches() && dbCredential.getSdlc() == null) {
            logger.info("More than 4 Fields: " + dbCredential.getName());
            migrate(extraFieldsMatcher, dbCredential);
        }

        if(dbCredential.getSdlc() != null)
            logger.info("Successfully retrieved " + dbCredential.getName());
        else {
            logger.error("Failed to migrate: " + dbCredential.getName());
        }

        return dbCredential;
    }

    private void migrate(String ags, String sdlc, String component, String key, DBCredential dbCredential) throws Exception{
        String user = "FideliusMigrateTask";

        String credentialSecret = fideliusService.getCredential(key,ags,sdlc,component, tableName, user);

        if(credentialSecret == null)
            throw new Exception("Error retrieving key");
        else {
            logger.info(dbCredential.getName() + " retrieved");
            dbCredential.setSdlc(sdlc);
            if(component != null)
                dbCredential.setComponent(component);
        }
    }

    private void migrate(Matcher matcher, DBCredential dbCredential){
        try {
            String key = matcher.group(4);
            String ags = matcher.group(1);
            String sdlc = matcher.group(3);
            String component = matcher.group(2);
            migrate(ags, sdlc, component, key, dbCredential);
        } catch(Exception e){
            logger.error("Error retrieving " + dbCredential.getName(), e.getMessage());
            try {
                String key = matcher.group(3)+"."+matcher.group(4);
                String ags = matcher.group(1);
                String sdlc = matcher.group(2);
                migrate(ags, sdlc, null, key, dbCredential);
            } catch(Exception e1){
                logger.error("Error retrieving " + dbCredential.getName(), e.getMessage());
            }
        }
    }

    public DBCredential guessCredentialProperties(DBCredential dbCredential) {
        Matcher threeFieldsMatcher = threeFieldsPattern.matcher(dbCredential.getName());

        if (threeFieldsMatcher.matches()) {
            logger.info("Parsing " + dbCredential.getName());
                String sdlc = threeFieldsMatcher.group(2);
                dbCredential.setSdlc(sdlc);
        } else{
            try {
                String sdlc = dbCredential.getName().split("\\.")[1];
                dbCredential.setSdlc(sdlc);
            } catch (Exception e) {
                logger.error("Error parsing key " + dbCredential.getName());
            }
        }

        return dbCredential;
    }
}

