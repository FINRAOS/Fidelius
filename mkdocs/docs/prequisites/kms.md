# KMS

## Overview
Fidelius uses KMS service to encrypt and decrypt secrets into DynamoDb table.  
KMS uses the 3 tags to pass on to the encryption context.  The Encryption context
allows the ability to restrict decrypt/encrypt access from different applications or
from specific components.  

### Application
Application is a tag that can be used to restrict access to decrypt/encrypt secrets based
on roles with decrypt/encrypt for a specific Application.  An example could be a team within a 
larger organization that has access to a specific application such as 'FIDELIUS'. Attempts
to decrypt a secret such as `FIDELIUS.dev.application` from role from another team such as 'DEVOPS'
would be denied.

``` json
{
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::111122223333:role/RoleForExampleApp"
  },
  "Action": [
    "kms:Encrypt",
    "kms:Decrypt"
  ],
  "Resource": "arn:aws:kms:us-west-2:111122223333:key/*",
  "Condition": {
    "StringEquals": {
      "kms:EncryptionContext:Application": "FIDELIUS"
    }
  }
}

```

### Component (_Optional_)
Component is a tag that can be used to further restrict access to decrypt/encrypt secrets
for a specific application.  For example, you might want to restrict access so that
only the backend-service of an application can decrypt something like a database password.
In this case then if a secret could look something like this `FIDELIUS.dev.backend-service.database`.
If the application does not contain that backend-service role, then 'backend-service' won't get
attached to the encryption-context and therefore deny access to decrypt.

``` json
{
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::111122223333:role/RoleForExampleApp"
  },
  "Action": [
    "kms:Encrypt",
    "kms:Decrypt"
  ],
  "Resource": "arn:aws:kms:us-west-2:111122223333:key/*",
  "Condition": {
    "StringEquals": {
      "kms:EncryptionContext:Application": "FIDELIUS",
      "kms:EncryptionContext:Component": "backend-service"
    }
  }
}

```

### SDLC
The SDLC represents the Software Development Life Cycle for the secret.  This can be used
to differentiate between multiple secrets within the same Account.  This could be something
like having both a QA environment and a QA-int environment where one could contain a different
set of connections to specific databases used.  An example could be `FIDELIUS.qa.database` and 
`Fidelius.qa-int.database`.  One would fetch the database secret for the dev database and another
would fetch the secret for the dev-int database.

It is recommended to maintain different AWS accounts per SDLC as this would ensure the proper
isolation between SDLC such as a DEV, QA, and PROD environment.  This should also be applied
to Fidelius and the Credential table.  Each Account would have a DynamoDb table that only contain
secrets from that Account.

``` json
{
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::111122223333:role/RoleForExampleApp"
  },
  "Action": [
    "kms:Encrypt",
    "kms:Decrypt"
  ],
  "Resource": "arn:aws:kms:us-west-2:111122223333:key/*",
  "Condition": {
    "StringEquals": {
      "kms:EncryptionContext:Application": "FIDELIUS",
      "kms:EncryptionContext:SDLC": "qa"
    }
  }
}

```