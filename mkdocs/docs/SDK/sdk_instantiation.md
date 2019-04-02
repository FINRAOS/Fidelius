# Additional Details

## Instantiation


A Fidelius object can be instantiated with varying levels of configuration. The default constructor uses the
[DefaultAWSCredentialsProviderChain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html)
and default [ClientConfiguration](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/ClientConfiguration.html).

The region is set from the **AWS_DEFAULT_REGION** environment variable, or "**us-east-1**" if the variable is not set.

``` java
// Default Constructor
FideliusClient fideliusClient = new FideliusClient();
```

* Network-related settings (proxy, timeout, etc.) can be set by configuring and passing the [ClientConfiguration](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/ClientConfiguration.html)
object. If only proxy needs to be configured then the environment variables CRED_PROXY and CRED_PORT can be set and use the default ClientConfiguration.

* Authentication is configured by passing in an [AWSCredentialsProvider](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentialsProvider.html)

* AWS Region can be specified by passing the appropriate string value. See:
http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/regions/Regions.html

### <B>Example 1:</B> Passing Proxy and Port with custom ClientConfiguration
``` java
 ClientConfiguration clientConf = new ClientConfiguration()
     .withProxyHost("proxy.company.com")
     .withProxyPort(8081);
 AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();
 String region = Regions.US_EAST_1.getName();

 FideliusClient fideliusClient = new FideliusClient(clientConf, provider, region);

```

### <B>Example 2:</B> Setting proxy environment variables and using default ClientConfiguration
- Linux
```bash
export CRED_PROXY=proxy.company.com
export CRED_PORT=8081
```
- Windows
```bash
//windows
set CRED_PROXY=proxy.company.com
set CRED_PORT=8081


```
``` java
FideliusClient fideliusClient = new FideliusClient();

// or

AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();
FideliusClient fideliusClient = new FideliusClient(Regions.US_EAST_1.getName());


```
