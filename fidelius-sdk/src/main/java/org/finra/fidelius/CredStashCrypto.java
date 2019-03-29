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

public interface CredStashCrypto {

    /**
     * Credstash initial value for the counter starts at 1 (the default for pycrypto) rather than 0 or
     * a randomly chosen value.  New randomly chosen values should always be used when the key is being reused
     * to prevent attackers from finding identically encrypted blocks and deducing the blocks to be identical
     * when unencrypted. In this case it's safe to reuse initial values because a new key is chosen for
     * every encrypted secret.
     */
    byte[] INITIALIZATION_VECTOR = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

    byte[] decrypt(byte[] key, byte[] contents);

    byte[] digest(byte[] keyBytes, byte[] contents);
}
