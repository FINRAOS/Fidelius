# Fidelius Client Methods
## getCredential

Retrieves the most recent version of the encryptedCredential as plaintext. 

### Examples:

- Fetches Application/SDLC/Component values from either Environment variables or from  instance tags when running on EC2 instance

    ``` java
    String cred = fideliusClient.getCredential("encryptedCredential");
    ```

-  Uses Application/SDLC/Component values provided manually


    ``` java
    String cred2 = fideliusClient.getCredential("encryptedCredential", "Application", "SDLC", "Component");
    ```

## putCredential

Encrypts and puts encryptedCredential into the table. If the encryptedCredential exists, puts the encryptedCredential 
with an incremented version number. 

### Examples:

- Fetches Application/SDLC/Component values from either Environment variables or from  instance tags when running on 
EC2 instance

    ``` java
    fideliusClient.putCredential("encryptedCredential", "contents as plaintext");
    ```

- Fetches Application/SDLC/Component values from either Environment variables or from  instance tags when running on 
EC2 instance with specific table-name and KMS Key.
    
    ``` java
    fideliusClient.putCredential("encryptedCredential", "contents as plaintext", "table-name", "KMS key");
    ```
    
- Uses Application/SDLC/Component values provided manually
    ``` java
    fideliusClient.putCredential("encryptedCredential", "contents as plaintext", "Application", "SDLC", "Component", "table-name", "KMS key");
    ```



Default Values (if passed as `null`):

* table: "credential-store"
* kmsKey: "alias/credstash"

## deleteCredential

Removes entries corresponding to the specified credential from the database. 

### Examples:

- Deletes a credential getting Application/SDLC/Component values from either Environment variables or from instance tags when running on 
EC2 instance

    ``` java
    fideliusClient.deleteCredential("encryptedCredential");
    ```

- Deletes a credential "Application.Component.SDLC.encryptedCredential" from the table "table-name".

    ``` java
    fideliusClient.deleteCredential("credentialName", "Application", "SDLC", "Component", "table-name");
    ```