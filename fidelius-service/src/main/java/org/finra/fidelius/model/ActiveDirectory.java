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

public class ActiveDirectory {
    private String validActiveDirectoryRegularExpression;
    private String validActiveDirectoryDescription;

    public ActiveDirectory() {
    }

    public ActiveDirectory(String validActiveDirectoryRegularExpression, String validActiveDirectoryDescription) {
        this.validActiveDirectoryRegularExpression = validActiveDirectoryRegularExpression;
        this.validActiveDirectoryDescription = validActiveDirectoryDescription;
    }

    public String getValidActiveDirectoryRegularExpression() {
        return validActiveDirectoryRegularExpression;
    }

    public void setValidActiveDirectoryRegularExpression(String validActiveDirectoryRegularExpression) {
        this.validActiveDirectoryRegularExpression = validActiveDirectoryRegularExpression;
    }

    public String getValidActiveDirectoryDescription() {
        return validActiveDirectoryDescription;
    }

    public void setValidActiveDirectoryDescription(String validActiveDirectoryDescription) {
        this.validActiveDirectoryDescription = validActiveDirectoryDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActiveDirectory that = (ActiveDirectory) o;

        if (validActiveDirectoryRegularExpression != null ? !validActiveDirectoryRegularExpression.equals(that.validActiveDirectoryRegularExpression) : that.validActiveDirectoryRegularExpression != null)
            return false;
        return validActiveDirectoryDescription != null ? validActiveDirectoryDescription.equals(that.validActiveDirectoryDescription) : that.validActiveDirectoryDescription == null;
    }

    @Override
    public int hashCode() {
        int result = validActiveDirectoryRegularExpression != null ? validActiveDirectoryRegularExpression.hashCode() : 0;
        result = 31 * result + (validActiveDirectoryDescription != null ? validActiveDirectoryDescription.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActiveDirectory{" +
                "validActiveDirectoryRegularExpression='" + validActiveDirectoryRegularExpression + '\'' +
                ", validActiveDirectoryDescription='" + validActiveDirectoryDescription + '\'' +
                '}';
    }
}
