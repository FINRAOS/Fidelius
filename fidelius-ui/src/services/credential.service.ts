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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { Account } from './account.service';
import { API_URL } from '../config/application';

@Injectable()
export class CredentialService {

  URL: string =  API_URL;
  constructor(private _http: HttpClient) { }

  getCredentials(selected: any): Observable<ICredential[]> {
    return this._http.get<ICredential[]>(this.URL + '/credentials?account=' + selected.account.alias +
      '&region=' + selected.region +
      '&application=' + selected.application.toUpperCase() +
      '&environment=' + selected.sdlc);
  }

  getCredential(selected: any, credential: Credential): Observable<ICredential> {
    let params: HttpParams = new HttpParams();
    params = params.append('account', selected.account.alias);
    params = params.append('region', selected.region);
    params = params.append('application', selected.application.toUpperCase());
    return this._http.get<ICredential>(this.URL + '/credentials/' + credential.longKey + '/', {params: params});
  }

  createCredential(credential: Credential): Observable<any> {
    return this._http.post<any>(this.URL + '/credentials/secret', credential);
  }

  updateCredential(credential: Credential): Observable<any> {
    return this._http.put<any>(this.URL + '/credentials/secret', credential);
  }

  deleteCredential(credential: Credential): Observable<any> {
    return this._http.delete<any>( this.URL + '/credentials/secret?application=' + credential.application.toUpperCase() +
      '&region=' + credential.region +
      '&environment=' + credential.environment +
      '&account=' + credential.account +
      '&component=' + credential.component +
      '&shortKey=' + credential.shortKey);
  }

  rotateCredential(credential: Credential): Observable<any> {
     return this._http.post<any>(this.URL + '/credentials/rotate', credential);
  }

  getSourceNames(account: string, region: string, sourceType: string){
    return this._http.get<any>( this.URL + '/sources?account=' + account +
      '&region=' + region +
      '&sourceType=' + sourceType.toLowerCase());
  }

  createMetadata(metadata: Metadata): Observable<any> {
    return this._http.post<any>(this.URL + '/credentials/metadata', metadata);
  }

  updateMetadata(metadata: Metadata){
    return this._http.put<any>(this.URL + '/credentials/metadata', metadata);
  }

  deleteMetadata(credential: Credential): Observable<any> {
    return this._http.delete<any>( this.URL + '/credentials/metadata?application=' + credential.application.toUpperCase() +
      '&region=' + credential.region +
      '&environment=' + credential.environment +
      '&account=' + credential.account +
      '&component=' + credential.component +
      '&shortKey=' + credential.shortKey);
  }

  getSecret(credential: Credential): Observable<any> {
    return this._http.get<Credential>(this.URL + '/credentials/secret?application=' + credential.application.toUpperCase() +
      '&region=' + credential.region +
      '&environment=' + credential.environment +
      '&account=' + credential.account +
      '&component=' + credential.component +
      '&shortKey=' + credential.shortKey);
  }

  getMetadata(credential: Credential): Observable<any>{
    return this._http.get<Credential>(this.URL + '/credentials/metadata?application=' + credential.application.toUpperCase() +
    '&region=' + credential.region +
    '&environment=' + credential.environment +
    '&account=' + credential.account +
    '&component=' + credential.component +
    '&shortKey=' + credential.shortKey);
  }

  getCredentialHistory(credential: Credential): Observable<any> {
    return this._http.get<History>(this.URL + '/credentials/history?application=' + credential.application.toUpperCase() +
      '&component=' + credential.component +
      '&account=' + credential.account +
      '&region=' + credential.region +
      '&environment=' + credential.environment +
      '&shortKey=' + credential.shortKey);
  }

  getActiveDirectoryPasswordValidation(): Observable<IActiveDirectory> {
    return this._http.get<IActiveDirectory>(this.URL + '/validActiveDirectoryRegularExpression');
  }
}

export interface IActiveDirectory {
  validActiveDirectoryRegularExpression: string;
  validActiveDirectoryDescription: string;
}

export interface ICredential {
  shortKey: string;
  longKey: string;
  environment: string;
  component: string;
  lastUpdatedBy: string;
  lastUpdatedDate: string;
  region: string;
}

export class Credential implements ICredential {
  shortKey: string = '';
  longKey: string = undefined;
  environment: string = '';
  component: string = undefined;
  lastUpdatedBy: string = '';
  lastUpdatedDate: string;
  secret: string = undefined;
  application: string = undefined;
  account: string = undefined;
  region: string = undefined;
  isActiveDirectory: boolean = undefined;
}

export class Metadata implements IMetadata {
  lastUpdatedBy: string = undefined;
  lastUpdatedDate: string  = undefined;
  component: string  = undefined;
  longKey: string  = undefined;
  shortKey: string  = undefined;
  sourceType: string  = undefined;
  source: string  = undefined;
  account: string  = undefined;
  region: string  = undefined;
  environment: string  = undefined;
  application: string  = undefined;
}

export class History extends Credential {
  history: IHistory[] = [];
}

export interface IHistory {
  revision: number;
  updatedBy: string;
  updatedDate: string;
}

export interface ISelected {
  account: Account;
  region: string;
  application: string;
  environment: string;
  key: string;
}

export class Selected implements ISelected {
  account: Account = new Account();
  region: string = '';
  application: string = '';
  environment: string = '';
  key: string = '';
}

export interface IMetadata {
  lastUpdatedBy: string;
  lastUpdatedDate: string;
  component: string;
  longKey: string;
  shortKey: string;
  sourceType: string;
  source: string;
  account: string;
  region: string;
  environment: string;
  application: string;
}
