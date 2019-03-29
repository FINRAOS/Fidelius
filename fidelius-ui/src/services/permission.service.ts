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

import { Injectable } from '@angular/core';
import { IUser } from './user.service';
import { Account } from './account.service';
import { PERMISSIONS } from '../config/permissions';
@Injectable()
export class PermissionService {

  constructor() { }

  getAuthorizations(user: IUser, account: Account): Permission {
    let authorizations: Permission = new Permission();
    let accountPermissions: any = [];
    if (account && account.sdlc && user && user.role) {
      accountPermissions = PERMISSIONS.permissions.accounts[account.sdlc].roles[user.role];
      if (accountPermissions) {
        for (let i of accountPermissions) {
          authorizations[i] = true;
        }
      }
    }
    return authorizations;
  }
}

export class Permission implements IPermission {
  createCredential: boolean = false;
  updateCredential: boolean = false;
  viewCredential: boolean = false;
  viewCredentialHistory: boolean = false;
  viewCredentialSecret: boolean = false;
  deleteCredential: boolean = false;
}

interface IPermission {
  createCredential: boolean;
  updateCredential: boolean;
  viewCredential: boolean;
  viewCredentialHistory: boolean;
  viewCredentialSecret: boolean;
  deleteCredential: boolean;
}
