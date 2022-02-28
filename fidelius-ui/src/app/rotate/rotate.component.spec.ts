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

import { RotateComponent } from './rotate.component';
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatDialogModule, MatInputModule, MatSnackBar, MatSnackBarModule, MatFormFieldModule, MatSelectModule } from '@angular/material';
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

describe('RotateComponent', () => {
  let component: RotateComponent;
  let fixture: ComponentFixture<RotateComponent>;
  let debugElement: DebugElement;

  class MockCredentialService {
    rotateCredential(credential: Credential): any {
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
      declarations: [ RotateComponent ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      imports: [FormsModule, ReactiveFormsModule, MatInputModule, MatSelectModule, MatAutocompleteModule, MatFormFieldModule,  MatSnackBarModule, MatDialogModule, BrowserAnimationsModule, HttpClientModule],
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
    fixture = TestBed.createComponent(RotateComponent);
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

  it(`should load getMetadata `, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'getMetadata').and.callFake(() => (Observable.of({
      "application": "APP",
      "shortKey": "newValue",
      "longKey": "test",
      "environment": "dev",
      "component": undefined,
      "lastUpdatedBy": "",
      "lastUpdatedDate": "",
      "sourceType": "RDS",
      "source" : "source",
      "region": "east"})));

    component.getMetadata();
    fixture.detectChanges();


    expect(credentialService.getMetadata).toHaveBeenCalledTimes(1);
    expect(component.metadata.source).toEqual("source");
    expect(component.metadata.sourceType).toEqual("RDS");
    expect(component.metadata.shortKey).toEqual("newValue");
    expect(component.metadata.application).toEqual("APP");
    expect(component.metadata.region).toEqual("east");
    expect(component.hasError).toBeFalsy();
  });

  it(`should handle error on load getMetadata `, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const alertService: AlertService = fixture.debugElement.injector.get(AlertService);
    spyOn(credentialService, 'getMetadata').and.callFake( () => (Observable.throw({status: 404})));
    spyOn(alertService, 'openAlert');

    component.getMetadata();
    fixture.detectChanges();

    expect(credentialService.getMetadata).toHaveBeenCalledTimes(1);
    expect(alertService.openAlert).toHaveBeenCalledWith({status: 404});
    expect(component.hasError).toBeTruthy();
  });
  
  it(`should display error if error calling rotateCredential() `, async() => {
    const credentialService: any = fixture.debugElement.injector.get(CredentialService);
    const snackbarService: any = fixture.debugElement.injector.get(MatSnackBar);
    spyOn(credentialService, 'rotateCredential').and.callFake( () => (Observable.throw({status: 404})));
    spyOn(snackbarService, 'open').and.returnValue(Observable.of({status: 404}));

    component.rotate();
    fixture.detectChanges();

    expect(snackbarService.open).toHaveBeenCalledTimes(1);
    expect(component.sendingForm).toBeFalsy();
  });

  it(`should open snack bar on success rotateCredential() call`, async() => {
    const credentialService: any = fixture.debugElement.injector.get(CredentialService);
    const snackbarService: any = fixture.debugElement.injector.get(MatSnackBar);
    spyOn(credentialService, 'rotateCredential').and.returnValue(Observable.of({credential: {longKey: true}}));
    spyOn(snackbarService, 'open').and.returnValue(Observable.of({credential: true}));

    component.rotateCredential();
    fixture.detectChanges();

    expect(snackbarService.open).toHaveBeenCalled();
  });

  xit(`Should display error when component contains spaces`, async() => {

  });

  xit(`Should show snackbar on successful form submit`, async() => {

  });

  xit(`Should close RotateCredential screen after displaying snackbar on form submit`, async() => {

  });

  xit( `Should show dialog error dialog on error after submitting`, async() => {

  });

  xit( `Should emit closeSideNav event on cancel`, async() => {

  });

  xit( `Should emit closeSideNav event when clicking on 'x' `, async() => {

  });

});
