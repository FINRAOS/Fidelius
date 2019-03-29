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

import { HistoryComponent } from './history.component';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { Credential, CredentialService, Selected } from '../../services/credential.service';
import { Observable } from 'rxjs/Observable';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule, MatDialogModule, MatSnackBarModule, MatTableModule, MatSortModule } from '@angular/material';
import {HttpClientModule, HttpErrorResponse} from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TdDialogService } from '@covalent/core';
import { By } from '@angular/platform-browser';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import { ClipboardModule } from 'ngx-clipboard';

describe('HistoryComponent', () => {
  let component: HistoryComponent;
  let fixture: ComponentFixture<HistoryComponent>;
  let debugElement: DebugElement;

  class MockCredentialService {
    getCredentialHistory(credential: Credential): any {
      return Observable.of({history: false});
    }
  }

  class MockTdDialogService {
    openAlert(): any {
      return true;
    }
  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [MatTableModule, FormsModule, ReactiveFormsModule, MatInputModule, MatSortModule,
        HttpClientModule, MatSnackBarModule, MatDialogModule, BrowserAnimationsModule, ClipboardModule],
      providers: [ {provide: TdDialogService, useClass: MockTdDialogService },
                   {provide: CredentialService, useClass: MockCredentialService } ],
      declarations: [ HistoryComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HistoryComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;

    component.selected = new Selected();
    component.selected.application = 'TESTAGS';
    component.selected.account.alias = 'Prod';

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it(`should load properties from Selected`, async() => {
    expect(component.credential.account).toEqual('Prod');
    expect(component.credential.application).toEqual('TESTAGS');

    expect(component.credential.account).not.toEqual('notProd');
    expect(component.credential.application).not.toEqual('notTESTAGS');
  });

  it(`should call getHistory() on init`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'getCredentialHistory').and.returnValue(Observable.of({history: 'test'}));
    component.ngOnInit();
    fixture.detectChanges();

    expect(component.loading).toBeTruthy();
    expect(credentialService.getCredentialHistory).toHaveBeenCalled();
    expect(component.dataSource.data).toEqual('test');
    expect(component.loading).toBeFalsy();

  });

  it(`should throw error if getHistory() fails to retrieve data`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'getCredentialHistory').and.returnValue(
      Observable.throw(new HttpErrorResponse({error: {message: 'Error. Credential History Not Found', error: 'NOT FOUND'}, status: 404})));
    spyOn(dialogService, 'openAlert');
    component.ngOnInit();
    fixture.detectChanges();

    expect(credentialService.getCredentialHistory).toHaveBeenCalled();
    expect(dialogService.openAlert).toHaveBeenCalledWith(
      {message: 'Error. Credential History Not Found', title: 'Error: 404 NOT FOUND', closeButton: 'Ok'});

  });

  it(`should display 403 error if 403 error received calling getHistory()`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'getCredentialHistory').and.returnValue(
      Observable.throw(new HttpErrorResponse(
        {error: {message: 'The current user has insufficient permissions to perform this action.', error: 'FORBIDDEN'}, status: 403})));
    spyOn(dialogService, 'openAlert');

    component.getHistory();
    fixture.detectChanges();

    expect(dialogService.openAlert).toHaveBeenCalledWith({message: 'The current user has insufficient permissions to perform this action.',
      title: 'Error: 403 FORBIDDEN', closeButton: 'Ok'});
  });

  it(`should display Throttling error if 408 error calling getHistory() `, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'getCredentialHistory').and.returnValue(
      Observable.throw(new HttpErrorResponse(
        {error: {message: 'Request throttling encountered. Please try again later.', error: 'REQUEST TIMEOUT'}, status: 408})));
    spyOn(dialogService, 'openAlert');

    component.getHistory();
    fixture.detectChanges();

    expect(dialogService.openAlert).toHaveBeenCalledWith({
      message: 'Request throttling encountered. Please try again later.',
      title: 'Error: 408 REQUEST TIMEOUT', closeButton: 'Ok'});
  });

});
