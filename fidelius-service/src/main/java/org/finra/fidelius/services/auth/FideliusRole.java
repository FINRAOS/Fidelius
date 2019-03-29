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

package org.finra.fidelius.services.auth;

public enum FideliusRole {
    /**
     * Role with admin access across all accounts.  Role can delete in all accounts.
     */
    MASTER,
    /**
     * Role with admin access across all accounts.  Role cannot delete in prod accounts.
     */
    OPS,
    /**
     * Role with admin access on credentials that a user is member of and where accounts are not in prod.
     */
    DEV,
    /**
     * Role assigned to users who have no memberships
     */
    UNAUTHORIZED
}