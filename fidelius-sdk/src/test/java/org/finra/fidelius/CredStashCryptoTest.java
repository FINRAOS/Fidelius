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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public abstract class CredStashCryptoTest {
    @Parameterized.Parameters(name = "{index} {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        "Simple",
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        "AAAAAA==", "Uw+K+w==", "AA7855E13839DD767CD5DA7C1FF5036540C9264B7A803029315E55375287B4AF"
                }
        });
    }

    private String key;
    private String digestKey;
    private String encrypted;
    private String decrypted;
    private String digest;

    public CredStashCryptoTest(String name, String key, String digestKey, String decrypted, String encrypted, String digest) {
        this.key = key;
        this.digestKey = digestKey;
        this.decrypted = decrypted;
        this.encrypted = encrypted;
        this.digest = digest;
    }

    protected abstract CredStashCrypto getCryptoImplementation();


    @Test
    public void testDecrypt() throws Exception {
        byte[] keyBytes = javax.xml.bind.DatatypeConverter.parseHexBinary(key);
        byte[] decryptedBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(decrypted);
        byte[] encryptedbytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(encrypted);

        CredStashCrypto crypto = getCryptoImplementation();

        byte[] actualDecrypted = crypto.decrypt(keyBytes, encryptedbytes);

        assertThat("Decrypted: " + javax.xml.bind.DatatypeConverter.printBase64Binary(actualDecrypted), actualDecrypted, equalTo(decryptedBytes));
    }

    @Test
    public void testDigest() throws Exception {
        byte[] decryptedBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(decrypted);
        byte[] digestKeyBytes = javax.xml.bind.DatatypeConverter.parseHexBinary(digestKey);
        byte[] digestBytes = javax.xml.bind.DatatypeConverter.parseHexBinary(digest);

        CredStashCrypto crypto = getCryptoImplementation();

        byte[] actualDigest = crypto.digest(digestKeyBytes, decryptedBytes);

        assertThat("Digest: " + javax.xml.bind.DatatypeConverter.printHexBinary(actualDigest), actualDigest, equalTo(digestBytes));
    }
}