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

import { MainComponent } from './main.component';
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import {
  MatDialogModule, MatSnackBarModule, MatMenuModule, MatTableModule, MatSidenav,
  MatSnackBar, MatButtonModule,
} from '@angular/material';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CovalentMediaModule, TdDialogService } from '@covalent/core';
import { By } from '@angular/platform-browser';
import { CredentialService, Selected, ICredential, Credential } from '../../services/credential.service';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import { Account, AccountService } from '../../services/account.service';
import { Permission, PermissionService } from '../../services/permission.service';
import { UserService } from '../../services/user.service';
import { BrowserService } from '../../services/browser.service';
import { NavigationEnd, Router } from '@angular/router';

describe('MainComponent', () => {
  let component: MainComponent;
  let fixture: ComponentFixture<MainComponent>;
  let debugElement: DebugElement;
  let credentials: ICredential[] = [
    {
      'shortKey': 'laborum',
      'longKey': 'APPLICATION.TOOLS.Lorem.laborum',
      'environment': 'TOOLS',
      'component': 'Lorem',
      'region': 'us-east-1',
      'lastUpdatedBy': 'Felecia Cohen',
      'lastUpdatedDate': '2018-04-04T12:51:37.803Z',
    },
    {
      'shortKey': 'mollit',
      'longKey': 'APPLICATION.TOOLS.officia.mollit',
      'environment': 'TOOLS-int',
      'component': 'officia',
      'region': 'us-east-1',
      'lastUpdatedBy': 'Figueroa Robles',
      'lastUpdatedDate': '2018-04-04T12:51:37.803Z',
    },
    {
      'shortKey': 'pariatur',
      'longKey': 'APPLICATION.TOOLS.adipisicing.pariatur',
      'environment': 'TOOLS',
      'component': 'adipisicing',
      'region': 'us-east-1',
      'lastUpdatedBy': 'Caitlin Sellers',
      'lastUpdatedDate': '2018-04-04T12:51:37.803Z',
    },
  ];

  class MockCredentialService {
    getCredentials(selected: Selected): any {
      return Observable.of({secret: false});
    }
  }

  class MockSnackBarService {
    open(): any {
      return Observable.of({secret: false});
    }
  }

  class MockAccountService {
    getAccounts(): any {
      return Observable.of({accounts: true});
    }
  }

  class MockPermissionService {
    getAuthorizations(): any {
      return new Permission();
    }
  }

  class MockUserService {
    getUserRole(): any {
      return {};
    }
  }

  class MockTdDialogService {
    openAlert(): any {
      return true;
    }
    openConfirm(): any {
      return true;
    }
    afterClosed(): any {
      return true;
    }
  }

  class MockBrowserService {
    checkIfIEOrEdge(): boolean {
      return true;
    }
  }

  class MockRouter{
    navigate(route: string): void{}

    public events = Observable.of( new NavigationEnd(0, 'http://localhost:4200/', 'http://localhost:4200/add'));

  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MainComponent, MatSidenav ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      imports: [ HttpClientModule, MatSnackBarModule, MatDialogModule, BrowserAnimationsModule, CovalentMediaModule, MatMenuModule, MatTableModule, MatButtonModule],
      providers: [ {provide: CredentialService, useClass: MockCredentialService },
                    {provide: MatSnackBar, useClass: MockSnackBarService },
                    {provide: TdDialogService, useClass: MockTdDialogService },
                    {provide: AccountService, useClass: MockAccountService },
                    {provide: BrowserService, useClass: MockBrowserService },
                    {provide: Router, useClass: MockRouter },
                    {provide: PermissionService, useClass: MockPermissionService }],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MainComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
    localStorage.removeItem('darkTheme');
    localStorage.removeItem('role');
    component.selected = new Selected();

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it( `should load with darkTheme`, async() => {
    expect(component.darkTheme).toBeTruthy();
  });

  it( `should load with no authorizations`, async() => {
    expect(component.authorizations.createCredential).toBeFalsy();
    expect(component.authorizations.deleteCredential).toBeFalsy();
    expect(component.authorizations.updateCredential).toBeFalsy();
    expect(component.authorizations.viewCredential).toBeFalsy();
    expect(component.authorizations.viewCredentialHistory).toBeFalsy();
    expect(component.authorizations.viewCredentialSecret).toBeFalsy();
  });

  it( `should load with addSideNav opened as false`, async() => {
    // expect(component.addSideNav).toBeFalsy();
  });

  it( `should load with editSideNav opened as false`, async() => {
    // expect(component.editSideNav).toBeFalsy();
  });

  it( `should load with showSideNav opened as false`, async() => {
    // expect(component.showSideNav).toBeFalsy();
  });

  it( `should load with showSideNav opened as false`, async() => {
    // expect(component.showSideNav).toBeFalsy();
  });

  it( `should load with theme from local storage`, async() => {
    localStorage.setItem('darkTheme', 'false');

    fixture = TestBed.createComponent(MainComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(localStorage.getItem('darkTheme')).toEqual('false');
    expect(component.darkTheme).toBeFalsy();

    localStorage.setItem('darkTheme', 'true');

    fixture = TestBed.createComponent(MainComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(localStorage.getItem('darkTheme')).toEqual('true');
    expect(component.darkTheme).toBeTruthy();
  });

  it('should display error on loadUser() if failure to getUserRole() ocurrs', async() => {
    const userService: UserService = fixture.debugElement.injector.get(UserService);
    spyOn(userService, 'getUserRole').and.returnValue(Observable.throw(new Error('Error. Could not retrieve user')));

    component.loadUser();
    fixture.detectChanges();

    expect(component.error).toBeTruthy();

  });

  it( `should save theme to local storage`, async() => {
    component.toggleTheme();
    fixture.detectChanges();

    expect(localStorage.getItem('darkTheme')).toEqual('false');

    component.toggleTheme();
    fixture.detectChanges();

    expect(localStorage.getItem('darkTheme')).toEqual('true');
  });

  xit( `should have empty user memberships`, async() => {

  });

  xit( `should load with theme from local storage`, async() => {
    // fixture.detectChanges();
    expect(component.error).toBeTruthy();
    // expect(component.user).toEqual('fas');
  });

  it(`should display user is authorized if user.role == ops`, async() => {
    const permissionService: PermissionService = fixture.debugElement.injector.get(PermissionService);
    spyOn(permissionService, 'getAuthorizations').and.returnValue(
      {'createCredential': true,
            'deleteCredential': true,
            'updateCredential': true,
            'viewCredentialSecret': true});

    component.user = { name: 'testUser', role: 'ops' , memberships: []};
    component.selected.account.sdlc = 'prod';
    component.selected.account.alias = 'prod';

    component.checkAuthorization();
    fixture.detectChanges();

    expect(component.authorizations.createCredential).toBeTruthy();
    expect(component.authorizations.deleteCredential).toBeTruthy();
    expect(component.authorizations.updateCredential).toBeTruthy();
    expect(component.authorizations.viewCredentialSecret).toBeTruthy();
  });

  it(`should display user is authorized if user.role == dev and account !== prod`, async() => {
    const permissionService: PermissionService = fixture.debugElement.injector.get(PermissionService);
    spyOn(permissionService, 'getAuthorizations').and.returnValue(
      {'createCredential': true,
        'deleteCredential': true,
        'updateCredential': true,
        'viewCredentialSecret': true});
    component.user = { name: 'testUser', role: 'dev' , memberships: []};
    component.selected.account.alias = 'Dev';
    component.selected.account.sdlc = 'dev';

    component.checkAuthorization();
    fixture.detectChanges();
    expect(component.authorizations.createCredential).toBeTruthy();

    component.selected.account.alias = 'Qa';
    component.selected.account.sdlc = 'qa';

    expect(component.authorizations.createCredential).toBeTruthy();
    fixture.detectChanges();
  });

  it(`should display user is !authorized if user.role == dev and account === prod`, async() => {
    component.user = { name: 'testUser', role: 'dev' , memberships: []};
    component.selected.account.alias = 'Prod';

    component.checkAuthorization();
    fixture.detectChanges();

    expect(component.authorizations.createCredential).toBeFalsy();
  });

  it(`should have all sideNavs as false when closeSideNav() is called`, async() => {
    component.isSideNavOpened = true;

    component.closeSideNav();

    expect(component.isSideNavOpened).toBeFalsy();
    expect(component.selectedCredential).toEqual(undefined);
  });

  it(`should mark isSideNavOpened when calling openSideNav`, async() => {
    component.isSideNavOpened = false;
    let result: any = {};
    component.openSideNav(component.selectedCredential, 'false', null);

    expect(component.isSideNavOpened).toBeTruthy();
    expect(component.selectedCredential).toEqual(result);
  });

  it(`should call route with add when calling openSideNav`, async() => {
    const router: Router = fixture.debugElement.injector.get(Router);
    spyOn(component, 'searchCredentials');
    spyOn(router, 'navigate');
    component.isSideNavOpened = false;

    component.openSideNav(component.selectedCredential, 'add', null);

    expect(component.isSideNavOpened).toBeTruthy();
    expect(component.isSideNavCloseDisabled).toBeTruthy();
    expect(router.navigate).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['add']);
  });

  it(`should call route with edit when calling openSideNav`, async() => {
    const router: Router = fixture.debugElement.injector.get(Router);
    spyOn(component, 'searchCredentials');
    spyOn(router, 'navigate');
    component.isSideNavOpened = false;

    component.openSideNav(component.selectedCredential, 'edit', null);

    expect(component.isSideNavOpened).toBeTruthy();
    expect(component.isSideNavCloseDisabled).toBeTruthy();
    expect(router.navigate).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['edit']);
  });

  it(`should call route with view when calling openSideNav`, async() => {
    const router: Router = fixture.debugElement.injector.get(Router);
    spyOn(component, 'searchCredentials');
    spyOn(router, 'navigate');
    component.isSideNavOpened = false;

    component.openSideNav(component.selectedCredential, 'view', 1);

    expect(component.isSideNavOpened).toBeTruthy();
    expect(component.isSideNavCloseDisabled).toBeFalsy();
    expect(router.navigate).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['view', {tab: 1}]);
  });

  it(`should refresh after close if true is passed to closeSideNavAndRefresh`, async() => {
    spyOn(component, 'searchCredentials');
    component.selected.account.alias = 'Dev';
    component.selected.account.sdlc = 'dev';
    component.selected.application = 'APP';
    component.selected.region = 'us-east-2';

    component.closeSideNavAndRefresh(true);

    expect(component.searchCredentials).toHaveBeenCalledTimes(1);
    expect(component.selectedCredential).toEqual(undefined);
  });

  it(`should not refresh after close if false is passed to closeSideNavAndRefresh`, async() => {
    spyOn(component, 'searchCredentials');
    component.selected.account.alias = 'Dev';
    component.selected.account.sdlc = 'dev';
    component.selected.application = 'APP';
    component.selected.region = 'us-east-2';

    component.closeSideNavAndRefresh(false);
    fixture.detectChanges();

    expect(component.searchCredentials).toHaveBeenCalledTimes(0);
    expect(component.selectedCredential).toEqual(undefined);
  });

  it(`should call credentialService with Selected`, async() => {
    component.user = { name: 'testUser', role: 'dev' , memberships: ['APP']};
    component.selected.account.alias = 'Prod';
    component.selected.application = 'APP';
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'getCredentials').and.returnValue(Observable.of(['test']));

    component.searchCredentials(component.selected);
    fixture.detectChanges();

    expect(credentialService.getCredentials).toHaveBeenCalled();
  });

  it(`should not call credentialService with Selected when invalid credential`, async() => {
    component.user = { name: 'testUser', role: 'dev' , memberships: ['APP']};
    component.selected.account.alias = 'Prod';
    component.selected.application = '';
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'getCredentials').and.returnValue(Observable.of(['test']));

    component.searchCredentials(component.selected);
    fixture.detectChanges();

    expect(credentialService.getCredentials).not.toHaveBeenCalled();
  });

  it(`should assign credentials on getCredentials()`, async() => {
    component.user = { name: 'testUser', role: 'dev' , memberships: ['APP']};
    component.selected.account.alias = 'Prod';
    component.selected.application = 'APP';
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'getCredentials').and.returnValue(Observable.of(['test']));

    component.searchCredentials(component.selected);
    fixture.detectChanges();

    expect(component.dataSource.data).toEqual(['test']);
    expect(component.loading).toBeFalsy();
    expect(component.loaded).toBeTruthy();
  });

  it(`should clear credentials if none returned on SearchCredentials()`, async() => {
    component.user = { name: 'testUser', role: 'dev' , memberships: ['APP']};
    component.selected.account.alias = 'Prod';
    component.selected.application = 'APP';
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'getCredentials').and.returnValue(Observable.throw(new Error('Error. Not Authorized')));

    component.searchCredentials(component.selected);
    fixture.detectChanges();

    expect(component.loading).toBeFalsy();
    expect(component.loaded).toBeTruthy();
    expect(component.dataSource.source).toEqual(undefined);
  });

  it(`should open confirm delete dialog`, async() => {
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(dialogService, 'openConfirm');

    component.confirmDelete(new Credential());
    fixture.detectChanges();

    expect(dialogService.openConfirm).toHaveBeenCalled();
  });


  it(`should display snackbar after successful delete`, async() => {
    component.user = { name: 'testUser', role: 'dev' , memberships: []};
    component.selected.account.alias = 'Prod';
    const longKey: string = 'application.component.environment.key';
    let credential: Credential = new Credential();
    credential.longKey = longKey;
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const snackBarService: MatSnackBar = fixture.debugElement.injector.get(MatSnackBar);
    spyOn(credentialService, 'deleteCredential').and.returnValue(Observable.of(['deleted']));
    spyOn(snackBarService, 'open').and.returnValue(Observable.of(['opened']));

    component.confirmDelete(credential);
    fixture.detectChanges();

    expect(snackBarService.open).toHaveBeenCalledWith( 'Credential application.component.environment.key deleted','', Object({ duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom' }) );
    expect(credentialService.deleteCredential).toHaveBeenCalled();
  });

  it(`should display error after unsuccessful delete`, async() => {
    component.user = { name: 'testUser', role: 'dev' , memberships: []};
    component.selected.account.alias = 'Prod';
    let credential: Credential = new Credential();
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(dialogService, 'openAlert');
    spyOn(credentialService, 'deleteCredential').and.returnValue(Observable.throw(new Error('Error. Not Authorized')));

    component.confirmDelete(credential);
    fixture.detectChanges();

    expect(credentialService.deleteCredential).toHaveBeenCalled();
    expect(dialogService.openAlert).toHaveBeenCalledWith( Object({ message: 'Error. Not Authorized', title: 'undefined Error', closeButton: 'Ok' }));
  });

  it(`getUniqueEnvironments should only return unique environments from credentials`, async() => {
    let environments: string[] = component.getUniqueEnvironments(credentials);

    expect(environments.length).toEqual(3);
    expect(environments).toContain('ALL');
  });

  it(`should filter dataSource.data when applyEnvironmentFilter is called`, async() => {
    component.credentials = credentials;
    component.dataSource.data = credentials;

    component.applyEnvironmentFilter('tools');

    expect(component.dataSource.data.length).toEqual(2);
    expect(component.dataSource.data.length).not.toEqual(3);
  });

  it(`should unfilter dataSource.data when applyEnvironmentFilter is called with ALL`, async() => {
    component.credentials = credentials;
    component.dataSource.data = credentials;
    component.applyEnvironmentFilter('tools');

    component.applyEnvironmentFilter('all');

    expect(component.dataSource.data.length).toEqual(3);
    expect(component.dataSource.data.length).not.toEqual(2);
  });

  it(`should return true if addSideNav or editSidenNav are opened when calling isCloseSideNavDisabled()`, async() => {
    // component.addSideNav = true;

    // let result: boolean = component.isCloseSideNavDisabled();

    // expect(result).toEqual(true);

    // component.editSideNav = true;

    // result = component.isCloseSideNavDisabled();

    // expect(result).toEqual(true);
  });

  xit(`should not display side nav when closeSideNav() is called`, async() => {
    let element: DebugElement = debugElement.query(By.css('mat-sidenav'));
    // component.openAddCredential();

    fixture.detectChanges();
    console.log(element.nativeElement);
    expect(component.sideNav.opened).toEqual(false);
    //
    // component.closeSideNav();
    //
    // expect(component.addSideNav).toBeFalsy();
  });

  it(`should call getAuthorizations whenever searchCredentials is called`, async() => {
    let selected: Selected = new Selected();
    selected.account = new Account();
    selected.account.alias = 'test_awsdev';
    selected.region = 'us-east-1';
    selected.environment = 'dev';
    selected.application = 'APP';

    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    spyOn(credentialService, 'getCredentials').and.returnValue({});
    component.searchCredentials(selected);

    expect(component.checkAuthorization()).toHaveBeenCalled();
  });

  it(`should display 403 error if 403 error received calling getCredentials()`, async() => {
    let selected: Selected = new Selected();
    selected.account = new Account();
    selected.account.alias = 'test_awsdev';
    selected.region = 'us-east-1';
    selected.environment = 'dev';
    selected.application = 'APP';
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'getCredentials').and.returnValue(
      Observable.throw(new HttpErrorResponse(
        {error: {message: 'The current user has insufficient permissions to perform this action.', error: 'FORBIDDEN'},
          status: 403})));
    spyOn(dialogService, 'openAlert');

    component.searchCredentials(selected);
    fixture.detectChanges();

    expect(dialogService.openAlert).toHaveBeenCalledWith({message: 'The current user has insufficient permissions to perform this action.',
      title: 'Error: 403 FORBIDDEN', closeButton: 'Ok'});
  });

  it(`should display Throttling error if 408 error calling getCredentials() `, async() => {
    let selected: Selected = new Selected();
    selected.account = new Account();
    selected.account.alias = 'test_awsdev';
    selected.region = 'us-east-1';
    selected.environment = 'dev';
    selected.application = 'APP';
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'getCredentials').and.returnValue(
      Observable.throw(new HttpErrorResponse(
        {error: {message: 'Request throttling encountered. Please try again later.', error: 'REQUEST TIMEOUT'}, status: 408})));
    spyOn(dialogService, 'openAlert');

    component.searchCredentials(selected);
    fixture.detectChanges();

    expect(dialogService.openAlert).toHaveBeenCalledWith({
      message: 'Request throttling encountered. Please try again later.',
      title: 'Error: 408 REQUEST TIMEOUT', closeButton: 'Ok'});
  });

  it(`should display Table Not Found error if 404 error received calling getCredentials()`, async() => {
    let selected: Selected = new Selected();
    selected.account = new Account();
    selected.account.alias = 'test_awsdev';
    selected.region = 'us-east-1';
    selected.environment = 'dev';
    selected.application = 'APP';
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: TdDialogService = fixture.debugElement.injector.get(TdDialogService);
    spyOn(credentialService, 'getCredentials').and.returnValue(
      Observable.throw(new HttpErrorResponse(
        {error: {message: 'A credential table cannot be found or does not exist on account: test_awsdev in region: us-east-1',
          error: 'NOT FOUND'}, status: 404})));
    spyOn(dialogService, 'openAlert');

    component.searchCredentials(selected);
    fixture.detectChanges();

    expect(dialogService.openAlert).toHaveBeenCalledWith(
      {message: 'A credential table cannot be found or does not exist on account: test_awsdev in region: us-east-1',
      title: 'Error: 404 NOT FOUND', closeButton: 'Ok'});
  });

});
