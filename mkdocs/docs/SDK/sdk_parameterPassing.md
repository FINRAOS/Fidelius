# Additional Details

## Passing Application,SDLC and Component to get and put Credentials

Application, SDLC and Component values are used to:

* Provide the encryption context of the encryptedCredential
* Form the encryptedCredential name as:
    * `<Application>.<Component>.<SDLC>.<encryptedCredential>` if the component is provided
    * `<Application>.<SDLC>.<encryptedCredential>`  otherwise

Application and SDLC are mandatory to be provided. Component is optional.

There are three possible ways to pass these values:

1. Directly to the **getCredential** and **putCredential** APIs as parameters.
2. Setting environment variables CRED_Application, CRED_SDLC and CRED_Component
3. Setting EC2 instance tags namely Application, SDLC and Component.

Environment variables take higher priority over EC2 instances tags.