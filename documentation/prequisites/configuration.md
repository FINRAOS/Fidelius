# Configuration

## Overview
Fidelius requires some configuration for it to run in your environment, see below for all of the supported configuration parameters

### Authorization
Currently Fidelius only supports authorization through LDAP, the application expects authentication to be done via SSO, and looks for the username in a header.

| Property                               	| Description                                                                                                                                                                             	| Type    	|
|----------------------------------------	|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------	|---------	|
| spring.ldap.contextSource.url          	| The Endpoint Fidelius calls to fetch the account data for all of your AWS accounts.                                                                                                     	| string  	|
| spring.ldap.contextSource.uri          	| The URI where Fidelius can call your account Info service.                                                                                                                              	| string  	|
| spring.ldap.contextSource.userDn       	| The DN for the user that gatekeeper connects as to query ldap (e.g. cn=admin,dc=example,dc=org )                                                                                        	| string  	|
| aws.proxy.host                         	| (Optional) The Proxy Host. If you are not behind a proxy you can ignore this                                                                                                            	| string  	|
| aws.proxy.port                         	| (Optional) The Proxy Port. If you are not behind a proxy you can ignore this                                                                                                            	| string  	|
| spring.ldap.contextSource.base         	| The base that LDAP calls will be made off of (e.g. dc=example,dc=org)                                                                                                                   	|  string 	|
| spring.ldap.contextSource.password     	| The password to the ldap user                                                                                                                                                           	| string  	|
| fidelius.auth.userIdHeader             	| The header in which Fidelius looks to extract the authenticated user                                                                                                                    	| string  	|
| fidelius.auth.masterGroupsPattern      	| A regular expression that is used to extract master group names from the LDAP results. The regular expression must have exactly one capture ( e.g developer_([A-Za-z0-9]+)_dev) pattern 	| string  	|
| fidelius.auth.opsGroupsPattern         	| A regular expression that is used to extract ops group names from the LDAP results. The regular expression must have exactly one capture ( e.g developer_([A-Za-z0-9]+)_dev) pattern    	| string  	|
| fidelius.auth.devGroupsPattern         	| A regular expression that is used to extract dev group names from the LDAP results. The regular expression must have exactly one capture ( e.g developer_([A-Za-z0-9]+)_dev) pattern    	| string  	|
| fidelius.auth.ldap.IsActiveDirectory   	| Whether your LDAP server is Microsoft Active Directory or not (Nested groups are not supported with Non-Active Directory LDAP servers)                                                  	| boolean 	|
| fidelius.auth.ldap.objectClass         	| The Object class to look for users with (ex. posixAccount, person, user)                                                                                                                	| string  	|
| fidelius.auth.ldap.usersIdAttribute    	| The uid                                                                                                                                                                                 	| string  	|
| fidelius.auth.ldap.usersNameAttribute  	| The name                                                                                                                                                                                	| string  	|
| fidelius.auth.ldap.usersEmailAttribute 	| The email                                                                                                                                                                               	| string  	|
| fidelius.auth.ldap.usersDnAttribute    	| The DN for the user that Fidelius connects as to query ldap (e.g. cn=admin,dc=example,dc=org )                                                                                          	| string  	|
| fidelius.auth.ldap.pattern             	| A regular expression that is used to extract group names from the LDAP results. The regular expression must have exactly one capture ( e.g developer_([A-Za-z0-9]+)_dev) pattern        	| string  	|
| fidelius.auth.ldap.groupsBase          	| The base where your groups are stored on your organization's LDAP server (e.g. ou=groups)                                                                                               	| string  	|
| fidelius.auth.ldap.alternativeGroupsBase  | (Optional) An alternative base where your groups are stored on your organization's LDAP server (e.g. ou=groups)                                                                           | string  	|
| fidelius.auth.ldap.server              	| The domain of the LDAP server that gatekeeper should connect to                                                                                                                         	| string  	|
| fidelius.auth.ldap.usersBase           	| The base in which the Users are stored on the LDAP Server (e.g. ou=Users,dc=example,dc=org)                                                                                             	| string  	|
| fidelius.auth.ldap.alternativeUsersBase   | (Optional) An alternative base in which the Users are stored on the LDAP Server (e.g. ou=Users,dc=example,dc=org)                                                                         | string  	|
| fidelius.auth.ldap.base                	| The base that LDAP calls will be made off of (e.g. dc=example,dc=org)                                                                                                                   	| string  	|
| fidelius.auth.ldap.usersCnAttribute    	| The cn                                                                                                                                                                                  	| string  	|
|                                        	|                                                                                                                                                                                         	|         	|


