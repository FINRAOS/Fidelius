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

import { TestBed, inject } from '@angular/core/testing';

import { Account, AccountService } from './account.service';
import { HttpClientModule } from '@angular/common/http';
import { Observable } from 'rxjs';

describe('AccountService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientModule],
      providers: [AccountService]
    });
  });

  it('should be created', inject([AccountService], (service: AccountService) => {
    expect(service).toBeTruthy();
  }));

  it('should group accounts by sdlc and sort by alias', inject ([AccountService], (service: AccountService) => {
    spyOn((service as any)._http, 'get').and.returnValue(Observable.of(testAccountList));
    let results: Account[];
    service.getAccounts().subscribe( (accounts: Account[]) => {results = accounts;});

    expect(results.length).toEqual(9);
    expect(results[0].alias).toEqual('A-DEV');
    expect(results[1].alias).toEqual('C-DEV');
    expect(results[2].alias).toEqual('L-DEV');
    expect(results[3].alias).toEqual('PROD-A');
    expect(results[4].alias).toEqual('PROD-B');
    expect(results[5].alias).toEqual('PROD-z');
    expect(results[6].alias).toEqual('A-QA');
    expect(results[7].alias).toEqual('L-QA');
    expect(results[8].alias).toEqual('Z-QA');
  }));
});



const testAccountList: any[] = [
  {
    "accountId": "12345678913",
    "name": "",
    "sdlc": "prod",
    "alias": "PROD-B",
    "regions": [
      {
        "name": "us-east-1"
      }
    ]
  },
  {
    "accountId": "12345678910",
    "name": "",
    "sdlc": "qa",
    "alias": "Z-QA",
    "regions": [
      {
        "name": "us-east-1"
      },
      {
        "name": "us-east-2"
      }
    ]
  },
  {
    "accountId": "12345678911",
    "name": "",
    "sdlc": "dev",
    "alias": "L-DEV",
    "regions": [
      {
        "name": "us-east-1"
      }
    ]
  },
  {
    "accountId": "12345678912",
    "name": "",
    "sdlc": "qa",
    "alias": "A-QA",
    "regions": [
      {
        "name": "us-east-1"
      }
    ]
  },
  {
    "accountId": "12345678923",
    "name": "",
    "sdlc": "dev",
    "alias": "A-DEV",
    "regions": [
      {
        "name": "us-east-1"
      }
    ]
  },
  {
    "accountId": "12345678914",
    "name": "",
    "sdlc": "qa",
    "alias": "L-QA",
    "regions": [
      {
        "name": "us-east-1"
      }
    ]
  },
  {
    "accountId": "12345678919",
    "name": "",
    "sdlc": "dev",
    "alias": "C-DEV",
    "regions": [
      {
        "name": "us-east-1"
      }
    ]
  },
  {
    "accountId": "12345678913",
    "name": "",
    "sdlc": "prod",
    "alias": "PROD-z",
    "regions": [
      {
        "name": "us-east-1"
      }
    ]
  },
  {
    "accountId": "12345678913",
    "name": "",
    "sdlc": "prod",
    "alias": "PROD-A",
    "regions": [
      {
        "name": "us-east-1"
      }
    ]
  }
];
