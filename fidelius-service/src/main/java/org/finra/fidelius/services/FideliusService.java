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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kms.AWSKMSClient;
import org.finra.fidelius.FideliusClient;
import org.springframework.stereotype.Service;

@Service
public class FideliusService extends FideliusClient {

    public FideliusService() {
        super();
    }

    public FideliusService(String region) {
        super(region);
    }

    public FideliusService(ClientConfiguration clientConfiguration, AWSCredentialsProvider provider, String region) {
        super(clientConfiguration, provider, region);
    }

    public void setFideliusClient(AmazonDynamoDBClient dynamoDBClient, AWSKMSClient awskmsClient){
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

    public void deleteCredential(String name, String ags,  String sdlc,  String component, String table,
                                 String user) throws Exception {
        try {
            super.deleteCredential(name, ags, sdlc, component, table, user);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }
}
