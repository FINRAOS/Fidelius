# IAM roles

## Overview
Different IAM roles need to be setup so that Fidelius can properly restrict access and so 
that the application can properly decrypt/encrypt secrets across all applications


## Cross Account Role
The Fidelius backend service should be able to assume role into a cross account role created on every account
with the following permissions.

This role IAM role should contain the following permissions:
```json

{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "kms:Describe*",
                "kms:DescribeKey",
                "kms:Get*",
                "kms:*Grant",
                "kms:List*",
                "kms:Encrypt",
                "kms:Decrypt",
                "kms:ReEncrypt*",
                "kms:Generate*"
            ],
            "Resource": "arn:aws:kms:us-west-1:111111111111:key/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        },
        {
            "Effect": "Allow",
            "Action": [
                "kms:ListAliases",
                "kms:ListKeys"
            ],
            "Resource": [
                "*"
            ]
        }
    ]
}

```

### Trust Relationships
The cross account role should be assumed by the role used to launch the Fidelius backend service
``` json
 arn:aws:iam::111111111111:role/FIDELIUS_BACKEND_SERVICE
```

## Fidelius Backend Service
This role is used to launch the backend service.  This role should have permission to assume each
of the cross account roles in all accounts with desired access.  That should look like this:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "0",
            "Effect": "Allow",
            "Action": "sts:AssumeRole",
            "Resource": "arn:aws:iam::*:role/Cross_Account_Fidelius"
        }
    ]
}
```

## Application Roles
Each Application should have an IAM role that restricts the Application's encrypt/decrypt
for that specific Encryption-Context such as Application, Component, and SDLC.

#### APP
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "**ID**",
            "Effect": "Allow",
            "Action": [
                "dynamodb:BatchGetItem",
                "dynamodb:BatchWriteItem",
                "dynamodb:DescribeStream",
                "dynamodb:DescribeTable",
                "dynamodb:GetItem",
                "dynamodb:GetRecords",
                "dynamodb:ListStreams",
                "dynamodb:ListTables",
                "dynamodb:PutItem",
                "dynamodb:Query",
                "dynamodb:Scan"
            ],
            "Resource": [
                "arn:aws:dynamodb:*:*:table/credential-store"
            ],
            "Condition": {
                "ForAllValues:StringLike": {
                    "dynamodb:LeadingKeys": [
                        "APP.*"
                    ]
                }
            }
        }
    ]
}
```
`APP.*` represents that this app can decrypt/encrypt secrets with the Application field 'APP' with any 'Component' and any 'SDLC'.

#### SDLC
```json

{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "**ID**",
            "Effect": "Allow",
            "Action": [
                "dynamodb:BatchGetItem",
                "dynamodb:BatchWriteItem",
                "dynamodb:DescribeStream",
                "dynamodb:DescribeTable",
                "dynamodb:GetItem",
                "dynamodb:GetRecords",
                "dynamodb:ListStreams",
                "dynamodb:ListTables",
                "dynamodb:PutItem",
                "dynamodb:Query",
                "dynamodb:Scan"
            ],
            "Resource": [
                "arn:aws:dynamodb:*:*:table/credential-store"
            ],
            "Condition": {
                "ForAllValues:StringLike": {
                    "dynamodb:LeadingKeys": [
                        "APP.dev.*",
                        "APP.*.dev.*"
                    ]
                }
            }
        }
    ]
}
```
`APP.dev` represents that this app can decrypt/encrypt secrets with the Application field 'APP' on 'dev' only and any 'Component'.

#### Component
```json

{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "**ID**",
            "Effect": "Allow",
            "Action": [
                "dynamodb:BatchGetItem",
                "dynamodb:BatchWriteItem",
                "dynamodb:DescribeStream",
                "dynamodb:DescribeTable",
                "dynamodb:GetItem",
                "dynamodb:GetRecords",
                "dynamodb:ListStreams",
                "dynamodb:ListTables",
                "dynamodb:PutItem",
                "dynamodb:Query",
                "dynamodb:Scan"
            ],
            "Resource": [
                "arn:aws:dynamodb:*:*:table/credential-store"
            ],
            "Condition": {
                "ForAllValues:StringLike": {
                    "dynamodb:LeadingKeys": [
                        "APP.database.dev.*"
                    ]
                }
            }
        }
    ]
}
```
`APP.database.dev` represents that this app can only decrypt/encrypt secrets with the Application field 'APP' on 'dev' only and with 'Component', 'database'.