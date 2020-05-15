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
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import org.apache.commons.codec.binary.Base64;
import com.amazonaws.services.dynamodbv2.document.BatchWriteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JCredStash {
    protected AmazonDynamoDB amazonDynamoDBClient;
    protected AWSKMS awskmsClient;
    protected CredStashCrypto cryptoImpl;
    protected AWSSecurityTokenService awsSecurityTokenService;
    protected DynamoDB dynamoDB;

    protected JCredStash() {
        this.amazonDynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
        this.awskmsClient = AWSKMSClientBuilder.defaultClient();
        this.cryptoImpl = new CredStashBouncyCastleCrypto();
        this.awsSecurityTokenService = AWSSecurityTokenServiceClient.builder().withClientConfiguration(new ClientConfiguration()).build();
        this.dynamoDB = new DynamoDB(amazonDynamoDBClient);
    }

    protected JCredStash(AWSCredentialsProvider awsCredentialsProvider) {
        this.amazonDynamoDBClient = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(awsCredentialsProvider)
                .build();
        this.awskmsClient = AWSKMSClientBuilder.standard()
                .withCredentials(awsCredentialsProvider)
                .build();
        this.cryptoImpl = new CredStashBouncyCastleCrypto();
        this.awsSecurityTokenService = AWSSecurityTokenServiceClient.builder().withClientConfiguration(new ClientConfiguration()).build();
        this.dynamoDB = new DynamoDB(amazonDynamoDBClient);
    }

    protected JCredStash(AmazonDynamoDB amazonDynamoDBClient, AWSKMS awskmsClient) {
        this.amazonDynamoDBClient = amazonDynamoDBClient;
        this.awskmsClient = awskmsClient;
        this.cryptoImpl = new CredStashBouncyCastleCrypto();
        this.awsSecurityTokenService = AWSSecurityTokenServiceClient.builder().withClientConfiguration(new ClientConfiguration()).build();
        this.dynamoDB = new DynamoDB(amazonDynamoDBClient);
    }

    protected JCredStash(AmazonDynamoDB amazonDynamoDBClient, AWSKMS awskmsClient, AWSSecurityTokenService awsSecurityTokenService) {
        this.amazonDynamoDBClient = amazonDynamoDBClient;
        this.awskmsClient = awskmsClient;
        this.cryptoImpl = new CredStashBouncyCastleCrypto();
        this.awsSecurityTokenService = awsSecurityTokenService;
        this.dynamoDB = new DynamoDB(amazonDynamoDBClient);
    }

    protected Map<String, AttributeValue> readDynamoItem(String tableName, String secret) {
        // TODO: allow multiple secrets to be fetched by pattern or list
        // TODO: allow specific version to be fetched
        QueryResult queryResult = amazonDynamoDBClient.query(new QueryRequest(tableName)
                .withLimit(1)
                .withScanIndexForward(false)
                .withConsistentRead(true)
                .addKeyConditionsEntry("name", new Condition()
                        .withComparisonOperator(ComparisonOperator.EQ)
                        .withAttributeValueList(new AttributeValue(secret)))
        );
        if(queryResult.getCount() == 0) {
            throw new RuntimeException("Secret " + secret + " could not be found");
        }
        Map<String, AttributeValue> item = queryResult.getItems().get(0);

        return item;
    }

    protected QueryResult getCredentials(String tableName, String secret) {
        QueryRequest queryRequest = new QueryRequest(tableName)
                .withScanIndexForward(false)
                .withConsistentRead(true)
                .addKeyConditionsEntry("name", new Condition()
                        .withComparisonOperator(ComparisonOperator.EQ)
                        .withAttributeValueList(new AttributeValue(secret)));

        QueryResult queryResult = amazonDynamoDBClient.query(queryRequest);

        if(queryResult.getCount() == 0) {
            throw new RuntimeException("Secret " + secret + " could not be found");
        }

        return queryResult;
    }

    protected String getUpdatedBy() throws Exception {
        try {
            return awsSecurityTokenService.getCallerIdentity(new GetCallerIdentityRequest()).getArn();
        } catch(Exception e){
            throw new RuntimeException("Error getting user");
        }
    }

    private ByteBuffer decryptKeyWithKMS(byte[] encryptedKeyBytes, Map<String, String> context) {
        ByteBuffer blob = ByteBuffer.wrap(encryptedKeyBytes);

        DecryptResult decryptResult = awskmsClient.decrypt(new DecryptRequest().withCiphertextBlob(blob).withEncryptionContext(context));

        return decryptResult.getPlaintext();
    }

    protected int getHighestVersion(String name, String tableName) {

        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient);
        Table table = dynamoDB.getTable(tableName);

        QuerySpec spec = new QuerySpec()
                .withScanIndexForward(false)
                .withConsistentRead(true)
                .withKeyConditionExpression("#n = :v_name")
                .withValueMap(new ValueMap()
                        .withString(":v_name", name)
                )
                .withNameMap(new NameMap()
                        .with("#n", "name")
                )
                .withProjectionExpression("version");

        ItemCollection<QueryOutcome> items = table.query(spec);

        Integer maxVersion = 0;
        Iterator<Item> iter = items.iterator();
        while (iter.hasNext()) {
            Item next = iter.next();
            Integer version = new Integer((String) next.get("version"));
            if (version.compareTo(maxVersion) > 0) {
                maxVersion = version.intValue();
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
        GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest()
                .withKeyId(kmsKey)
                .withEncryptionContext(context)
                .withNumberOfBytes(64);
        GenerateDataKeyResult dataKeyResult = awskmsClient.generateDataKey(dataKeyRequest);
        byte[] resultArray = dataKeyResult.getPlaintext().array();
        byte[] dataKey = Arrays.copyOfRange(resultArray, 0, 32);
        byte[] hmacKey = Arrays.copyOfRange(resultArray, 32, resultArray.length);

        // encrypt credential contents using dataKey and create hmac
        // original data key is not used after this point
        // plaintext contents are not used after this point
        CredStashBouncyCastleCrypto crypto = new CredStashBouncyCastleCrypto();
        byte[] encryptedContents = crypto.encrypt(dataKey, credential.getBytes());
        byte[] hmac = crypto.digest(hmacKey, encryptedContents);
        byte[] wrappedKey = dataKeyResult.getCiphertextBlob().array();

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
        PutItemRequest request = new PutItemRequest(tableName, data)
                .withConditionExpression("attribute_not_exists(#n)")
                .withExpressionAttributeNames(cond);
        amazonDynamoDBClient.putItem(request);
    }

    protected void deleteSecret(String tableName, String secretName) throws InterruptedException {

        QueryResult queryResult = getCredentials(tableName, secretName);

        TableWriteItems itemsToDelete = new TableWriteItems(tableName);
        for ( Map<String, AttributeValue> item :queryResult.getItems()) {
            itemsToDelete.addHashAndRangePrimaryKeyToDelete(
                    "name", item.get("name").getS(),
                    "version", item.get("version").getS());
        }

        Map<String, List<WriteRequest>> unprocessed = null ;
        int attempts = 0;
        do {
            if (attempts > 0) {
                // exponential backoff per DynamoDB recommendation
                Thread.sleep((1 << attempts) * 1000);
            }
            attempts++;
            BatchWriteItemOutcome outcome;
            if (unprocessed == null || unprocessed.size() > 0) {
                // handle initial request
                outcome = dynamoDB.batchWriteItem(itemsToDelete);
            } else {
                // handle unprocessed items
                outcome = dynamoDB.batchWriteItemUnprocessed(unprocessed);
            }
            unprocessed = outcome.getUnprocessedItems();
        } while (unprocessed.size() > 0 && attempts < 6);

        if(unprocessed.size() > 0)
            throw new RuntimeException("Error deleting secret " + secretName + " with " + unprocessed.size() + " versions not deleted");
    }
}