## Application
| Property                                       	| Description                                                                        	                                | Type   	|
|------------------------------------------------	|-------------------------------------------------------------------------------------------------------------------	|--------	|
| fidelius.membership-server-url                 	| The endpoint of the service Fidelius uses to fetch the Ops/Master memberships      	                                | string 	|
| fidelius.membership-server-uri                 	| The URI Fidelius can use to call the membership service for OPS/Master memberships 	                                | string 	|
| fidelius.dynamoTable                           	| Name of DynamoDB table to be used to store secrets.                                	                                | string 	|
| fidelius.javax.contentSecurityPolicy 	            | (Optional) Content-Security-Policy header to be appended to "default-src 'self' 'unsafe-inline' 'unsafe-eval'; "      | string 	|
| fidelius.kmsKey                                	| KMS key used to encrypt/decrypt secrets.                                           	                                | string 	|
| fidelius.rotate.serviceAccountPattern         	| When rotating a service account, the regex for valid service account names. No validation if omitted. 	            | string 	|
| fidelius.rotate.url         	                    | Secret rotation endpoint URL. 	                                                                                    | string 	|
| fidelius.rotate.uri         	                    | Secret rotation endpoint URI. 	                                                                                    | string 	|
| fidelius.validActiveDirectoryRegularExpression 	| Regular Expression used to validate secrets marked labeled Active Directory.       	                                | string 	|
| fidelius.validActiveDirectoryDescription       	| Description of Regular Expression to guide users to enter valid secret             	                                | string 	|


### AWS

| Property                    	| Description                                                                               	| Type   	|
|-----------------------------	|-------------------------------------------------------------------------------------------	|--------	|
| fidelius.account-server-url 	| The Endpoint Fidelius calls to fetch the account data for all of your AWS accounts.       	| string 	|
| fidelius.account-server-uri 	| The URI where Fidelius can call your account Info service.                                	| string 	|
| aws.proxy.host              	| (Optional) The Proxy Host. If you are not behind a proxy you can ignore this              	| string 	|
| aws.proxy.port              	| (Optional) The Proxy Port. If you are not behind a proxy you can ignore this              	| string 	|
| fidelius.assumeRole         	| The AWS IAM role that Fidelius will assume to interact with AWS (e.g. Xacnt_APP_Fidelius) 	| string 	|


### OAuth 2.0 Config

Note: providing the following configuration will enable OAuth 2.0 for the API call made by the secret rotation endpoint.

 | Property                    	        | Description                                                                               	| Type   	|
 |------------------------------------- |-------------------------------------------------------------------------------------------	|--------	|
 | fidelius.auth.oauth.clientId         | The Client ID used when fetching the OAuth 2.0 token.              	                        | string 	|
 | fidelius.auth.oauth.clientSecret     | The Client Secret used when fetching the OAuth 2.0 token.             	                    | string 	|
 | fidelius.auth.oauth.tokenUrl         | The Endpoint Fidelius calls to fetch the OAuth 2.0 token.            	                        | string 	|
 | fidelius.auth.oauth.tokenUri         | The Endpoint URI Fidelius uses to fetch the OAuth 2.0 token.                                	| string 	|
 | fidelius.rotate.oauth.clientId       | The Client ID used when fetching the OAuth 2.0 token for the secret rotation endpoint.        | string 	|
 | fidelius.rotate.oauth.clientSecret   | The Client Secret used when fetching the OAuth 2.0 token for the secret rotation endpoint.    | string 	|
 | fidelius.rotate.oauth.tokenUrl       | The Endpoint Fidelius calls to fetch the OAuth 2.0 token for the secret rotation endpoint.    | string 	|
 | fidelius.rotate.oauth.tokenUri       | The Endpoint URI Fidelius uses to fetch the OAuth 2.0 token for the secret rotation endpoint. | string 	|
