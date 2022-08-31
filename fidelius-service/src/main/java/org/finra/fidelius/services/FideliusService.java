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
import org.finra.fidelius.FideliusClient;
import org.finra.fidelius.MetadataParameters;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.kms.KmsClient;

@Service
public class FideliusService extends FideliusClient {

    public FideliusService() {
        super();
    }

    public FideliusService(String region) {
        super(region);
    }

    public FideliusService(ClientOverrideConfiguration clientConfiguration, AwsCredentialsProvider provider, String region) {
        super(clientConfiguration, provider, region);
    }

    public void setFideliusClient(DynamoDbClient dynamoDBClient, KmsClient awskmsClient){
        super.setFideliusClient(dynamoDBClient, awskmsClient);
    }

    public String getCredential(String name, String ags,  String sdlc,  String component,
                                String table, String user) throws Exception {
        return super.getCredential(name, ags, sdlc, component, table, user, false);
    }

    public String putCredential(String name, String contents, String ags, String sdlc, String component,
                                  String table, String user, String kmsKey) throws Exception {
       return super.putCredential(name, contents, ags, sdlc, component, table, user, kmsKey);
    }

    public String putCredentialWithMetadata(String name, String contents, String ags, String sdlc, String component, String source,
                                String sourceType, String table, String user, String kmsKey) throws Exception {
        return super.putCredentialWithMetadata(name, contents, ags, sdlc, component, source, sourceType, table, user, kmsKey);
    }

    public void deleteCredential(String name, String ags,  String sdlc,  String component, String table,
                                 String user) throws Exception {
        try {
            super.deleteCredential(name, ags, sdlc, component, table, user);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public void deleteCredentialWithMetadata(String name, String ags,  String sdlc,  String component, String table,
                                 String user) throws Exception {
        try {
            super.deleteCredentialWithMetadata(name, ags, sdlc, component, table, user);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public MetadataParameters getMetadata(String name, String ags, String sdlc, String component,
                                          String table, String user) throws Exception {
        return super.getMetadata(name, ags, sdlc, component, table, user, false);
    }

    public String putMetadata(String name, String ags, String sdlc, String component, String sourceType,
                              String source, String table, String user, String kmsKey) throws Exception {
        return super.putMetadata(name, ags, sdlc, component, sourceType, source, table, user, kmsKey);
    }

    public void deleteMetadata(String name, String ags,  String sdlc,  String component, String table,
                                 String user) throws Exception {
        try {
            super.deleteMetadata(name, ags, sdlc, component, table, user);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }


}
