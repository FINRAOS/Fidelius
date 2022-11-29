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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;

public class EncryptedCredential {

    private String datakey;
    private String credential;
    private String hmac;
    private String version;
    private String fullName;
    private String updateBy;
    private String updatedOn;
    private String component;
    private String sdlc;

    private byte[] dataKeyBytes;
    private byte[] credentialBytes;
    private byte[] hmacBytes;

    public String getDatakey() {
        return datakey;
    }

    public EncryptedCredential setDatakey(String datakey) {
        this.datakey = datakey;
        this.dataKeyBytes = base64AttributeValueToBytes(datakey);
        return this;
    }

    public String getCredential() {
        return credential;
    }

    public EncryptedCredential setCredential(String credential) {
        this.credential = credential;
        this.credentialBytes = base64AttributeValueToBytes(credential);
        return this;
    }

    public String getHmac() {
        return hmac;
    }

    public EncryptedCredential setHmac(String hmac) {
        this.hmac = hmac;
        this.hmacBytes = hexAttributeValueToBytes(hmac);
        return this;
    }

    public String getVersion() {
        return version;
    }

    public EncryptedCredential setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public EncryptedCredential setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public EncryptedCredential setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
        return this;
    }
    public String getUpdateOn() {
        return updatedOn;
    }

    public EncryptedCredential setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
        return this;
    }

    public String getComponent() {
        return component;
    }

    public EncryptedCredential setComponent(String component) {
        this.component = component;
        return this;
    }

    public String getSdlc() {
        return sdlc;
    }

    public EncryptedCredential setSdlc(String sdlc) {
        this.sdlc = sdlc;
        return this;
    }

    public byte[] getDataKeyBytes() {
        return dataKeyBytes;
    }

    public byte[] getCredentialBytes() {
        return credentialBytes;
    }

    public byte[] getHmacBytes() {
        return hmacBytes;
    }

    private byte[] base64AttributeValueToBytes(String value) {
        return Base64.decodeBase64(value);
    }

    private byte[] hexAttributeValueToBytes(String value) {
        try {
            return Hex.decodeHex(value.toCharArray());
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "EncryptedCredential{" +
                "datakey='" + datakey + '\'' +
                ", credential='" + credential + '\'' +
                ", hmac='" + hmac + '\'' +
                ", version='" + version + '\'' +
                ", fullName='" + fullName + '\'' +
                ", updateBy='" + updateBy + '\'' +
                ", updatedOn='" + updatedOn + '\'' +
                ", component='" + component + '\'' +
                ", sdlc='" + sdlc + '\'' +
                ", dataKeyBytes=" + Arrays.toString(dataKeyBytes) +
                ", credentialBytes=" + Arrays.toString(credentialBytes) +
                ", hmacBytes=" + Arrays.toString(hmacBytes) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EncryptedCredential that = (EncryptedCredential) o;

        if (!datakey.equals(that.datakey)) return false;
        if (!credential.equals(that.credential)) return false;
        if (!hmac.equals(that.hmac)) return false;
        if (!version.equals(that.version)) return false;
        if (!fullName.equals(that.fullName)) return false;
        if (updateBy != null ? !updateBy.equals(that.updateBy) : that.updateBy != null) return false;
        if (updatedOn != null ? !updatedOn.equals(that.updatedOn) : that.updatedOn != null) return false;
        if (component != null ? !component.equals(that.component) : that.component != null) return false;
        if (sdlc != null ? !sdlc.equals(that.sdlc) : that.sdlc != null) return false;
        if (!Arrays.equals(dataKeyBytes, that.dataKeyBytes)) return false;
        if (!Arrays.equals(credentialBytes, that.credentialBytes)) return false;
        return Arrays.equals(hmacBytes, that.hmacBytes);
    }

    @Override
    public int hashCode() {
        int result = datakey.hashCode();
        result = 31 * result + credential.hashCode();
        result = 31 * result + hmac.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + fullName.hashCode();
        result = 31 * result + (updateBy != null ? updateBy.hashCode() : 0);
        result = 31 * result + (updatedOn != null ? updatedOn.hashCode() : 0);
        result = 31 * result + (component != null ? component.hashCode() : 0);
        result = 31 * result + (sdlc != null ? sdlc.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(dataKeyBytes);
        result = 31 * result + Arrays.hashCode(credentialBytes);
        result = 31 * result + Arrays.hashCode(hmacBytes);
        return result;
    }
}