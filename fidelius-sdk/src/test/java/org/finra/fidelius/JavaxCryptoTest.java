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

import org.finra.fidelius.CredStashCrypto;
import org.finra.fidelius.CredStashJavaxCrypto;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;

public class JavaxCryptoTest extends CredStashCryptoTest {

    @ClassRule
    public static TestRule assumption = new TestRule() {
        @Override
        public Statement apply(Statement statement, Description description) {
            try {
                int maxAllowedKeyLength = Cipher.getMaxAllowedKeyLength("AES");
                Assume.assumeThat("Unlimited Strength policy files installed", maxAllowedKeyLength, Matchers.greaterThanOrEqualTo(256));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            return statement;
        }
    };

    public JavaxCryptoTest(String name, String key, String digestKey, String decrypted, String encrypted, String digest) {
        super(name, key, digestKey, decrypted, encrypted, digest);
    }

    @Override
    protected CredStashCrypto getCryptoImplementation() {
        return new CredStashJavaxCrypto();
    }
}
