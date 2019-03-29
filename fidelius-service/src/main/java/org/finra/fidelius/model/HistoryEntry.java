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

import java.util.Date;

public class HistoryEntry {

    private Integer revision;
    private String updatedBy;
    private String updatedDate;

    public HistoryEntry(Integer revision, String updatedBy, String updatedDate) {
        this.revision = revision;
        this.updatedBy = updatedBy;
        this.updatedDate = updatedDate;
    }

    public Integer getRevision() {
        return revision;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    @Override
    public String toString() {
        return "HistoryEntry{" +
                "revision=" + revision +
                ", updatedBy='" + updatedBy + '\'' +
                ", updatedDate='" + updatedDate + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HistoryEntry that = (HistoryEntry) o;

        if (!revision.equals(that.revision)) return false;
        if (!updatedBy.equals(that.updatedBy)) return false;
        return updatedDate.equals(that.updatedDate);
    }

    @Override
    public int hashCode() {
        int result = revision.hashCode();
        result = 31 * result + updatedBy.hashCode();
        result = 31 * result + updatedDate.hashCode();
        return result;
    }
}
