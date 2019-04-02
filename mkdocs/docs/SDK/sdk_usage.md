# Usage 

There are two main strategies to using the Fidelius SDK's API.

## 1. Passing Application and SDLC to the APIs

``` java
FideliusClient fideliusClient = new FideliusClient();
String application = "APP";
String sdlc = "dev";

fideliusClient.putCredential("name","secret", application, sdlc, null, null, null);
String secret = fideliusClient.getCredential("name", application, sdlc, null, null);

```

## 2. Configuring Application and SDLC as environment variables

``` bash
export CRED_Application="APP"
export CRED_SDLC="dev"
```

``` java
FideliusClient fideliusClient = new FideliusClient();

fideliusClient.putCredential("name","secret");
String secret = fideliusClient.getCredential("name");

```