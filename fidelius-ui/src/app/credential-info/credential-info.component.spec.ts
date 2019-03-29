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
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { MatButtonModule, MatTabsModule } from '@angular/material';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

import { CredentialInfoComponent } from './credential-info.component';
import { MainComponent } from '../main/main.component';
import { Credential, CredentialService, Selected } from '../../services/credential.service';
import { AlertService } from '../../services/alert.service';
import { ActivatedRoute } from '@angular/router';
import { BrowserService } from '../../services/browser.service';
import { Observable } from 'rxjs/Observable';

describe('CredentialInfoComponent', () => {
  let component: CredentialInfoComponent;
  let fixture: ComponentFixture<CredentialInfoComponent>;
  let debugElement: DebugElement;

  class MockMainComponent {
    selected = {
      "application": "TESTAGS",
      "region": "east",
      "account": {
        "alias": "Prod",
        "sdlc": "prod"
      }
    };

    selectedCredential = {
    "application": "APP",
    "shortKey": "test",
    "longKey": "test",
    "environment": "test",
    "component": undefined,
    "lastUpdatedBy": "",
    "lastUpdatedDate": "",
    "region": ""};

    authorizations = {
      viewCredential: true
    }
  }

  class MockCredentialService{
    getCredential(selected: Selected, credential: Credential): any {
      return Observable.of({credential: {}});
    }
  }

  class MockAlertService{
    openAlert():any{}
  }

  class MockActivatedRoute{
    snapshot = {params: {tab: "test"}};
  }

  class MockBrowserService {
    checkIfIEOrEdge(): boolean {
      return true;
    }
  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientModule, BrowserAnimationsModule, MatTabsModule, MatButtonModule],
      declarations: [ CredentialInfoComponent ],
      providers: [
        {provide: MainComponent, useClass: MockMainComponent },
        {provide: CredentialService, useClass: MockCredentialService },
        {provide: AlertService, useClass: MockAlertService },
        {provide: BrowserService, useClass: MockBrowserService },
        {provide: ActivatedRoute, useClass: MockActivatedRoute },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CredentialInfoComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit(`should emit closeSideNav event on close with true`, async() => {
    // spyOn(component.sideNavClosed, 'emit');

    component.closeSideNav(true);

    // expect(component.sideNavClosed.emit).toHaveBeenCalled();
    // expect(component.sideNavClosed.emit).toHaveBeenCalledWith(true);
  });

  xit(`should emit closeSideNav event on close`, async() => {
    // spyOn(component.sideNavClosed, 'emit');

    component.closeSideNav(false);

    // expect(component.sideNavClosed.emit).toHaveBeenCalled();
    // expect(component.sideNavClosed.emit).toHaveBeenCalledWith(false);
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
      "region": ""})));

    component.loadCredential();
    fixture.detectChanges();

    expect(credentialService.getCredential).toHaveBeenCalledTimes(1);
    expect(component.credential.shortKey).toEqual("newValue");
    expect(component.credential.application).toEqual("APP");
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
    expect(component.credential.shortKey).toEqual(undefined);
    expect(component.credential.application).toEqual(undefined);
    expect(component.isLoading).toBeFalsy();
    expect(component.hasError).toBeTruthy();
  });

  xit(`should set the information tab active if selected tab variable is 0`, async() => {
    component.selectedTab = 0;
    let infoElement: DebugElement = debugElement.query(By.css('mat-tab[name=information-tab]'));
    let historyElement: DebugElement = debugElement.query(By.css('mat-tab[name=history-tab]'));
    expect(infoElement.parent.properties.isActive).toBeTruthy();
    expect(historyElement.parent.properties.isActive).toBeFalsy();
  });

  xit(`should set the history tab active if selected tab variable is 1`, async() => {
    component.selectedTab = 1;
    let infoElement: DebugElement = debugElement.query(By.css('mat-tab[name=information-tab]'));
    let historyElement: DebugElement = debugElement.query(By.css('mat-tab[name=history-tab]'));
    expect(infoElement.parent.properties.isActive).toBeFalsy();
    expect(historyElement.parent.properties.isActive).toBeTruthy();
  });
});
