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

package org.finra.fidelius.model;

import org.hibernate.validator.HibernateValidator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class MetadataTest {

    private LocalValidatorFactoryBean localValidatorFactory;
    private Metadata metadata;

    @Before
    public void setup() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();

        metadata = new Metadata();
        metadata.setApplication("app");
        metadata.setRegion("us-east-1");
        metadata.setAccount("dev");
        metadata.setShortKey("secret");
        metadata.setEnvironment("dev");
    }

    @Test
    public void CreateMetadata(){
        Metadata metadata = new Metadata("testKey", "META#APP.TestComponent.dev.testKey", "account", "us-east-1","APP", "dev",
                "testSourceType", "testSource", "TestComponent", "Jon Snow", "2018-04-04T12:51:37.803Z");

        assertEquals("testKey",metadata.getShortKey());
        assertEquals("META#APP.TestComponent.dev.testKey", metadata.getLongKey());
        assertEquals( "account",metadata.getAccount());
        assertEquals("us-east-1", metadata.getRegion());
        assertEquals("testSourceType", metadata.getSourceType());
        assertEquals("testSource", metadata.getSource());

    }


    @Test
    public void Test6RequiredFieldsValid() {

        metadata.setSourceType("testSourceType");
        metadata.setSource("testSource");
        Set<ConstraintViolation<Metadata>> constraintViolations = localValidatorFactory.validate(metadata);

        assertEquals( 0, constraintViolations.size() );
    }
}