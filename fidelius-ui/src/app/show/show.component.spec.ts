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

import { async, ComponentFixture, inject, TestBed } from '@angular/core/testing';

import { ShowComponent } from './show.component';
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement, InjectionToken, NO_ERRORS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, MatSnackBar, MatSnackBarModule } from '@angular/material';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { Credential, CredentialService, Selected } from '../../services/credential.service';
import { TdDialogService } from "@covalent/core";
import { ClipboardModule, ClipboardService } from 'ngx-clipboard';
import { By } from '@angular/platform-browser';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import { MainComponent } from '../main/main.component';
import { BrowserService } from '../../services/browser.service';
import { CredentialInfoComponent } from '../credential-info/credential-info.component';

describe('ShowComponent', () => {
  let component: ShowComponent;
  let fixture: ComponentFixture<ShowComponent>;
  let debugElement: DebugElement;

  class MockCredentialService {
    getSecret(credential: Credential): any {
      return Observable.of({secret: false});
    }
    getMetadata(selected: Selected, credential: Credential): any {
      return Observable.of({
        "shortKey": "key",
        "longKey": "APP.prod.key",
        "environment": "prod",
        "source": "source",
        "sourceType": "RDS",
        "component": undefined,
        "lastUpdatedBy": "",
        "lastUpdatedDate": "",
        "region": ""});
    }
    getCredentialHistory(credential: Credential): any {
      return Observable.of([{
        "revision": 1,
        "updatedBy": "name",
        "updatedDate": "1/1/11, 12:00 PM",
      }])
    };
    
  }

  class MockTdDialogService {
    openAlert(): any {
      return true;
    }
  }

  class MockClipBoardService {
    copyFromContent(): any {
      return true;
    }
  }

  class MockSnackBarService {
    open(): any {
      return Observable.of({secret: false});
    }
  }

  class MockMainComponent{
  }

  class MockCredentialInfoComponent{
  }
  
  class MockBrowserService{
    checkIfIEOrEdge():boolean{
      return false;
    }
  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ FormsModule, ReactiveFormsModule, MatInputModule, MatFormFieldModule, HttpClientModule, MatSnackBarModule, MatProgressSpinnerModule, MatDialogModule, BrowserAnimationsModule, ClipboardModule],
      providers: [ {provide: TdDialogService, useClass: MockTdDialogService },
                   {provide: CredentialService, useClass: MockCredentialService },
                   {provide: ClipboardService, useClass: MockClipBoardService },
                   {provide: MainComponent, useClass: MockMainComponent},
                   {provide: BrowserService, useClass: MockBrowserService},
                   {provide: CredentialInfoComponent, useClass: MockCredentialInfoComponent}
                  ],
      declarations: [ ShowComponent ],
      schemas: [NO_ERRORS_SCHEMA],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ShowComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;

    component.selected.account.alias = 'Prod';
    component.selected.application = 'TESTAGS';
    component.selected.region = 'east';

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it(`should load properties from Selected`, async() => {
    expect(component.credential.account).toEqual('Prod');
    expect(component.credential.application).toEqual('TESTAGS');

    expect(component.credential.account).not.toEqual('wrongProd');
    expect(component.credential.application).not.toEqual('testAgs');
  });

  it(`should display 'AGS' as readonly `, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=application]'));
    expect(element.attributes.hasOwnProperty('readonly')).toBeTruthy();
  });

  it(`should display 'Account' input as readonly`, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=account]'));
    expect(element.attributes.hasOwnProperty('readonly')).toBeTruthy();
  });

  it(`should display 'Full Qualified Name' as readonly`, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=longKey]'));
    expect(element.attributes.hasOwnProperty('readonly')).toBeTruthy();
  });

  it(`should display Full Qualified Name if AGS, Component, Account, Key are present`, async() => {
    component.credential.component = 'notEmpty';
    component.credential.shortKey = 'notEmptyKey';
    fixture.detectChanges();

    let element: DebugElement = debugElement.query(By.css('input[name=longKey]'));
    expect(element.parent.properties.hidden).toBeFalsy();
  });

  xit(`should display secret field as invalid if invalid length'`, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=secret]'));

  });

  it(`should display secret input as password field`, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=secret]'));
    expect(element.attributes['ng-reflect-type']).toEqual('password');
    expect(element.attributes['ng-reflect-type']).not.toEqual('text');
  });

  it(`should display secret input as text when user clicks view icon`, async() => {
    let element: DebugElement = debugElement.query(By.css('textarea[name=secret]'));
    component.hideSecret = false;
    fixture.detectChanges();
    expect(element.attributes['ng-reflect-type']).not.toEqual('password');
  });

  it(`should call loadSecret() if hideSecret is true`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'getSecret').and.returnValue(Observable.of({secret: 'test'}));
    component.hideSecret = true;

    component.showSecret();
    fixture.detectChanges();

    expect(credentialService.getSecret).toHaveBeenCalled();
    expect(component.credential.secret).toEqual('test');
  });

  it(`should not call loadSecret() if hideSecret is false`, async() => {
    component.hideSecret = false;

    component.showSecret();
    fixture.detectChanges();

    expect(component.credential.secret).toContain('loadingSecret');
    expect(component.hideSecret).toBeTruthy();
  });

  it(`should assign secret on return of loadSecret()`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'getSecret').and.returnValue(Observable.of({secret: 'newTest'}));

    component.loadSecret();
    fixture.detectChanges();

    expect(component.credential.secret).toContain('newTest');
    expect(component.loadedSecret).toBeTruthy();
  });

  it(`should display error if error loading secret on loadSecret()`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'getSecret').and.returnValue(Observable.throw(new Error("Error. Credential Not Found")));
    spyOn(dialogService, 'openAlert');

    component.loadSecret();
    fixture.detectChanges();

    expect(dialogService.openAlert).toHaveBeenCalledWith({message: 'Error. Credential Not Found', title: 'undefined Error', closeButton: 'Ok'});
    expect(component.loadedSecret).toBeFalsy()
  });

  it(`should call copyFromContent on copy()`, async() => {
    const clipboardService: ClipboardService = fixture.debugElement.injector.get(ClipboardService);
    const snackbarService: MatSnackBar = fixture.debugElement.injector.get(MatSnackBar);
    spyOn(clipboardService, 'copyFromContent').and.returnValue(Observable.of(true));
    spyOn(snackbarService, 'open');

    component.copy();
    fixture.detectChanges();

    expect(clipboardService.copyFromContent).toHaveBeenCalledWith(component.credential.secret);
    expect(snackbarService.open).toHaveBeenCalled();
  });

});
