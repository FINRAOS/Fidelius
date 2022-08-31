/*
 * Copyright (c) 2019. Fidelius Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.finra.fidelius;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

import java.util.HashMap;

import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FideliusClient.class, LoggerFactory.class})
@PowerMockIgnore( {"javax.management.*","javax.net.ssl.*"})


public class FideliusClientTests {

    @Test
    // Tests passed values when env variables are  provided.
    public void testPassedValuesOverridingEnv() throws Exception {

        JCredStash jCredStashMock = mock(JCredStash.class);

        EnvConfig envConfigMock = mock(EnvConfig.class,withSettings().defaultAnswer(CALLS_REAL_METHODS));

        FideliusClient fideliusClient = mock(FideliusClient.class,withSettings().defaultAnswer(CALLS_REAL_METHODS));

        fideliusClient.jCredStash = jCredStashMock;
        fideliusClient.envConfig = envConfigMock;

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("CRED_Application","APP");
        map.put("CRED_SDLC","dev");
        map.put("CRED_Component","gatekeeper");

        doReturn(map).when(envConfigMock).getEnvVars();
        doReturn("TestUser").when(jCredStashMock).getUpdatedBy();


        verifyPassedValues(fideliusClient,jCredStashMock);


    }

    @Test
    // Tests passed values when env variables are  provided.
    public void testPassedValuesOverridingEnvAndEc2() throws Exception {

        JCredStash jCredStashMock = mock(JCredStash.class);

        FideliusClient fideliusClient = mock(FideliusClient.class,withSettings().defaultAnswer(CALLS_REAL_METHODS));

        EnvConfig envConfigMock = mock(EnvConfig.class,withSettings().defaultAnswer(CALLS_REAL_METHODS));

        fideliusClient.jCredStash = jCredStashMock;
        fideliusClient.envConfig = envConfigMock;

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("CRED_Application","APP");
        map.put("CRED_SDLC","dev");
        map.put("CRED_Component","gatekeeper");

        HashMap<String, String> tags = new HashMap<String, String>();
        tags.put("Application","APP");
        tags.put("SDLC","dev");
        tags.put("Component","gatekeeper");


        doReturn(map).when(envConfigMock).getEnvVars();
        doReturn(tags).when(fideliusClient).getEC2Tags();


        verifyPassedValues(fideliusClient,jCredStashMock);


    }



    @Test
    // Tests passed values when env variables are  provided.
    public void testPassedValues() throws Exception {

        JCredStash jCredStashMock = mock(JCredStash.class);

        FideliusClient fideliusClient = mock(FideliusClient.class,withSettings().defaultAnswer(CALLS_REAL_METHODS));

        fideliusClient.jCredStash = jCredStashMock;

        EnvConfig envConfigMock = mock(EnvConfig.class,withSettings().defaultAnswer(CALLS_REAL_METHODS));
        fideliusClient.envConfig = envConfigMock;

        verifyPassedValues(fideliusClient,jCredStashMock);


    }


    private void verifyPassedValues(FideliusClient fideliusClient, JCredStash jCredStashMock) throws Exception {

        HashMap<String, String> expectedContext = new HashMap<String, String>();
        expectedContext.put("Application","SOMEAGS");
        expectedContext.put("SDLC","somesdlc");

        HashMap<String, String> expectedContextComp = new HashMap<String, String>();
        expectedContextComp.put("Application","SOMEAGS");
        expectedContextComp.put("SDLC","somesdlc");
        expectedContextComp.put("Component","somecomp");


        //when FID_CONTEXT_APPLICATION/FID_CONTEXT_SDLC are passed
        fideliusClient.putCredential("somecred","somepwd","someags","somesdlc",null,"sometable", "TestUser", "somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","SOMEAGS.somesdlc.somecred","somepwd","0000000000000000001", "TestUser","somekey",expectedContext);
        verify(fideliusClient,times(0)).getEC2Tags();
        verify(fideliusClient.envConfig,times(0)).getApplication();
        verify(fideliusClient.envConfig,times(0)).getSdlc();
        verify(fideliusClient.envConfig,times(0)).getComponent();



        //when FID_CONTEXT_APPLICATION/FID_CONTEXT_SDLC/Component are passed
        reset(jCredStashMock);
        fideliusClient.putCredential("somecred","somepwd","someags","somesdlc","somecomp","sometable", "TestUser","somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","SOMEAGS.somecomp.somesdlc.somecred","somepwd","0000000000000000001","TestUser","somekey",expectedContextComp);
        verify(fideliusClient,times(0)).getEC2Tags();
        verify(fideliusClient.envConfig,times(0)).getApplication();
        verify(fideliusClient.envConfig,times(0)).getSdlc();
        verify(fideliusClient.envConfig,times(0)).getComponent();


        //when FID_CONTEXT_APPLICATION only is passed
        reset(jCredStashMock);
        fideliusClient.putCredential("somecred","somepwd","someags","somesdlc","somecomp","sometable", "TestUser", "somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","SOMEAGS.somecomp.somesdlc.somecred","somepwd","0000000000000000001", "TestUser","somekey",expectedContextComp);
        verify(fideliusClient,times(0)).getEC2Tags();
        verify(fideliusClient.envConfig,times(0)).getApplication();
        verify(fideliusClient.envConfig,times(0)).getSdlc();
        verify(fideliusClient.envConfig,times(0)).getComponent();
    }

    @Test
    // Verifies if env variables take the precendence when no FID_CONTEXT_APPLICATION/FID_CONTEXT_SDLC values are passed.
    public void testEnvVariables() throws Exception {
        JCredStash jCredStashMock = mock(JCredStash.class);

        FideliusClient fideliusClient = mock(FideliusClient.class,withSettings().defaultAnswer(CALLS_REAL_METHODS));

        doThrow(RuntimeException.class).when(jCredStashMock).deleteSecret(anyString(), anyString());
        doReturn("testUser").when(fideliusClient).getUser();

        HashMap<String, String> envMap = new HashMap<String, String>();
        envMap.put("CRED_Application","APP");
        envMap.put("CRED_SDLC","dev");
        envMap.put("CRED_Component","gatekeeper");



        EnvConfig envConfigMock = spy(new EnvConfig());
        when(envConfigMock.getEnvVars()).thenReturn(envMap);

        PowerMockito.whenNew(EnvConfig.class).withNoArguments().thenReturn(envConfigMock);

        fideliusClient.jCredStash = jCredStashMock;

        fideliusClient.envConfig = new EnvConfig();


        HashMap<String, String> expectedContext = new HashMap<String, String>();
        expectedContext.put("Application","APP");
        expectedContext.put("SDLC","dev");
        expectedContext.put("Component","gatekeeper");



        // when
        resetTestEnvMocks(fideliusClient,jCredStashMock,envMap);
        fideliusClient.putCredential("somecred","somepwd");


        //then
        verify(jCredStashMock,times(1)).putSecret("credential-store","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001", "testUser", null,expectedContext);
        verify(fideliusClient,times(0)).getEC2Tags();

        //when
        resetTestEnvMocks(fideliusClient,jCredStashMock,envMap);
        fideliusClient.putCredential("somecred","somepwd","sometable","somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001","testUser", "somekey",expectedContext);
        verify(fideliusClient,times(0)).getEC2Tags();



        //when FID_CONTEXT_APPLICATION/FID_CONTEXT_SDLC/Component are all null
        resetTestEnvMocks(fideliusClient,jCredStashMock,envMap);
        fideliusClient.putCredential("somecred","somepwd",null,null,null,"sometable","somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001","testUser","somekey",expectedContext);
        verify(fideliusClient,times(0)).getEC2Tags();


        //when FID_CONTEXT_SDLC/Component are all null
        resetTestEnvMocks(fideliusClient,jCredStashMock,envMap);
        fideliusClient.putCredential("somecred","somepwd","someAGS",null,null,"sometable", "TestUser","somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001", "TestUser","somekey",expectedContext);
        verify(fideliusClient,times(0)).getEC2Tags();


        //when FID_CONTEXT_SDLC is not null
        resetTestEnvMocks(fideliusClient,jCredStashMock,envMap);
        fideliusClient.putCredential("somecred","somepwd", null,"somesdlc",null,"sometable", "TestUser", "somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001","TestUser","somekey",expectedContext);
        verify(fideliusClient,times(0)).getEC2Tags();


        //when component is not null
        resetTestEnvMocks(fideliusClient,jCredStashMock,envMap);
        fideliusClient.putCredential("somecred","somepwd", null,null,"somecomp","sometable","TestUser","somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001","TestUser","somekey",expectedContext);
        verify(fideliusClient,times(0)).getEC2Tags();


    }

    private void resetTestEnvMocks(FideliusClient fideliusClient, JCredStash jCredStashMock, HashMap<String,String> envMap) throws Exception{
        reset(jCredStashMock);
        reset(fideliusClient);
        doReturn(envMap).when(fideliusClient.envConfig).getEnvVars();
        doReturn("testUser").when(fideliusClient).getUser();
    }

    @Test
    // Tests fetching FID_CONTEXT_APPLICATION,FID_CONTEXT_SDLC from EC2 tags when no env variables or api parameters passed
    public void testEc2Tags() throws Exception {

        JCredStash jCredStashMock = mock(JCredStash.class);

        FideliusClient fideliusClient = mock(FideliusClient.class,withSettings().defaultAnswer(CALLS_REAL_METHODS));

        fideliusClient.jCredStash = jCredStashMock;
        EnvConfig envConfigMock = mock(EnvConfig.class,withSettings().defaultAnswer(CALLS_REAL_METHODS));
        fideliusClient.envConfig = envConfigMock;
        doReturn("testUser").when(fideliusClient).getUser();

        HashMap<String, String> tags = new HashMap<String, String>();
        tags.put("Application","APP");
        tags.put("SDLC","dev");
        tags.put("Component","gatekeeper");


        HashMap<String, String> expectedContext = new HashMap<String, String>();
        expectedContext.put("Application","APP");
        expectedContext.put("SDLC","dev");
        expectedContext.put("Component","gatekeeper");


        // when
        resetEc2Mocks(fideliusClient,jCredStashMock,envConfigMock,tags);
        fideliusClient.putCredential("somecred","somepwd");


        //then
        verify(jCredStashMock,times(1)).putSecret("credential-store","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001", "testUser",null,expectedContext);
        verify(fideliusClient,times(1)).getEC2Tags();

        //when
        resetEc2Mocks(fideliusClient,jCredStashMock,envConfigMock,tags);
        fideliusClient.putCredential("somecred","somepwd","sometable","somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001","testUser","somekey",expectedContext);
        verify(fideliusClient,times(1)).getEC2Tags();



        //when FID_CONTEXT_APPLICATION/FID_CONTEXT_SDLC/Component are all null
        resetEc2Mocks(fideliusClient,jCredStashMock,envConfigMock,tags);
        fideliusClient.putCredential("somecred","somepwd",null,null,null,"sometable","somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001","testUser","somekey",expectedContext);
        verify(fideliusClient,times(1)).getEC2Tags();


        //when FID_CONTEXT_SDLC/Component are all null
        resetEc2Mocks(fideliusClient,jCredStashMock,envConfigMock,tags);
        fideliusClient.putCredential("somecred","somepwd","someAGS",null,null,"sometable","TestUser","somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001","TestUser","somekey",expectedContext);
        verify(fideliusClient,times(1)).getEC2Tags();


        //when FID_CONTEXT_SDLC is not null
        resetEc2Mocks(fideliusClient,jCredStashMock,envConfigMock,tags);
        fideliusClient.putCredential("somecred","somepwd", null,"somesdlc",null,"sometable","TestUser","somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001","TestUser","somekey",expectedContext);
        verify(fideliusClient,times(1)).getEC2Tags();


        //when component is not null
        resetEc2Mocks(fideliusClient,jCredStashMock,envConfigMock,tags);
        fideliusClient.putCredential("somecred","somepwd", null,null,"somecomp","sometable","TestUser","somekey");

        //then
        verify(jCredStashMock,times(1)).putSecret("sometable","APP.gatekeeper.dev.somecred","somepwd","0000000000000000001","TestUser","somekey",expectedContext);
        verify(fideliusClient,times(1)).getEC2Tags();

    }

    private void resetEc2Mocks(FideliusClient fideliusClient, JCredStash jCredStashMock, EnvConfig envConfigMock, HashMap<String,String> tags) throws Exception{
        reset(jCredStashMock);
        reset(fideliusClient);
        reset(envConfigMock);
        doReturn(tags).when(fideliusClient).getEC2Tags();
        doReturn("testUser").when(fideliusClient).getUser();

    }



    @Test
    public void testNoTagsNoEnvNoParms() throws Exception {

        JCredStash jCredStashMock = mock(JCredStash.class);

        FideliusClient fideliusClient = mock(FideliusClient.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));

        fideliusClient.jCredStash = jCredStashMock;

        EnvConfig envConfigMock = mock(EnvConfig.class,withSettings().defaultAnswer(CALLS_REAL_METHODS));
        fideliusClient.envConfig = envConfigMock;

        HashMap<String, String> tags = new HashMap<String, String>();


        resetEc2Mocks(fideliusClient, jCredStashMock, envConfigMock,tags);
        doThrow(new Exception("Application or SDLC not specified and cannot be retrieved from tags or environment.")).when(fideliusClient).putCredential("somecred","somepwd");


        resetEc2Mocks(fideliusClient, jCredStashMock, envConfigMock,tags);
        doThrow(new Exception("Application or SDLC not specified and cannot be retrieved from tags or environment.")).when(fideliusClient).putCredential("somecred","somepwd",null,null,null,null,null);


        resetEc2Mocks(fideliusClient, jCredStashMock, envConfigMock,tags);
        doThrow(new Exception("Application or SDLC not specified and cannot be retrieved from tags or environment.")).when(fideliusClient).putCredential("somecred","somepwd",null,null,null,"sometable","somekey");


    }

    @Test(expected = RuntimeException.class)
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void deleteCredentialThrowsExceptionIfCredentialNotFound() throws Exception {

        Logger loggerMock = mock(Logger.class);

        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(any(Class.class))).
                thenReturn(loggerMock);

        JCredStash jCredStashMock = spy(JCredStash.class);

        FideliusClient fideliusClient = spy(FideliusClient.class);

        doThrow(RuntimeException.class).when(jCredStashMock).deleteSecret(anyString(), anyString());
        doReturn("testUser").when(fideliusClient).getUser();

        fideliusClient.jCredStash = jCredStashMock;

        fideliusClient.deleteCredential("secret", "app", "dev", "component", "table", null);

        verify(jCredStashMock, times(1)).deleteSecret("table", "APP.component.dev.secret");
        verify(loggerMock).info("Credential APP.component.dev.secret not found. [java.lang.RuntimeException] ");

    }

    @Test()
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void deleteCredentialSuccessfullyDeletes() throws Exception {

        Logger loggerMock = mock(Logger.class);
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(any(Class.class))).
                thenReturn(loggerMock);

        JCredStash jCredStashMock = spy(JCredStash.class);

        FideliusClient fideliusClient = spy(FideliusClient.class);

        doNothing().when(jCredStashMock).deleteSecret(anyString(), anyString());
        doReturn("testUser").when(fideliusClient).getUser();

        fideliusClient.jCredStash = jCredStashMock;

        fideliusClient.deleteCredential("secret", "application", "dev", "component","table", null);

        verify(jCredStashMock, times(1)).deleteSecret("table", "APPLICATION.component.dev.secret");
        verify(loggerMock, times(2)).info(anyString());
        verify(loggerMock).info("User testUser deleted credential APPLICATION.component.dev.secret");
    }

    @Test()
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void usernameGetsLoggedOnDeleteCredential() throws Exception {
        Logger loggerMock = mock(Logger.class);
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(any(Class.class))).
                thenReturn(loggerMock);

        JCredStash jCredStashMock = spy(JCredStash.class);
        FideliusClient fideliusClient = spy(FideliusClient.class);

        doNothing().when(jCredStashMock).deleteSecret(anyString(), anyString());

        fideliusClient.jCredStash = jCredStashMock;

        fideliusClient.deleteCredential("secret", "app", "dev", "component", "table", "NewUser");

        verify(jCredStashMock, times(1)).deleteSecret("table", "APP.component.dev.secret");
        verify(loggerMock, times(2)).info(anyString());
        verify(loggerMock).info("User NewUser deleted credential APP.component.dev.secret");
    }

    @Test()
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void usernameGetsLoggedOnPutCredential() throws Exception {
        Logger loggerMock = mock(Logger.class);
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(any(Class.class))).
                thenReturn(loggerMock);

        JCredStash jCredStashMock = spy(JCredStash.class);
        FideliusClient jCredStashFx = spy(FideliusClient.class);
        HashMap<String, String> context = new HashMap<String, String>();
        context.put("Application", "APP");
        context.put("SDLC", "dev");
        context.put("Component", "component");

        doNothing().when(jCredStashMock).putSecret(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyMapOf(String.class, String.class));
        doReturn(0).when(jCredStashMock).getHighestVersion(anyString(), anyString());

        jCredStashFx.jCredStash = jCredStashMock;

        jCredStashFx.putCredential("secret", "password", "app", "dev", "component", "table", "NewUser", null);

        verify(jCredStashMock, times(1)).putSecret("table", "APP.component.dev.secret", "password", "0000000000000000001", "NewUser", null, context);
        verify(loggerMock, times(2)).info(anyString());
        verify(loggerMock).info("Version 0000000000000000001 of APP.component.dev.secret stored in table by User NewUser");

    }

    @Test()
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void usernameGetsLoggedOnGetCredentialWithUserNamePassed() throws Exception {
        Logger loggerMock = mock(Logger.class);
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(any(Class.class))).
                thenReturn(loggerMock);

        JCredStash jCredStashMock = spy(JCredStash.class);
        FideliusClient fideliusClient = spy(FideliusClient.class);
        HashMap<String, String> context = new HashMap<>();
        context.put("Application", "APP");
        context.put("SDLC", "dev");
        context.put("Component", "component");

        doReturn("decryptedPassword").when(jCredStashMock).getSecret(anyString(), anyString(), anyMapOf(String.class, String.class));

        fideliusClient.jCredStash = jCredStashMock;

        String result = fideliusClient.getCredential("secret", "app", "dev", "component", "table", "NewTestUser", true);
        Assert.assertEquals("decryptedPassword", result);

        verify(jCredStashMock, times(1)).getSecret("table", "APP.component.dev.secret", context);
        verify(loggerMock, times(2)).info(anyString());
        verify(loggerMock).info("User NewTestUser retrieved contents of APP.component.dev.secret");
    }

    @Test()
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void usernameGetsLoggedOnGetCredentialWithoutUserNamePassed() throws Exception {
        Logger loggerMock = mock(Logger.class);
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(any(Class.class))).
                thenReturn(loggerMock);

        JCredStash jCredStashMock = spy(JCredStash.class);
        FideliusClient fideliusClient = spy(FideliusClient.class);
        HashMap<String, String> context = new HashMap<>();
        context.put("Application", "APP");
        context.put("SDLC", "dev");
        context.put("Component", "component");

        doReturn("decryptedPassword").when(jCredStashMock).getSecret(anyString(), anyString(), anyMapOf(String.class, String.class));
        doReturn("TestUser").when(fideliusClient).getUser();

        fideliusClient.jCredStash = jCredStashMock;

        String result = fideliusClient.getCredential("secret", "app", "dev", "component", "table");
        Assert.assertEquals("decryptedPassword", result);

        verify(jCredStashMock, times(1)).getSecret("table", "APP.component.dev.secret", context);
        verify(loggerMock, times(2)).info(anyString());
        verify(loggerMock).info("User TestUser retrieved contents of APP.component.dev.secret");
    }


    @Test(expected = RuntimeException.class)
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void errorWhenFailToGetUserOnGetCredential() throws Exception {
        JCredStash jCredStashMock = spy(JCredStash.class);
        FideliusClient fideliusClient = spy(FideliusClient.class);
        StsClient awsSecurityTokenService = spy(StsClient.class);

        doThrow(new RuntimeException("AWS Cannot get Identity Error")).when(awsSecurityTokenService).getCallerIdentity(any(GetCallerIdentityRequest.class));

        fideliusClient.jCredStash = jCredStashMock;
        fideliusClient.stsClient = awsSecurityTokenService;

        String result = fideliusClient.getCredential("secret", "app", "dev", "component", "table");

        Assert.assertNull(result);
    }


    @Test(expected = RuntimeException.class)
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void errorWhenFailToGetUserOnPutCredential() throws Exception {
        JCredStash jCredStashMock = spy(JCredStash.class);
        FideliusClient fideliusClient = spy(FideliusClient.class);
        StsClient awsSecurityTokenService = spy(StsClient.class);
        HashMap<String, String> context = new HashMap<String, String>();
        context.put("Application", "APP");
        context.put("SDLC", "dev");
        context.put("Component", "component");

        doNothing().when(jCredStashMock).putSecret(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyMapOf(String.class, String.class));
        doReturn(0).when(jCredStashMock).getHighestVersion(anyString(), anyString());

        doThrow(new RuntimeException("AWS Cannot get Identity Error")).when(awsSecurityTokenService).getCallerIdentity(any(GetCallerIdentityRequest.class));

        fideliusClient.jCredStash = jCredStashMock;
        fideliusClient.stsClient = awsSecurityTokenService;

        fideliusClient.putCredential("somecred","somepwd","someapp","somesdlc",null,"sometable", null, "somekey");
    }


    @Test(expected = RuntimeException.class)
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void errorWhenFailToGetUserOnDeleteCredential() throws Exception {
        JCredStash jCredStashMock = spy(JCredStash.class);
        FideliusClient fideliusClient = spy(FideliusClient.class);
        StsClient awsSecurityTokenService = spy(StsClient.class);

        doThrow(new RuntimeException("AWS Cannot get Identity Error")).when(awsSecurityTokenService).getCallerIdentity(any(GetCallerIdentityRequest.class));
        doNothing().when(jCredStashMock).deleteSecret(anyString(), anyString());

        fideliusClient.jCredStash = jCredStashMock;
        fideliusClient.stsClient = awsSecurityTokenService;

        fideliusClient.deleteCredential("secret", "app", "dev", "component", "table", null);
    }

    @Test()
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void getCredentialReturnsAPPCredentialName() throws Exception {
        Logger loggerMock = mock(Logger.class);
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(any(Class.class))).
                thenReturn(loggerMock);

        JCredStash jCredStashMock = spy(JCredStash.class);
        FideliusClient fideliusClient = spy(FideliusClient.class);
        HashMap<String, String> context = new HashMap<>();
        context.put("Application", "APP");
        context.put("SDLC", "dev");
        context.put("Component", "component");

        HashMap<String, String> APPContext = context;
        APPContext.remove("Component");

        doReturn("decryptedPassword").when(jCredStashMock).getSecret("table", "APP.dev.secret", APPContext);
        doThrow(new RuntimeException()).when(jCredStashMock).getSecret("table", "APP.component.dev.secret", context);

        fideliusClient.jCredStash = jCredStashMock;

        String result = fideliusClient.getCredential("secret", "app", "dev", "component", "table", "TestUser", true);
        Assert.assertEquals("decryptedPassword", result);
    }

    @Test()
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void getCredentialOnlyReturnsExactCredentialName() throws Exception {
        Logger loggerMock = mock(Logger.class);
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(any(Class.class))).
                thenReturn(loggerMock);

        JCredStash jCredStashMock = spy(JCredStash.class);
        FideliusClient fideliusClient = spy(FideliusClient.class);
        HashMap<String, String> context = new HashMap<>();
        context.put("Application", "APP");
        context.put("SDLC", "dev");
        context.put("Component", "component");

        doReturn("decryptedPassword").when(jCredStashMock).getSecret("table", "APP.dev.secret", context);
        doThrow(new RuntimeException()).when(jCredStashMock).getSecret("table", "APP.component.dev.secret", context);

        fideliusClient.jCredStash = jCredStashMock;

        String result = fideliusClient.getCredential("secret", "app", "dev", "component", "table", "TestUser", false);
        Assert.assertEquals(null, result);

        verify(jCredStashMock, times(1)).getSecret("table", "APP.component.dev.secret", context);
        verify(loggerMock, times(2)).info(anyString());
        verify(loggerMock).info("Credential APP.component.dev.secret not found. [java.lang.RuntimeException] ");
    }

    @Test()
    @PrepareForTest({LoggerFactory.class, FideliusClient.class})
    public void getUserCorrectlyParsesUserStringsInARNForm() throws Exception {
        Logger loggerMock = mock(Logger.class);
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(any(Class.class))).
                thenReturn(loggerMock);
        
        String userARN = "arn:aws:sts::123456789:assumed-role/aws_dev_d/test_username";
        String userARN_2 = "arn:aws:sts::123456789:assumed-role/assumed-role/test_username";
        String userARN_3 = "arn:aws:sts::123456789:assumed-role/test_username";
        String userARN_4 = "arn:aws:sts::123456789:ASSUMED-ROLE/test_username";
        FideliusClient fideliusClient = spy(FideliusClient.class);

        doReturn(userARN).when(fideliusClient).getUserIdentity();
        Assert.assertTrue(fideliusClient.getUser().equals("aws_dev_d/test_username"));

        doReturn(userARN_2).when(fideliusClient).getUserIdentity();
        Assert.assertTrue(fideliusClient.getUser().equals("assumed-role/test_username"));

        doReturn(userARN_3).when(fideliusClient).getUserIdentity();
        Assert.assertTrue(fideliusClient.getUser().equals("test_username"));

        doReturn(userARN_4).when(fideliusClient).getUserIdentity();
        Assert.assertTrue(fideliusClient.getUser().equals("arn:aws:sts::123456789:ASSUMED-ROLE/test_username"));

    }
}


