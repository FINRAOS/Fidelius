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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public final class Constants {
    private static final Logger logger = LoggerFactory.getLogger(Constants.class);

    public static final String FID_CONTEXT_APPLICATION = getField("FID_CONTEXT_APPLICATION") != null ? getField("FID_CONTEXT_APPLICATION") : "Application";
    public static final String FID_CONTEXT_SDLC = getField("FID_CONTEXT_SDLC") != null ? getField("FID_CONTEXT_SDLC") : "SDLC";
    public static final String FID_CONTEXT_COMPONENT = getField("FID_CONTEXT_COMPONENT") != null ? getField("FID_CONTEXT_COMPONENT") : "Component";
    public static final String DEFAULT_TABLE=  "credential-store";
    public static final String DEFAULT_KMS_KEY = "alias/credstash";
    public static final String DEFAULT_LAMBDA = "CREDSTSH-amg-password-rotation";
    public static final String CREDSTASH_ENV_PREFIX = "CRED_";
    public static final String PROXY = "PROXY";
    public static final String PORT= "PORT";

    private static boolean propertiesLoaded = false;
    private static boolean propertiesNotFound = false;
    private static Properties properties;

    public static String getField(String name) {
        if(!propertiesLoaded && !propertiesNotFound) {
            try {
                properties = getProperties();
            } catch (Exception e){
                logger.warn("Could not load environments from properties file");
                properties = null;
                propertiesNotFound = true;
            }
        }
        String value = System.getenv(name);
        if (value != null) {
            logger.info("Loading environment value for " + name + " = " + value);
            return System.getenv(name);
        } else if( propertiesLoaded && properties.getProperty(name) != null){
            return properties.getProperty(name);
        }
        logger.info("Loading default value for " + name);
        return null;
    }

    private static Properties getProperties() throws Exception{
        try {
            Properties pom = new Properties();
            pom.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/maven/org.finra.credstsh/fidelius-sdk/pom.properties"));
            logger.info("Using Fidelius-SDK " + pom.getProperty("version"));
        } catch (Exception e){
            logger.warn("Error loading pom.properties");
        }

        try {
            // Look for Fidelius.properties
            Properties properties = loadProperties("fidelius.properties");
            return properties;
        } catch (Exception e){
            // Look for application.properties
            Properties properties = loadProperties("application.properties");
            return properties;
        }
    }

    private static Properties loadProperties(String propertyName) throws Exception{
        InputStream fileInput = null;

        fileInput = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyName);
        Properties properties = new Properties();
        properties.load(fileInput);
        fileInput.close();
        logger.info("Loaded environments from " + propertyName + " " + Thread.currentThread().getContextClassLoader().getResource(propertyName));
        propertiesLoaded = true;
        return properties;
    }
}
