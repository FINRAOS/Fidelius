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

import org.apache.commons.codec.binary.Base64;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JCredStash {
    protected DynamoDbClient dynamoDbClient;
    protected KmsClient kmsClient;
    protected CredStashCrypto cryptoImpl;
    protected StsClient stsClient;
    protected static  List<String> TABLE_HEADERS = Arrays.asList("name", "component", "sdlc", "contents", "version", "updatedBy", "updatedOn", "key", "hmac", "source", "sourceType");

    protected JCredStash() {
        this.dynamoDbClient = DynamoDbClient.builder().build();
        this.kmsClient = KmsClient.builder().build();
        this.cryptoImpl = new CredStashBouncyCastleCrypto();
        this.stsClient = StsClient.builder().overrideConfiguration(ClientOverrideConfiguration.builder().build()).build();
    }

    protected JCredStash(AwsCredentialsProvider awsCredentialsProvider) {
        this.dynamoDbClient = DynamoDbClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .build();
        this.kmsClient = KmsClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .build();
        this.cryptoImpl = new CredStashBouncyCastleCrypto();
        this.stsClient = StsClient.builder().overrideConfiguration(ClientOverrideConfiguration.builder().build()).build();
    }

    protected JCredStash(DynamoDbClient amazonDynamoDBClient, KmsClient awskmsClient) {
        this.dynamoDbClient = amazonDynamoDBClient;
        this.kmsClient = awskmsClient;
        this.cryptoImpl = new CredStashBouncyCastleCrypto();
        this.stsClient = StsClient.builder().overrideConfiguration(ClientOverrideConfiguration.builder().build()).build();
    }

    protected JCredStash(DynamoDbClient dynamoDbClient, KmsClient kmsClient, StsClient stsClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.kmsClient = kmsClient;
        this.cryptoImpl = new CredStashBouncyCastleCrypto();
        this.stsClient = stsClient;
    }

    protected Map<String, AttributeValue> readDynamoItem(String tableName, String secret) {
        // TODO: allow multiple secrets to be fetched by pattern or list
        // TODO: allow specific version to be fetched
        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put("name", Condition.builder()
                .attributeValueList(
                        AttributeValue.builder().s(secret).build()
                )
                .comparisonOperator(ComparisonOperator.EQ)
                .build());
        keyConditions.put("version", Condition.builder()
                .attributeValueList(
                        AttributeValue.builder().s("0").build()
                )
                .comparisonOperator(ComparisonOperator.BEGINS_WITH)
                .build());
        QueryResponse queryResponse = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .limit(1)
                .scanIndexForward(false)
                .consistentRead(true)
                .keyConditions(keyConditions)
                .build()
        );
        if(queryResponse.count() == 0) {
            throw new RuntimeException("Secret " + secret + " could not be found");
        }
        Map<String, AttributeValue> item = queryResponse.items().get(0);

        return item;
    }

    protected QueryResponse getCredentials(String tableName, String secret) {
        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put("name", Condition.builder()
                .attributeValueList(
                        AttributeValue.builder().s(secret).build()
                )
                .comparisonOperator(ComparisonOperator.EQ)
                .build());
        keyConditions.put("version", Condition.builder()
                .attributeValueList(
                        AttributeValue.builder().s("0").build()
                )
                .comparisonOperator(ComparisonOperator.BEGINS_WITH)
                .build());
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .scanIndexForward(false)
                .consistentRead(true)
                .keyConditions(keyConditions)
                .build();

        QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

        if(queryResponse.count() == 0) {
            throw new RuntimeException("Secret " + secret + " could not be found");
        }

        return queryResponse;
    }

    protected String getUpdatedBy() throws Exception {
        try {
            return stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build()).arn();
        } catch(Exception e){
            throw new RuntimeException("Error getting user");
        }
    }

    private ByteBuffer decryptKeyWithKMS(byte[] encryptedKeyBytes, Map<String, String> context) {
        ByteBuffer blob = ByteBuffer.wrap(encryptedKeyBytes);

        DecryptResponse decryptResponse = kmsClient.decrypt(DecryptRequest.builder().ciphertextBlob(SdkBytes.fromByteBuffer(blob)).encryptionContext(context).build());

        return decryptResponse.plaintext().asByteBuffer();
    }

    protected int getHighestVersion(String name, String tableName) {
        HashMap<String, String> attributeName = new HashMap();
        HashMap<String, AttributeValue> attributeValue = new HashMap();
        attributeName.put("#n", "name");
        attributeValue.put(":v_name", AttributeValue.builder().s(name).build());
        QueryRequest spec = QueryRequest.builder()
                .tableName(tableName)
                .scanIndexForward(false)
                .consistentRead(true)
                .keyConditionExpression("#n = :v_name")
                .expressionAttributeValues(attributeValue)
                .expressionAttributeNames(attributeName)
                .projectionExpression("version")
                .build();

        List<Map<String, AttributeValue>> items = dynamoDbClient.query(spec).items();

        int maxVersion = 0;
        for(Map<String, AttributeValue> item : items) {
            int version = Integer.parseInt(item.get("version").s());
            if(version > maxVersion) {
                maxVersion = version;
            }
        }
        return maxVersion;
    }

    // default table name: "credential-store"
    protected String getSecret(String tableName, String secret, Map<String, String> context)  {

        // The secret was encrypted using AES, then the key for that encryption was encrypted with AWS KMS
        // Then both the encrypted secret and the encrypted key are stored in dynamo

        // First find the relevant rows from the credstash table
        Map<String, AttributeValue> dynamoCredential = readDynamoItem(tableName, secret);
        EncryptedCredential encryptedCredential = CredModelMapper.fromDynamo(dynamoCredential);
        return decrypt(encryptedCredential,context);

    }

    protected MetadataParameters getMetadata(String tableName, String metadataKey, Map<String, String> context)  {

        // First find the relevant rows from the credstash table
        Map<String, AttributeValue> dynamoMetadata = readDynamoItem(tableName, metadataKey);
        MetadataParameters metadataParameters = MetadataModelMapper.fromDynamo(dynamoMetadata);
        return metadataParameters;

    }

    protected String decrypt(EncryptedCredential encryptedCredential, Map<String,String> context){
        // First obtain that original key again using KMS
        ByteBuffer plainText = decryptKeyWithKMS(encryptedCredential.getDataKeyBytes(), context);

        // The key is just the first 32 bits, the remaining are for HMAC signature checking
        byte[] keyBytes = new byte[32];
        plainText.get(keyBytes);

        byte[] hmacKeyBytes = new byte[plainText.remaining()];
        plainText.get(hmacKeyBytes);
        byte[] digest = cryptoImpl.digest(hmacKeyBytes, encryptedCredential.getCredentialBytes());
        if(!Arrays.equals(digest, encryptedCredential.getHmacBytes())) {
            throw new RuntimeException("HMAC integrety check failed"); //TODO custom exception type
        }

        // now use AES to finally decrypt the actual secret
        byte[] decryptedBytes = cryptoImpl.decrypt(keyBytes, encryptedCredential.getCredentialBytes());
        return new String(decryptedBytes);
    }

    protected EncryptedCredential encrypt(String name, String credential, String version, String user, String kmsKey, Map<String,String> context){
        // generate a 64 byte key with KMS
        // half for data encryption, other half for HMAC
        GenerateDataKeyRequest dataKeyRequest = GenerateDataKeyRequest.builder()
                .keyId(kmsKey)
                .encryptionContext(context)
                .numberOfBytes(64)
                .build();
        GenerateDataKeyResponse dataKeyResponse = kmsClient.generateDataKey(dataKeyRequest);
        byte[] resultArray = dataKeyResponse.plaintext().asByteArray();
        byte[] dataKey = Arrays.copyOfRange(resultArray, 0, 32);
        byte[] hmacKey = Arrays.copyOfRange(resultArray, 32, resultArray.length);

        // encrypt credential contents using dataKey and create hmac
        // original data key is not used after this point
        // plaintext contents are not used after this point
        CredStashBouncyCastleCrypto crypto = new CredStashBouncyCastleCrypto();
        byte[] encryptedContents = crypto.encrypt(dataKey, credential.getBytes());
        byte[] hmac = crypto.digest(hmacKey, encryptedContents);
        byte[] wrappedKey = dataKeyResponse.ciphertextBlob().asByteArray();

        // format the hmac digest as a string containing only hexadecimal digits
        // see:
        //    HMAC.hexdigest()  https://docs.python.org/3/library/hmac.html
        //    http://stackoverflow.com/questions/1609899/java-equivalent-to-phps-hmac-sha1
        String hmacString = "";
        for (byte b : hmac) {
            hmacString += String.format("%02x", b);
        }

        // Base64 encode the wrapped datakey and contents
        byte[] base64WrappedKey = Base64.encodeBase64(wrappedKey);
        byte[] base64EncryptedContents = Base64.encodeBase64(encryptedContents);
        String wrappedKeyString = "";
        String encryptedContentsString = "";
        try {
            wrappedKeyString = new String(base64WrappedKey, "UTF-8");
            encryptedContentsString = new String(base64EncryptedContents, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String updatedOn = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);

        EncryptedCredential encryptedCredentialObj = new EncryptedCredential()
                .setFullName(name)
                .setCredential(encryptedContentsString)
                .setDatakey(wrappedKeyString)
                .setVersion(version)
                .setHmac(hmacString)
                .setUpdateBy(user)
                .setUpdatedOn(updatedOn)
                .setSdlc(context.get(Constants.FID_CONTEXT_SDLC))
                .setComponent(context.get(Constants.FID_CONTEXT_COMPONENT));
        return encryptedCredentialObj;
    }

    protected MetadataParameters add(String name, String version, String sourceType, String source, String user, String kmsKey, Map<String,String> context){

        String updatedOn = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);

        MetadataParameters metadataParameters = new MetadataParameters()
                .setFullName(name)
                .setSourceType(sourceType)
                .setSource(source)
                .setVersion(version)
                .setUpdateBy(user)
                .setUpdatedOn(updatedOn)
                .setSdlc(context.get(Constants.FID_CONTEXT_SDLC))
                .setComponent(context.get(Constants.FID_CONTEXT_COMPONENT));
        return metadataParameters;
    }

    protected void putSecret(String tableName, String secretName, String contents, String version, String kmsKey, Map<String, String> context) throws Exception {
        putSecret(tableName,  secretName,  contents,  version, null,  kmsKey, context);
    }

    protected void putSecret(String tableName, String secretName, String contents, String version, String user, String kmsKey, Map<String, String> context) throws Exception {

        // tableName defaults to encryptedCredential-store
        tableName = (tableName == null || tableName.length() == 0) ? "encryptedCredential-store" : tableName;

        // version defaults to 1; is zero-padded to length 19
        version = (version == null || version.length() == 0) ? String.format("%019d", 1) : version;

        // User that created secret: Defaults to IAM user
        user = ( user == null || user.length() == 0) ? getUpdatedBy(): user;

        // kmsKey defaults to alias/credstash
        kmsKey = (kmsKey == null || kmsKey.length() == 0) ? Constants.DEFAULT_KMS_KEY : kmsKey;

        // context defaults to empty map
        context = (context == null) ? new HashMap<String, String>() : context;

        EncryptedCredential encryptedCredential = encrypt(secretName,contents,version, user, kmsKey,context);
        final Map<String, AttributeValue> data = CredModelMapper.toDynamo(encryptedCredential);

        HashMap<String, String> cond = new HashMap<>();
        cond.put("#n", "name");
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(data)
                .conditionExpression("attribute_not_exists(#n)")
                .expressionAttributeNames(cond)
                .build();
        dynamoDbClient.putItem(request);
    }

    protected void putMetadata(String tableName, String secretName, String version, String sourceType,
                               String source, String user, String kmsKey, Map<String, String> context) throws Exception {

        // tableName defaults to encryptedCredential-store
        tableName = (tableName == null || tableName.length() == 0) ? "encryptedCredential-store" : tableName;

        // version defaults to 1; is zero-padded to length 19
        version = (version == null || version.length() == 0) ? String.format("%019d", 1) : version;

        // User that created secret: Defaults to IAM user
        user = ( user == null || user.length() == 0) ? getUpdatedBy(): user;

        // kmsKey defaults to alias/credstash
        kmsKey = (kmsKey == null || kmsKey.length() == 0) ? Constants.DEFAULT_KMS_KEY : kmsKey;

        // context defaults to empty map
        context = (context == null) ? new HashMap<String, String>() : context;

        MetadataParameters metadataParameters = add(secretName, version, sourceType, source, user, kmsKey, context);
        final Map<String, AttributeValue> data = MetadataModelMapper.toDynamo(metadataParameters);

        HashMap<String, String> cond = new HashMap<>();
        cond.put("#n", "name");
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(data)
                .conditionExpression("attribute_not_exists(#n)")
                .expressionAttributeNames(cond)
                .build();
        dynamoDbClient.putItem(request);
    }

    protected void deleteSecret(String tableName, String secretName) throws InterruptedException {

        QueryResponse queryResponse = getCredentials(tableName, secretName);
        Map<String, List<WriteRequest>> writeRequestMap = new HashMap<>();
        List<WriteRequest> writeRequests = new ArrayList<>();
        for (Map<String, AttributeValue> item : queryResponse.items()) {
            Map<String, AttributeValue> preppedItemMap = filterItemMapForDeletion(item);
            WriteRequest writeRequest = WriteRequest.builder()
                    .deleteRequest(DeleteRequest.builder().key(preppedItemMap).build()
            ).build();
            writeRequests.add(writeRequest);
        }
        writeRequestMap.put(tableName, writeRequests);
        BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder().requestItems(writeRequestMap).build();

        Map<String, List<WriteRequest>> unprocessed = null ;
        int attempts = 0;
        do {
            if (attempts > 0) {
                // exponential backoff per DynamoDB recommendation
                Thread.sleep((1 << attempts) * 1000);
            }
            attempts++;
            BatchWriteItemResponse batchWriteItemResponse = dynamoDbClient.batchWriteItem(batchWriteItemRequest);
            unprocessed = batchWriteItemResponse.unprocessedItems();
            batchWriteItemRequest = BatchWriteItemRequest.builder().requestItems(unprocessed).build();
        } while (unprocessed.size() > 0 && attempts < 6);

        if(unprocessed.size() > 0)
            throw new RuntimeException("Error deleting secret " + secretName + " with " + unprocessed.size() + " versions not deleted");
    }

    private Map<String, AttributeValue> filterItemMapForDeletion(Map<String, AttributeValue> items) {
        HashMap<String, AttributeValue> populatedItem = new HashMap<>();
        populatedItem.put("name", items.get("name"));
        populatedItem.put("version", items.get("version"));
        return populatedItem;
    }
}
