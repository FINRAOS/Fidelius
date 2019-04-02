# Quick Start

Fidelius includes a simple way to get Fidelius running locally to get you started.

Before you start you need to make sure the right permissions are set in order for the script to create the resources
required to get Fidelius running locally.  The script is able to 

- Create a DynamoDb table with the proper format to store secrets
- Build the required docker containers
- Start Fidelius locally with some services mocked talking to your DynamoDb table in order for you to be allowed to 
experience all of Fidelius features.

Demo Features

- Login as a Dev user
- Login as an Ops user  
- Login as a Master user
- Create secrets
- View secrets
- Update secrets
- Delete secrets

## Prerequisites

Fidelius requires the following tools to be installed and on your $PATH variable:

- Java 8+
- Maven 3+
- NPM 3+
- Docker

### Step 1 - Create a KMS key
 Create a KMS key with an alias `credstash` in AWS KMS.  Reference can be found [here](https://docs.aws.amazon.com/kms/latest/developerguide/create-keys.html)
 
### Step 2 - Create Assume Role
 
Create a AWS role that your role can assume.  This role should be called `Cross_Account_Fidelius`.  This can be changed
in the future.  The role needs to have this IAM permissions.    
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "dynamodb:CreateTable",
                "dynamodb:DescribeTable"
            ],
            "Effect": "Allow",
            "Resource": "arn:aws:dynamodb:<REGION>:<ACCOUNT NUMBER>:table/credential-store"
        },
        {
            "Action": [
                "dynamodb:ListTables"
            ],
            "Effect": "Allow",
            "Resource": "*"
        }
    ]
}
```

### Step 3 - Add KMS permissions
Add KMS permissions to `Cross_Account_Fidelius`
```json 
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "kms:Decrypt"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:kms:us-east-1:AWSACCOUNTID:key/credstash"
    },
    {
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:Query",
        "dynamodb:Scan"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:dynamodb:us-east-1:AWSACCOUNTID:table/credential-store"
    }
  ]
}
```

### Step 4 - Grant Assume Role 
Grant permission for your user role to assume into `Cross_Account_Fidelius`.
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "0",
            "Effect": "Allow",
            "Action": "sts:AssumeRole",
            "Resource": "arn:aws:iam::AWSACCOUNTID:role/Cross_Account_Fidelius"
        }
    ]
}

```

### Step 5 - Update Trust Relationship
Add your user role to Trust Relationship on `Cross_Account_fidelius` 
```json
 arn:aws:iam::AWSACCOUNTID:role/YOUR_USER_ROLE
```

### Step 6 - Set Environment Variables
Set environment variables

If you need a proxy, set environment variable `http_proxy`
```bash 
    export http_proxy=proxy.com:80
```

Set the location of your `AWS_DIRECTORY` that contains your credentials
```bash 
    export AWS_DIRECTORY=~/.aws
```

Set your `AWS_ACCOUNT_NUMBER` that will be used to create the resources needed for Fidelius to launch
```bash
    export AWS_ACCOUNT_NUMBER=12345678910
```

### Step 7 - Run Start Script
Windows 10 users will have to manually build the code and run the containers.  You can follow the steps 
[7A](#7a-build-containers) - [7H](#7h-#7h-start-fidelius-local-environment).

Mac or Linux users can run the following script that will build all the Fidelius code/containers and bring
Fidelius up locally. You don't have to manually run steps 7A-7H, since the script will do this for you.

Refresh your AWS tokens if you need to, otherwise run the start script.
```bash
bash start.sh
```


You should see the following screens in this order: 

```bash
-------------------------------------------------------
|         Building base Fidelius containers           |
-------------------------------------------------------
.
.
.

-------------------------------------------------------
|         Building Fidelius demo services             |
-------------------------------------------------------
.
.
.

-------------------------------------------------------
|         Building Fidelius setup container           |
-------------------------------------------------------
.
.
.

-------------------------------------------------------
|         Building Fidelius backend service           |
-------------------------------------------------------
.
.
.

-------------------------------------------------------
|         Building Fidelius UI                        |
-------------------------------------------------------
.
.
.

-------------------------------------------------------
|         Building Fidelius containers                |
-------------------------------------------------------
.
.
.

-------------------------------------------------------
|          Starting Fidelius Setup                     |
-------------------------------------------------------
.
.
.
-------------------------------------------------------
|          Starting Fidelius Local Environment         |
-------------------------------------------------------
.
.
.

-------------------------------------------------------
|              Fidelius User Endpoints                |
|                                                     |
|         DEV user -      https://localhost:443       |
|         OPS user -      https://localhost:444       |
|         MASTER user -   https://localhost:445       |
|                                                     |
-------------------------------------------------------

```

#### 7A. Build Containers
cd containers

run docker-compose build

#### 7B. Build Fidelius demo services 
cd demo-services/fake-account-service

run mvn clean install
   
#### 7C. Build Fidelius setup container
cd demo-services/fidelius-setup

run mvn clean install  

#### 7D. Build Fidelius backend service 
cd fidelius-service

run mvn clean install

#### 7E. Build Fidelius UI  
cd fidelius-ui

run npm install

npm run build

#### 7F. Build Fidelius containers
run docker-compose -f setup-docker-compose.yml build

run docker-compose -f local-docker-compose.yml build

#### 7G. Start Fidelius Setup
run docker-compose -f setup-docker-compose.yml up

Wait for process to complete

run docker-compose -f local-docker-compose.yml up

### Step 8 - Navigate to Fidelius

Navigate to the links above to experience Fidelius Secrets Manager.  When you are done you can simply 
run the stop script.  


Fidelius User Endpoints

| ROLE   	| URL                   	|
|--------	|-----------------------	|
| Dev    	| https://localhost:443 	|
| Ops    	| https://localhost:444 	|
| Master 	| https://localhost:445 	|

## Next Steps
After having Fidelius running locally with default configurations, you might want to configure the steps so that you
can run or deploy Fidelius for your organization.

The application configurations can be found at [Configuration](prequisites/configuration.md)

