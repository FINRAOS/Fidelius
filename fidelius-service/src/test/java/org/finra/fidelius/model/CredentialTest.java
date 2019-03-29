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

import static org.junit.Assert.*;

public class CredentialTest {

    private LocalValidatorFactoryBean localValidatorFactory;
    private  Credential credential;

    @Before
    public void setup() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();

        credential = new Credential();
        credential.setApplication("app");
        credential.setRegion("us-east-1");
        credential.setAccount("dev");
        credential.setShortKey("secret");
        credential.setEnvironment("dev");
    }

    @Test
    public void CreateCredential(){
        Credential credential = new Credential("testKey", "APP.TestComponent.dev.testKey", "account", "us-east-1","APP", "dev",
                "TestComponent", "Jon Snow", "2018-04-04T12:51:37.803Z");

        assertEquals("testKey",credential.getShortKey());
        assertEquals("APP.TestComponent.dev.testKey", credential.getLongKey());
        assertEquals( "account",credential.getAccount());
        assertEquals("us-east-1", credential.getRegion());

    }

    @Test
    public void TestActiveDirectoryDefaultRegEx() {
        credential.setSecret("test password");
        credential.setIsActiveDirectory(true);

        Set<ConstraintViolation<Credential>> constraintViolations = localValidatorFactory.validate(credential);

        assertEquals( 1, constraintViolations.size() );
        assertEquals(
                "Invalid Active Directory password",
                constraintViolations.iterator().next().getMessage()
        );
    }

    @Test
    public void ActiveDirectoryDefaultRegExValidPassword() {
        credential.setSecret("testpassword");
        credential.setIsActiveDirectory(true);
        Set<ConstraintViolation<Credential>> constraintViolations = localValidatorFactory.validate(credential);

        assertEquals( 0, constraintViolations.size() );
    }

    @Test
    public void ActiveDirectoryRegExDoesntGetCalledWhenBooleanFalse() {
        credential.setSecret("test password");
        credential.setIsActiveDirectory(false);
        Set<ConstraintViolation<Credential>> constraintViolations = localValidatorFactory.validate(credential);

        assertEquals( 0, constraintViolations.size() );
    }

    @Test
    public void Test6RequiredFields() {
        credential = new Credential();
        credential.setIsActiveDirectory(false);
        Set<ConstraintViolation<Credential>> constraintViolations = localValidatorFactory.validate(credential);

        assertEquals( 12, constraintViolations.size() );
    }

    @Test
    public void Test6RequiredFieldsValid() {
        credential.setIsActiveDirectory(false);
        credential.setSecret("test");
        Set<ConstraintViolation<Credential>> constraintViolations = localValidatorFactory.validate(credential);

        assertEquals( 0, constraintViolations.size() );
    }
}