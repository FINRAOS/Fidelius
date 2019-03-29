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

import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CredentialsTableComponent } from './credentials-table.component';
import {
  MatRipple, MatRippleModule, MatTableModule, MatSortModule, MatPaginatorModule,
  MatTable, MatTableDataSource, MatButtonModule, MatMenuModule
} from '@angular/material';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MainComponent } from '../main/main.component';
import { ICredential } from '../../services/credential.service';
import { Observable } from 'rxjs/Observable';

describe('CredentialsTableComponent', () => {
  let component: CredentialsTableComponent;
  let fixture: ComponentFixture<CredentialsTableComponent>;

  class MockMainComponent {
    selected = {
      "application": "TESTAGS",
      "region": "east",
      "account": {
        "alias": "Prod",
        "sdlc": "prod"
      }
    };

    dataSource: MatTableDataSource<ICredential> = new MatTableDataSource();

  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CredentialsTableComponent ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        {provide: MainComponent, useClass: MockMainComponent },
      ],
      imports: [ MatPaginatorModule, MatSortModule, MatTableModule, MatButtonModule, MatMenuModule, NoopAnimationsModule],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CredentialsTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
