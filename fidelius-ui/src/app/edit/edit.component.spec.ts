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

import { EditComponent } from './edit.component';
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSnackBar, MatSnackBarModule } from '@angular/material';
import {HttpClient, HttpClientModule, HttpErrorResponse} from '@angular/common/http';
import { TdDialogService } from '@covalent/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import {
  Credential, CredentialService, Selected, ICredential,
  IActiveDirectory
} from '../../services/credential.service';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import { MainComponent } from "../main/main.component";
import { BrowserService } from '../../services/browser.service';
import { AlertService } from '../../services/alert.service';

describe('EditComponent', () => {
  let component: EditComponent;
  let fixture: ComponentFixture<EditComponent>;
  let debugElement: DebugElement;

  class MockCredentialService {
    updateCredential(credential: Credential): any {
      return Observable.of({secret: false});
    }

    getCredential(selected: Selected, credential: Credential): any {
      return Observable.of({
        "shortKey": "key",
        "longKey": "APP.prod.key",
        "environment": "prod",
        "component": undefined,
        "lastUpdatedBy": "",
        "lastUpdatedDate": "",
        "region": ""});
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

    getSourceNames(account: string, region: string, sourceType: string): any {
      return Observable.of(
      ["test", "test2"]
      )
    }

    getSourceTypes(): any {
      return Observable.of(
        ["-", "Aurora", "RDS", "Service Account"]
      )
    }

    getActiveDirectoryPasswordValidation(): Observable<IActiveDirectory> {
      return Observable.of({
        "validActiveDirectoryRegularExpression": "^[a-zA-Z0-9]{16,}$",
        "validActiveDirectoryDescription": "Must contain no special characters.\nMust be greater than 16 characters."
      });
    }
  }

  class MockTdDialogService {
    openAlert(): any {
      return true;
    }
  }

  class MockSnackBarService {
    open(): any {
      return Observable.of({secret: false});
    }
  }

  class MockMainComponent {
    selectedCredential = {
      "shortKey": "key",
      "longKey": "APP.prod.key",
      "environment": "prod",
      "component": undefined,
      "lastUpdatedBy": "",
      "lastUpdatedDate": "",
      "region": ""};

    selected = {
      "application": "APP",
      "account": {
      "alias": "Prod",
      "sdlc": "prod"
    }};

    closeSideNavAndRefresh(closed: Boolean): void{}

  }

  class MockBrowserService {
    checkIfIEOrEdge(): boolean {
      return true;
    }
  }

  class MockAlertService {
    openAlert():any {
    }
  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ EditComponent ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      imports: [FormsModule, ReactiveFormsModule, MatInputModule, MatSelectModule, MatFormFieldModule, MatAutocompleteModule, MatSnackBarModule, MatDialogModule, BrowserAnimationsModule, HttpClientModule],
      providers: [ {provide: TdDialogService, useClass: MockTdDialogService },
        {provide: CredentialService, useClass: MockCredentialService },
        {provide: MainComponent, useClass: MockMainComponent },
        {provide: AlertService, useClass: MockAlertService },
        {provide: BrowserService, useClass: MockBrowserService },
        {provide: MatSnackBar, useClass: MockSnackBarService }],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EditComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it(`should load properties from Selected`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const credential =  {
      "account": 'Prod',
      "application": 'APP',
      "shortKey": "key",
      "longKey": "APP.prod.key",
      "environment": "prod",
      "component": undefined,
      "lastUpdatedBy": "",
      "lastUpdatedDate": "",
      "region": undefined};

    const selected = {
      "application": "APP",
      "account": {
        "alias": "Prod",
        "sdlc": "prod"
      }};

    spyOn(credentialService, 'getCredential').and.callThrough();

    component.ngOnInit();
    fixture.detectChanges();

    expect(credentialService.getCredential).toHaveBeenCalledWith(selected, credential);
  });

  it(`should display 'Edit Secret'`, async() => {
    let element: DebugElement = debugElement.query(By.css('span'));
    expect(element.nativeElement.textContent).toEqual('Edit Secret');
    expect(element.nativeElement.textContent).not.toEqual('Add Credential');
  });

  it(`should display 'AGS' input disabled `, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=application]'));
    expect(element.attributes.hasOwnProperty('disabled')).toBeTruthy();
  });

  xit(`should display 'Account' input disabled`, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=account]'));
    expect(element.attributes.hasOwnProperty('disabled')).toBeTruthy();
  });

  it(`should display 'Full Qualified Name' as readonly`, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=longKey]'));
    expect(element.attributes.hasOwnProperty('readonly')).toBeTruthy();
  });

  xit(`should display Full Qualified Name if AGS, Component, Account, Key are present`, async() => {
    component.credential.component = 'notEmpty';
    component.credential.shortKey = 'notEmptyKey';
    fixture.detectChanges();

    let element: DebugElement = debugElement.query(By.css('input[name=longKey]'));
    expect(element.parent.properties.hidden).toBeFalsy();
  });

  it(`should return AGS.Account.Key when getLongKey gets called`, async() => {
    let expected: string = 'TESTAGS.prod.testKey';
    component.credential.shortKey = 'testKey';
    component.credential.environment = 'prod';
    component.credential.application = 'TESTAGS';

    let result: string = component.getLongKey();

    expect(expected).toEqual(result);
  });

  it(`should load getCredential `, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'getCredential').and.callFake(() => (Observable.of({
      "application": "APP",
      "shortKey": "newValue",
      "longKey": "test",
      "environment": "dev",
      "component": undefined,
      "lastUpdatedBy": "",
      "lastUpdatedDate": "",
      "region": "east"})));

    component.loadCredential();
    fixture.detectChanges();

    expect(credentialService.getCredential).toHaveBeenCalledTimes(1);
    expect(component.credential.shortKey).toEqual("newValue");
    expect(component.credential.application).toEqual("APP");
    expect(component.credential.region).toEqual("east");
    expect(component.isLoading).toBeFalsy();
    expect(component.hasError).toBeFalsy();
  });

  it(`should handle error on load getCredential `, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const alertService: AlertService = fixture.debugElement.injector.get(AlertService);
    spyOn(credentialService, 'getCredential').and.callFake( () => (Observable.throw({status: 404})));
    spyOn(alertService, 'openAlert');

    component.loadCredential();
    fixture.detectChanges();

    expect(credentialService.getCredential).toHaveBeenCalledTimes(1);
    expect(alertService.openAlert).toHaveBeenCalledWith({status: 404});
    expect(component.isLoading).toBeFalsy();
    expect(component.hasError).toBeTruthy();
  });

  it(`should return AGS.Component.Account.Key when getLongKey gets called`, async() => {
    let expected: string = 'TESTAGS.component.prod.testKey';
    component.credential.shortKey = 'testKey';
    component.credential.component = 'component';
    component.credential.environment = 'prod';
    component.credential.application = 'TESTAGS';

    let result: string = component.getLongKey();

    expect(expected).toEqual(result);
  });

  it(`should hide Full Qualified Name if no component or key`, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=longKey]'));
    expect(element.parent.properties.hidden).toBeTruthy();
  });

  it(`should have Full Qualified Name equal to AGS.Component.Account.Key`, async() => {
    let expected: string = 'TESTAGS.component.Prod.testKey';
    component.credential.component = 'component';
    component.credential.shortKey = 'testKey';
    component.credential.environment = 'Prod';
    fixture.detectChanges();

    let element: DebugElement = debugElement.query(By.css('input[name=longKey]'));
    expect(element.attributes['ng-reflect-value']).toEqual(expected);

    component.credential.component = 'new';
    component.credential.shortKey = undefined;
    fixture.detectChanges();
    expect(element.attributes['ng-reflect-value']).not.toEqual(expected);
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
    let element: DebugElement = debugElement.query(By.css('input[name=secret]'));
    component.hideSecret = false;
    fixture.detectChanges();
    expect(element.attributes['ng-reflect-type']).toEqual('text');
    expect(element.attributes['ng-reflect-type']).not.toEqual('password');
  });

  it(`should emit closeSideNav event on close with false`, async() => {
    const parentComponent: MainComponent = fixture.debugElement.injector.get(MainComponent);
    spyOn(parentComponent, 'closeSideNavAndRefresh');

    component.closeSideNav(false);

    expect(parentComponent.closeSideNavAndRefresh).toHaveBeenCalledTimes(1);
    expect(parentComponent.closeSideNavAndRefresh).toHaveBeenCalledWith(false);
  });

  it(`should call credentialService with updated credential on submit`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'updateCredential').and.returnValue(Observable.of({credential: true}));

    component.updateCredential();
    fixture.detectChanges();

    expect(credentialService.updateCredential).toHaveBeenCalled();
  });

  it(`should display error if error calling updateCredential() `, async() => {
    const credentialService: any = fixture.debugElement.injector.get(CredentialService);
    const alertService: AlertService = fixture.debugElement.injector.get(AlertService);
    spyOn(credentialService, 'updateCredential').and.callFake( () => (Observable.throw({status: 404})));
    spyOn(alertService, 'openAlert');

    component.updateCredential();
    fixture.detectChanges();

    expect(alertService.openAlert).toHaveBeenCalledTimes(1);
    expect(alertService.openAlert).toHaveBeenCalledWith({status: 404});
    expect(component.sendingForm).toBeFalsy();
  });

  it(`should open snack bar on success updateCredential() call`, async() => {
    const credentialService: any = fixture.debugElement.injector.get(CredentialService);
    const snackbarService: any = fixture.debugElement.injector.get(MatSnackBar);
    spyOn(credentialService, 'updateCredential').and.returnValue(Observable.of({credential: {longKey: true}}));
    spyOn(snackbarService, 'open').and.returnValue(Observable.of({credential: true}));

    component.updateCredential();
    fixture.detectChanges();

    expect(snackbarService.open).toHaveBeenCalled();
  });

  it(`should set password pattern when credential.isActiveDirectory is set`, async() => {
    let pattern: string = 'testPattern';
    component.activeDirectory.validActiveDirectoryRegularExpression = pattern;
    component.credential.isActiveDirectory = true;

    component.setPasswordPattern();
    fixture.detectChanges();

    expect(component.passwordPattern).toEqual(pattern);
    expect(component.passwordPattern).toEqual(component.activeDirectory.validActiveDirectoryRegularExpression);
    expect(component.activeDirectory.validActiveDirectoryRegularExpression).toEqual(pattern);
  });

  it(`should set default pattern when credential.isActiveDirectory is set`, async() => {
    let pattern: string = 'testDefaultPattern';
    component.defaultPasswordPattern = pattern;
    component.credential.isActiveDirectory = false;

    component.setPasswordPattern();
    fixture.detectChanges();

    expect(component.passwordPattern).toEqual(pattern);
    expect(component.defaultPasswordPattern).toEqual(pattern);
  });

  xit(`Should display error when component contains spaces`, async() => {

  });

  xit(`Should show snackbar on successful form submit`, async() => {

  });

  xit(`Should close AddCredential screen after displaying snackbar on form submit`, async() => {

  });

  xit( `Should show dialog error dialog on error after submitting`, async() => {

  });

  xit( `Should emit closeSideNav event on cancel`, async() => {

  });

  xit( `Should emit closeSideNav event when clicking on 'x' `, async() => {

  });

  xit(`should display No Special Characters and 8-24 Character long message on secret input on load`, async() => {
    // expect(component.addForm.form.).toEqual('true');
    let element: DebugElement = debugElement.query(By.css('input[name=secret]'));

    element.nativeElement.value = '****';
    // element.nati
    // element = component.form.controls['secret'];
    // fixture.detectChanges();
    // tick();
    // expect(element.classes).toEqual('fase');
  });
});
