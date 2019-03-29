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

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.TreeMap;

public class EnvConfig {

    public static String[] envKeys = {
            Constants.CREDSTASH_ENV_PREFIX + Constants.FID_CONTEXT_APPLICATION,
            Constants.CREDSTASH_ENV_PREFIX + Constants.FID_CONTEXT_SDLC,
            Constants.CREDSTASH_ENV_PREFIX + Constants.FID_CONTEXT_COMPONENT,
            Constants.CREDSTASH_ENV_PREFIX + Constants.PROXY,
            Constants.CREDSTASH_ENV_PREFIX + Constants.PORT
    };

    private Map<String,String> env;

    private void init(){
        if(env == null){
            env = getEnvVars();
        }
    }

    protected  Map<String, String> getEnvVars() {

        Map<String, String> envVars =
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);


        Map<String, String> sysEnv = System.getenv();


        for(String key: envKeys){
            if(sysEnv.containsKey(key)){
                envVars.put(key,sysEnv.get(key));
            }
        }

        return envVars;
    }

    public String getApplication(){
        return getProp(Constants.CREDSTASH_ENV_PREFIX+Constants.FID_CONTEXT_APPLICATION);
    }

    public String getSdlc(){
        return getProp(Constants.CREDSTASH_ENV_PREFIX+Constants.FID_CONTEXT_SDLC);
    }

    public String getComponent(){
        return getProp(Constants.CREDSTASH_ENV_PREFIX+Constants.FID_CONTEXT_COMPONENT);
    }

    public String getProxy(){
        return getProp(Constants.CREDSTASH_ENV_PREFIX+Constants.PROXY);
    }

    public String getPort(){
        return getProp(Constants.CREDSTASH_ENV_PREFIX+Constants.PORT);
    }

    public boolean hasAgsSdlcEnv(){
        return StringUtils.isNoneBlank(getApplication(),getSdlc()) ;
    }

    public boolean hasComponentEnv(){
        return StringUtils.isNotBlank(getComponent());
    }

    public boolean hasProxyEnv(){
        return StringUtils.isNoneBlank(getProxy(),getPort());
    }


    private String getProp(String key){
        init();
        return StringUtils.trim(env.get(key));

    }
}
