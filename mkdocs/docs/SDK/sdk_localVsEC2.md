# Additional Details

## Running from EC2 Instance vs. Local

When running on an EC2 instance the prefered approach is to configure the Application, SDLC and Component as the 
instance tags. However, if there is a usecase where one does NOT want to use the EC2 instance tags (example: jenkins 
build on a generic EC2 slave), then the environment variables CRED_Application, CRED_SDLC and CRED_COMPONENT can be 
configured which will take higher priority.

Example using environment variables:
```bash

CRED_Application=APP
CRED_Component=database
CRED_SDLC=dev

```

When running locally, the prefered approach is to  set the environment variables. However, if there is a usecase to call
the **getCredential** and **putCredential** multiple times with different Application/SDLC/Component values, then pass 
those values directly to the APIs.