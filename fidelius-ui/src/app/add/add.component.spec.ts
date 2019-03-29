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

import { async, ComponentFixture, TestBed} from '@angular/core/testing';

import { AddComponent } from './add.component';
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Credential, CredentialService, IActiveDirectory } from '../../services/credential.service';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { TdDialogService } from '@covalent/core';
import { MatInputModule, MatSnackBarModule, MatDialogModule, MatSnackBar } from '@angular/material';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import { MainComponent } from '../main/main.component';
import { BrowserService } from '../../services/browser.service';
import { By } from '@angular/platform-browser';

describe('AddComponent', () => {
  let component: AddComponent;
  let fixture: ComponentFixture<AddComponent>;
  let debugElement: DebugElement;

  class MockCredentialService {
    createCredential(credential: Credential): any {
      return Observable.of({secret: false});
    }

    getActiveDirectoryPasswordValidation(): Observable<IActiveDirectory> {
      return Observable.of({
        "validActiveDirectoryRegularExpression": "^[a-zA-Z0-9]{16,}$",
        "validActiveDirectoryDescription": "Must contain no special characters.\nMust be greater than 16 characters."
      });
    }
  }

  class MockMainComponent {
    selected = {
      "application": "TESTAGS",
      "region": "east",
      "account": {
        "alias": "Prod",
        "sdlc": "prod"
      }
    };
  }

  class MockBrowserService {
    checkIfIEOrEdge(): boolean {
      return true;
    }
  }

  class MockSnackBarService {
    open(): any {
      return Observable.of({secret: false});
    }
  }

  class MockTdDialogService {
    openAlert(): any {
      return true;
    }
  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [FormsModule, ReactiveFormsModule, MatInputModule, HttpClientModule, MatSnackBarModule, MatDialogModule, BrowserAnimationsModule],
      providers: [ TdDialogService,
          {provide: CredentialService, useClass: MockCredentialService },
          {provide: BrowserService, useClass: MockBrowserService },
          {provide: MainComponent, useClass: MockMainComponent },
          {provide: MatSnackBar, useClass: MockSnackBarService }],
      declarations: [  AddComponent  ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
      .compileComponents();
  }));

  beforeEach( () => {
    fixture = TestBed.createComponent(AddComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.isIEOrEdge).toBeTruthy();
  });

  it(`should load properties from Selected`, async() => {
    expect(component.credential.account).toEqual('Prod');
    expect(component.credential.application).toEqual('TESTAGS');
    expect(component.credential.region).toEqual('east');

    expect(component.credential.account).not.toEqual('wrongProd');
    expect(component.credential.application).not.toEqual('testAgs');
    expect(component.credential.environment).not.toEqual('Prod');
  });

  it(`should display 'Add Secret'`, async() => {
    let element: DebugElement = debugElement.query(By.css('span'));
    expect(element.nativeElement.textContent).toEqual('Add Secret');
    expect(element.nativeElement.textContent).not.toEqual('Edit Secret');
  });

  it(`should display 'AGS' input disabled `, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name.application]'));
    expect(element.attributes.hasOwnProperty('disabled')).toBeTruthy();
  });

  it(`should display 'Account' input disabled`, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=account]'));
    expect(element.attributes.hasOwnProperty('disabled')).toBeTruthy();
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

  it(`should hide Full Qualified Name if no component or key`, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=longKey]'));
    expect(element.parent.properties.hidden).toBeTruthy();
  });

  it(`should have Full Qualified Name equal to AGS.Component.Account.Key`, async() => {
    let expected: string = 'TESTAGS.component.prod.testKey';
    component.credential.component = 'component';
    component.credential.shortKey = 'testKey';
    fixture.detectChanges();

    let element: DebugElement = debugElement.query(By.css('input[name=longKey]'));
    expect(element.attributes['ng-reflect-value']).toEqual(expected);

    component.credential.component = 'new';
    component.credential.shortKey = undefined;
    fixture.detectChanges();
    expect(element.attributes['ng-reflect-value']).not.toEqual(expected);
  });

  xit(`should resturn AGS.Account.Key when getLongKey gets called`, async() => {
    let expected: string = 'TESTAGS.prod.testKey';
    component.credential.shortKey = 'testKey';

    let result: string = component.getLongKey();

    expect(expected).toEqual(result);
  });

  xit(`should resturn AGS.Component.Account.Key when getLongKey gets called`, async() => {
    let expected: string = 'TESTAGS.component.prod.testKey';
    component.credential.shortKey = 'testKey';
    component.credential.component = 'component';

    let result: string = component.getLongKey();

    expect(expected).toEqual(result);
  });

  xit(`should display secret field as invalid if invalid length'`, async() => {
    let element: DebugElement = debugElement.query(By.css('input[name=secret]'));
    component.credential.secret = '12345';
    // component.credential.dispatchEvent(newEvent('input'));
    // component.addForm.control['secretControl'].input = '5555';
    // element.nativeElement.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    // expect(element.classes).toEqual('false');
    component.credential.secret = 'OneTwoThreeFourFiveSixSeven';
    fixture.detectChanges();

    // expect(element.classes).toEqual(true);
  });

  xit(`should display secret input as password field`, async() => {
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

  xit(`Should display error when component contains spaces`, async() => {

  });

  xit(`Should show snackbar on successful form submit`, async() => {

  });

  it(`Should close AddCredential screen after displaying snackbar on form submit`, async() => {

  });

  xit( `Should show dialog error dialog on error after submitting`, async() => {

  });

  xit( `Should emit closeSideNav event on cancel`, async() => {

  });

  it(`should emit closeSideNav event on close`, async() => {
    // spyOn(component.sideNavClosed, 'emit');

    component.closeSideNav(false);

    // expect(component.sideNavClosed.emit).toHaveBeenCalled();
    // expect(component.sideNavClosed.emit).toHaveBeenCalledWith(false);
  });

  it(`should call credentialService with new credential on submit`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'createCredential').and.returnValue(Observable.of({credential: true}));

    component.addCredential();
    fixture.detectChanges();

    expect(credentialService.createCredential).toHaveBeenCalled();
  });

  it(`should display 403 error if 403 error calling createCredential() `, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'createCredential').and.returnValue(
      Observable.throw(new HttpErrorResponse({error: {message: 'Unauthorized!', error: 'FORBIDDEN'}, status: 403})));
    spyOn(dialogService, 'openAlert');

    component.addCredential();
    fixture.detectChanges();

    expect(dialogService.openAlert).toHaveBeenCalledWith({message: 'Unauthorized!', title: 'Error: 403 FORBIDDEN', closeButton: 'Ok'});
  });

  it(`should display 403 error if 403 error received calling getCredentials()`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'getCredentials').and.returnValue(
      Observable.throw(new HttpErrorResponse({error: {message: 'Access Denied!', error: 'FORBIDDEN'}, status: 403})));
    spyOn(dialogService, 'openAlert');

    component.addCredential();
    fixture.detectChanges();

    expect(dialogService.openAlert).toHaveBeenCalledWith({message: 'Access Denied!',
      title: 'Error: 403 FORBIDDEN', closeButton: 'Ok'});
  });

  it(`should display Table Not Found error if 404 error received calling getCredentials() `, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'getCredentials').and.returnValue(
      Observable.throw(new HttpErrorResponse(
        {error: {message: 'Not Found', error: 'NOT FOUND'}, status: 404})));
    spyOn(dialogService, 'openAlert');

    component.addCredential();
    fixture.detectChanges();

    expect(dialogService.openAlert).toHaveBeenCalledWith({
      message: 'Not Found',
      title: 'Error: 404 NOT FOUND', closeButton: 'Ok'});
  });

  it(`should display Throttling error if 408 error calling getCredentials() `, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'getCredentials').and.returnValue(
      Observable.throw(new HttpErrorResponse(
        {error: {message: 'Request throttling encountered. Please try again later.', error: 'REQUEST TIMEOUT'}, status: 408})));
    spyOn(dialogService, 'openAlert');

    component.addCredential();
    fixture.detectChanges();

    expect(dialogService.openAlert).toHaveBeenCalledWith({
      message: 'Request throttling encountered. Please try again later.',
      title: 'Error: 408 REQUEST TIMEOUT', closeButton: 'Ok'});
  });

  it(`should open snack bar on success createCredential() call`, async() => {
    const credentialService: any = fixture.debugElement.injector.get(CredentialService);
    const snackbarService: any = fixture.debugElement.injector.get(MatSnackBar);
    spyOn(credentialService, 'createCredential').and.returnValue(Observable.of({credential: {longKey: true}}));
    spyOn(snackbarService, 'open').and.returnValue(Observable.of({credential: true}));

    component.addCredential();
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

  it(`should display error if user attempts to create duplicate credential`, async() => {
    component.credential.region = 'us-east-1';
    component.credential.application = 'APP';
    component.credential.environment = 'dev';
    component.credential.account = 'test_awsdev';
    component.credential.shortKey = 'testKey';
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const snackBarService: MatSnackBar = fixture.debugElement.injector.get(MatSnackBar);
    const errResponse: HttpErrorResponse = new HttpErrorResponse(
      {error: {message: 'Credential already exists!'}, status: 400});
    spyOn(credentialService, 'createCredential').and.returnValue(Observable.throw(errResponse));
    component.addCredential();
    expect(component.isDuplicateCredential).toEqual(true);
    expect(snackBarService.open).not.toHaveBeenCalled();
  });

  xit(`should display No Special Characters and 8-24 Character long message on secret input on load`, async() => {
    // expect(component.addForm.form.).toEqual('true');
    let element: DebugElement = debugElement.query(By.css('input[name=secret]'));

    element.nativeElement.value = '****';
    // element.nati
    // element = component.form.controls['secret'];
    // // fixture.detectChanges();
    // // tick();
    // expect(element.classes).toEqual('fase');
  });
});
