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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EnvConfig.class})
@PowerMockIgnore( {"javax.management.*","javax.net.ssl.*"})
public class EnvConfigTests {

    @Test
    public void testProxyAll(){

        HashMap<String, String> envMap = new HashMap<String, String>();
        envMap.put("CRED_PROXY","someproxy");
        envMap.put("CRED_PORT","1000");

        EnvConfig envConfigMock = initEnvSpy(envMap);

        Assert.assertEquals(envConfigMock.getProxy(),"someproxy");
        Assert.assertEquals(envConfigMock.getPort(),"1000");
        Assert.assertTrue(envConfigMock.hasProxyEnv());


    }

    @Test
    public void testProxyHostOnly(){

        HashMap<String, String> envMap = new HashMap<String, String>();
        envMap.put("CRED_PROXY","someproxy");

        EnvConfig envConfigMock = initEnvSpy(envMap);

        Assert.assertEquals(envConfigMock.getProxy(),"someproxy");
        Assert.assertEquals(envConfigMock.getPort(),null);
        Assert.assertFalse(envConfigMock.hasProxyEnv());


    }

    @Test
    public void testProxyPortOnly(){

        HashMap<String, String> envMap = new HashMap<String, String>();
        envMap.put("CRED_PORT","1000");

        EnvConfig envConfigMock = initEnvSpy(envMap);

        Assert.assertEquals(envConfigMock.getProxy(),null);
        Assert.assertEquals(envConfigMock.getPort(),"1000");
        Assert.assertFalse(envConfigMock.hasProxyEnv());


    }

    @Test
    public void testAgsSdlcComponentOnly(){

        TreeMap<String, String> envMap =  new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        envMap.put("CRED_APPLICATION","app");
        envMap.put("CRED_SDLC","dev");
        envMap.put("CRED_COMPONENT","gatekeeper");


        EnvConfig envConfigMock = initEnvSpy(envMap);

        Assert.assertEquals(null,envConfigMock.getProxy());
        Assert.assertEquals(null,envConfigMock.getPort());
        Assert.assertFalse(envConfigMock.hasProxyEnv());
        Assert.assertEquals("app",envConfigMock.getApplication());
        Assert.assertEquals("dev",envConfigMock.getSdlc());
        Assert.assertEquals("gatekeeper",envConfigMock.getComponent());
        Assert.assertTrue(envConfigMock.hasAgsSdlcEnv());
        Assert.assertTrue(envConfigMock.hasComponentEnv());

    }



    @Test
    public void testAgsOnly(){

        TreeMap<String, String> envMap =  new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        envMap.put("CRED_APPLICATION","app");


        EnvConfig envConfigMock = initEnvSpy(envMap);

        Assert.assertEquals(null,envConfigMock.getProxy());
        Assert.assertEquals(null,envConfigMock.getPort());
        Assert.assertFalse(envConfigMock.hasProxyEnv());
        Assert.assertEquals("app",envConfigMock.getApplication());
        Assert.assertEquals(null,envConfigMock.getSdlc());
        Assert.assertEquals(null,envConfigMock.getComponent());
        Assert.assertFalse(envConfigMock.hasAgsSdlcEnv());
        Assert.assertFalse(envConfigMock.hasComponentEnv());

    }


    @Test
    public void testSdlcOnly(){

        TreeMap<String, String> envMap =  new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        envMap.put("CRED_sdlc","dev");


        EnvConfig envConfigMock = initEnvSpy(envMap);

        Assert.assertEquals(null,envConfigMock.getProxy());
        Assert.assertEquals(null,envConfigMock.getPort());
        Assert.assertFalse(envConfigMock.hasProxyEnv());
        Assert.assertEquals(null,envConfigMock.getApplication());
        Assert.assertEquals("dev",envConfigMock.getSdlc());
        Assert.assertEquals(null,envConfigMock.getComponent());
        Assert.assertFalse(envConfigMock.hasAgsSdlcEnv());
        Assert.assertFalse(envConfigMock.hasComponentEnv());

    }

    @Test
    public void testComponentOnly(){

        TreeMap<String, String> envMap =  new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        envMap.put("CRED_component","something");


        EnvConfig envConfigMock = initEnvSpy(envMap);

        Assert.assertEquals(null,envConfigMock.getProxy());
        Assert.assertEquals(null,envConfigMock.getPort());
        Assert.assertFalse(envConfigMock.hasProxyEnv());
        Assert.assertEquals(null,envConfigMock.getApplication());
        Assert.assertEquals(null,envConfigMock.getSdlc());
        Assert.assertEquals("something",envConfigMock.getComponent());
        Assert.assertFalse(envConfigMock.hasAgsSdlcEnv());
        Assert.assertTrue(envConfigMock.hasComponentEnv());

    }

    private EnvConfig initEnvSpy(Map<String,String> env){
        EnvConfig envConfigMock = spy(new EnvConfig());
        when(envConfigMock.getEnvVars()).thenReturn(env);
        return envConfigMock;
    }
}

