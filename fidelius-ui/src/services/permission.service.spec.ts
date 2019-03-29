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

import { TestBed, inject, ComponentFixture } from '@angular/core/testing';

import { Permission, PermissionService } from './permission.service';
import { IUser } from './user.service';
import { Account } from './account.service';

describe('PermissionService', () => {
  let component: PermissionService;
  let fixture: ComponentFixture<PermissionService>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PermissionService]
    });

    component = new PermissionService();
  });

  it('should be created', inject([PermissionService], (service: PermissionService) => {
    expect(service).toBeTruthy();
  }));

  it('should return all false authorizations if no User or Account', inject([PermissionService], (service: PermissionService) => {
   let account: Account = undefined;
   let user: IUser = undefined;

   let authorizations: Permission = component.getAuthorizations(user, account);

   expect(authorizations.createCredential).toBeFalsy();
   expect(authorizations.viewCredentialSecret).toBeFalsy();
   expect(authorizations.viewCredential).toBeFalsy();
   expect(authorizations.viewCredentialHistory).toBeFalsy();
   expect(authorizations.updateCredential).toBeFalsy();
   expect(authorizations.deleteCredential).toBeFalsy();
  }));

  it('should return true on createCredential if role contains createCredential', inject([PermissionService], (service: PermissionService) => {
    let account: Account = new Account();
    account.sdlc = 'dev';
    let user: IUser = { name: 'testUser', role: 'DEV', memberships: []};

    let authorizations: Permission = component.getAuthorizations(user, account);

    expect(authorizations.createCredential).toBeTruthy();
  }));

  it('should return false on deleteCredential if role does not contain deleteCredential', inject([PermissionService], (service: PermissionService) => {
    let account: Account = new Account();
    account.sdlc = 'prod';
    let user: IUser = { name: 'testUser', role: 'DEV', memberships: []};

    let authorizations: Permission = component.getAuthorizations(user, account);

    expect(authorizations.deleteCredential).toBeFalsy();
  }));

  it('should return false on deleteCredential if role does not contain deleteCredential', inject([PermissionService], (service: PermissionService) => {
    let account: Account = new Account();
    account.sdlc = 'prod';
    let user: IUser = { name: 'testUser', role: 'PROD', memberships: []};

    let authorizations: Permission = component.getAuthorizations(user, account);

    expect(authorizations.deleteCredential).toBeFalsy();
  }));

  it('should return true on deleteCredential if role contains deleteCredential', inject([PermissionService], (service: PermissionService) => {
    let account: Account = new Account();
    account.sdlc = 'prod';
    let user: IUser = { name: 'testUser', role: 'MASTER', memberships: []};

    let authorizations: Permission = component.getAuthorizations(user, account);

    expect(authorizations.deleteCredential).toBeTruthy();
  }));

});
