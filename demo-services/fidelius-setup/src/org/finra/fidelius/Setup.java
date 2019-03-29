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
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.*;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Setup {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Setup.class);

    static String DYNAMO_TABLE = System.getenv("fidelius.dynamoTable");
    static String KMS_KEY = System.getenv("fidelius.kmsKey");
    static String REGION = System.getenv("cloud.aws.region");
    static String PROXY_HOST = System.getenv("fidelius.aws.proxyHost");
    static String PROXY_PORT = System.getenv("fidelius.aws.proxyPort");
    static String AWS_ACCOUNT_NUMBER = System.getenv("AWS_ACCOUNT_NUMBER");
    static String ASSUME_ROLE = System.getenv("fidelius.assumeRole");

    static List<String> applications = Arrays.asList("TESTAPP", "SECRETAPP", "TOPSECRETAPP");

    public static void main(String[] args){

        checkForEnvironmentVariables();

        createCredentialStore();

        AWSCredentialsProvider provider = getAwsCredentialsProvider();

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        if(PROXY_HOST != null && PROXY_PORT != null) {
            clientConfiguration.setProxyHost(PROXY_HOST);
            try {
                clientConfiguration.setProxyPort(Integer.parseInt(PROXY_PORT));
            } catch (NumberFormatException e){
                logger.error("Error converting Proxy Port " + PROXY_PORT + " to an integer", e);
            } catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }
        }

        FideliusClient fideliusClient = new FideliusClient(clientConfiguration, provider, REGION);

        insertTestCredentials(fideliusClient);

    }

    public static void insertTestCredentials(FideliusClient fideliusClient){
        try {
            logger.info("Inserting test credentials");
            for(String application: applications) {
                fideliusClient.putCredential(application.toLowerCase()+"_user", application+"_SECRET", application, "dev", "ldap", DYNAMO_TABLE, "bryand", KMS_KEY);

                fideliusClient.putCredential("database", application+"_SECRET", application, "dev", "request-service", DYNAMO_TABLE, "bryand", KMS_KEY);
                fideliusClient.putCredential("database", application+"_SECRET", application, "dev", "request-service", DYNAMO_TABLE, "mesaw", KMS_KEY);
                fideliusClient.putCredential("database", application+"_SECRET", application, "dev", "request-service", DYNAMO_TABLE, "hudsonv", KMS_KEY);
                fideliusClient.putCredential("database", application+"_SECRET", application, "dev", "request-service", DYNAMO_TABLE, KMS_KEY);

                fideliusClient.putCredential("database", application+"_SECRET_QA", application, "qa", "request-service", DYNAMO_TABLE, "bryand", KMS_KEY);
                fideliusClient.putCredential("database", application+"_SECRET_QA", application, "qa", "request-service", DYNAMO_TABLE, "AnderJ", KMS_KEY);

                fideliusClient.putCredential("database", application+"_SECRET_PROD", application, "prod", null, DYNAMO_TABLE, "wellsh", KMS_KEY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void checkForEnvironmentVariables(){
        if(DYNAMO_TABLE == null){
            logger.error("Missing fidelius.dynamoTable environment");
            System.exit(1);
        } else {
            logger.info("Using DynamoTable " + DYNAMO_TABLE);
        }

        if(KMS_KEY == null){
            logger.error("Missing fidelius.kmsKey environment");
            System.exit(1);
        } else {
            logger.info("Using KMS key " + KMS_KEY);
        }

        if(AWS_ACCOUNT_NUMBER == null) {
            logger.error("Missing AWS_ACCOUNT_NUMBER environment");
            System.exit(1);
        } else{
            logger.info("Using AWS Account Number " + AWS_ACCOUNT_NUMBER);
        }

        if(ASSUME_ROLE == null) {
            logger.error("Missing fidelius.assumeRole environment");
            System.exit(1);
        } else {
            logger.info("Using Assume Role " + ASSUME_ROLE);
        }

        if(REGION == null) {
            logger.error("Missing could.aws.region environment");
            System.exit(1);
        } else {
            logger.info("Using Region " + REGION);
        }

        logger.info("Using Proxy Host " + PROXY_HOST);
        logger.info("Using Proxy Port " + PROXY_PORT);
    }

    public static void createCredentialStore(){
        Process p = null;
        try {
            logger.info("Creating Dynamo table " + DYNAMO_TABLE);
            p = Runtime.getRuntime().exec("credstash -t " + DYNAMO_TABLE +  " setup");
        } catch (Exception e) {
            e.printStackTrace();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        String response;
        try {
           response = in.readLine();
            in.close();
            if(response != null)
                logger.info(response);

            if (response != null && response.equals("Credential Store table already exists")) {
                logger.info("Exiting");
                p.destroy();
                System.exit(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        String line = null;
        try {
            while ((line = error.readLine()) != null)
                logger.error(line);

            error.close();
            p.destroy();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static AWSSecurityTokenServiceClient createSecurityTokenServiceClient() {
        return new AWSSecurityTokenServiceClient(new ClientConfiguration());
    }

    public static AWSCredentialsProvider getAwsCredentialsProvider(){

        AssumeRoleRequest assumeRequest = new AssumeRoleRequest()
                .withRoleArn(getRole())
                .withRoleSessionName("FIDELIUS");

        final AssumeRoleResult assumeResult = createSecurityTokenServiceClient().assumeRole(assumeRequest);

        final AWSCredentialsProvider provider = new AWSCredentialsProvider() {
            public AWSCredentials getCredentials() {
                BasicSessionCredentials credentials = new BasicSessionCredentials(
                        assumeResult.getCredentials().getAccessKeyId(),
                        assumeResult.getCredentials().getSecretAccessKey(),
                        assumeResult.getCredentials().getSessionToken()
                );
                return credentials;
            }

            public void refresh() {

            }
        };

        return provider;
    }

    public static String getRole(){
        StringBuffer sb = new StringBuffer();
        sb.append("arn:aws:iam::");
        sb.append(AWS_ACCOUNT_NUMBER);
        sb.append(":role/");
        sb.append(ASSUME_ROLE);

        return sb.toString();
    }
}
