# DynamoDb

## Overview
Fidelius uses DynamoDb to store and retrieve secrets.  In order for Fidelius to run you need to first create a dedicated
DynamoDb table to be used by Fidelius.  You can accomplish this in one of the following 2 ways.

### 1.) Quick start 
A DynamoDb table can be created using the [Quick start script](../QuickStart.md) provided.  

### 2.) AWS Console
You can manually create a DynamoDb table using the AWS console.  It must contain the following properties:

| Property                  | Value             |
|-----------------------	|------------------	|
| Table name            	| credential-store 	|
| Primary partition key 	| name (String)    	|
| Primary sort key      	| version (String) 	|
