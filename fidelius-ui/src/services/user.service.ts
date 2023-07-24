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
import { HttpClient, HttpEvent, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/application';

@Injectable()
export class UserService {

  URL: string = API_URL;

  constructor(private _http: HttpClient) { }

  getUserRole(): Observable<any> {
      return this._http.get<IUser>(this.URL + '/auth/role');
  }

  getUserMemberships(): Observable<string[]> {
    return this._http.get<string[]>(this.URL + '/auth/memberships');
  }

  getAgs(): Observable<string[]> {
    return this._http.get<string[]>(this.URL + '/auth/ags');
  }
}

export interface IUser {
  name: string;
  role: string;
  memberships: string[];
}
