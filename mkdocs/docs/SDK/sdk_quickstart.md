# Getting Started

## Overview
The Fidelius SDK is a Java API for securely accessing and storing secrets using AWS DynamoDB tables. The Fidelius SDK,
built off of the JCredstash project (https://github.com/jessecoyle/jcredstash), aims to provide an easy to use
API that allows users to create, retrieve, and delete encrypted secrets stored on DynamoDB for Java Clients.

### Maven Dependency
 
``` xml
<dependencies>
        <dependency>
            <groupId>org.finra.fidelius</groupId>
            <artifactId>fidelius-sdk</artifactId>
            <version>1.0.0</version>
        </dependency>
</dependencies>
```

### Proxy Configuration 
Make sure to set the proxy if you are running this in an environment that requires a proxy.

##### Local Desktop

``` bash
export CRED_PROXY=proxy.company.com
export CRED_PORT=8081
```


##### AWS 
``` bash
export CRED_PROXY=awsproxy.company.com
export CRED_PORT=8082
```