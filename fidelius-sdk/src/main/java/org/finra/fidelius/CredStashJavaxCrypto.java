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

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


public class CredStashJavaxCrypto implements CredStashCrypto {
    protected final String CIPHER_TRANSFORMATION = "AES/CTR/NoPadding";
    protected final String MAC_SERVICE = "HmacSHA256";
    protected Cipher cipher;
    protected IvParameterSpec ivParameterSpec;
    protected Mac mac;

    public CredStashJavaxCrypto() {
        try {
            int maxAllowedKeyLength = Cipher.getMaxAllowedKeyLength("AES");
            if(maxAllowedKeyLength < 256) {
                throw new RuntimeException("Maximum key length " + maxAllowedKeyLength + " too low, likely Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files not installed");
            }
            cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            ivParameterSpec = new IvParameterSpec(INITIALIZATION_VECTOR);
            mac = Mac.getInstance(MAC_SERVICE);
        } catch(NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Error initializing javax.crypto", e);
        }
    }

    public byte[] decrypt(byte[] key, byte[] contents) {
        SecretKeySpec aes = new SecretKeySpec(key, "AES");

        try {

            // Credstash initial value for the counter starts at 1 (the default for pycrypto) rather than 0 or
            // a randomly chosen value.  New randomly chosen values should always be used when the key is being reused
            // to prevent attackers from finding identically encrypted blocks and deducing the blocks to be identical
            // when unencrypted. In this case it's safe to reuse initial values because a new key is chosen for
            // every encrypted secret.
            cipher.init(Cipher.DECRYPT_MODE, aes, ivParameterSpec);

            return cipher.doFinal(contents);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            throw new RuntimeException("Error executing javax.crypto", e);
        }
    }

    @Override
    public byte[] digest(byte[] keyBytes, byte[] contents) {
        SecretKeySpec hmac = new SecretKeySpec(keyBytes, "HmacSHA256");
        try {
            mac.init(hmac);
            return mac.doFinal(contents);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Error verifying javax.crypto", e);
        }
    }
}
