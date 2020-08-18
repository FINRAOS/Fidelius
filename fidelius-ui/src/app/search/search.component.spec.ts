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

import { SearchComponent } from './search.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatInputModule, MatSelectModule } from '@angular/material';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Selected } from '../../services/credential.service';
import { Account } from '../../services/account.service';
import { MainComponent } from '../main/main.component';
import { GroupByPipe } from '../pipes/group-by.pipe';

describe('SearchComponent', () => {
  let component: SearchComponent;
  let fixture: ComponentFixture<SearchComponent>;

  class MockMainComponent {
    selected = {
      "application": "TESTAPP",
      "region": "east",
      "account": {
        "alias": "Prod",
        "sdlc": "prod"
      }
    };

    searchCredentials(selected: Selected): void {
    }

    applyEnvironmentFilter(environment: string): void {
    }

    applyFilter(filter: string): void{}
  }


  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SearchComponent, GroupByPipe],
      schemas:[ CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        {provide: MainComponent, useClass: MockMainComponent },
      ],
      imports: [FormsModule, ReactiveFormsModule, MatAutocompleteModule, MatSelectModule, MatInputModule, BrowserAnimationsModule],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchComponent);
    component = fixture.componentInstance;

    component.applicationList = ['APP'];
    component.accounts = [];
    localStorage.clear();

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load previous Selected from localStorage', async() => {
    let account: Account = new Account();
    account.alias = 'Prod';
    account.regions = [{ name: 'east-2'}];
    component.accounts = [account];
    localStorage.setItem('account', 'Prod');
    localStorage.setItem('region', 'east-2');
    localStorage.setItem('application', 'APP');

    fixture.detectChanges();
    component.loadFromLocalStorage();

    expect(component.selected.account.alias).toEqual('Prod');
    expect(component.selected.region).toEqual('east-2');
    expect(component.selected.application).toEqual('APP');

  });

  it('should not load region from local storage that is not in the within region list', async() => {
    let account: Account = new Account();
    account.alias = 'Prod';
    account.regions = [{ name: 'east-2'}];
    component.accounts = [account];
    localStorage.setItem('region', 'east-5');
    fixture.detectChanges();

    component.loadFromLocalStorage();

    expect(component.selected.region).toEqual(undefined);
  });

  it('should not load account from local storage that is not in the within account list', async() => {
    let account: Account = new Account();
    account.alias = 'Prod';
    account.regions = [{ name: 'east-2'}];
    component.accounts = [account];
    localStorage.setItem('account', 'Tools');
    fixture.detectChanges();

    component.loadFromLocalStorage();

    expect(component.selected.region).toEqual(undefined);
  });

  it('should not load app from local storage that is not in the within app list', async() => {
    let account: Account = new Account();
    account.alias = 'Prod';
    account.regions = [{ name: 'east-2'}];
    component.accounts = [account];
    component.applicationList = ['APP'];
    localStorage.setItem('application', 'CREDSTSH');
    fixture.detectChanges();

    component.loadFromLocalStorage();

    expect(component.selected.application).toEqual('');
  });

  it('should save Selected when searchCredentials() is called', async() => {
    let account: Account = new Account();
    account.alias = 'Qa';
    account.regions = [{ name: 'west-1'}];
    component.accounts = [account];
    component.selected.account.alias = 'Qa';
    component.accountControl.setValue('Qa');
    component.selected.region = 'west-1';
    component.selected.application = 'APP';

    fixture.detectChanges();

    component.searchCredentials();

    expect(localStorage.getItem('account')).toEqual('Qa');
    expect(localStorage.getItem('region')).toEqual('west-1');
    expect(localStorage.getItem('application')).toEqual('APP');
  });

  it('should emit Selected if valid account, app, and region', function(done: any) {
    const parentComponent: MainComponent = fixture.debugElement.injector.get(MainComponent);
    spyOn(parentComponent, 'searchCredentials');
    component.selected.account.alias = 'Qa';
    component.selected.region = 'west-1';
    component.selected.application = 'APP';
    let expected: Selected = new Selected();
    expected.account.alias = 'Qa';
    expected.region = 'west-1';
    expected.application = 'APP';

    component.searchCredentials();

    fixture.detectChanges();

    setTimeout(function() {
      expect(parentComponent.searchCredentials).toHaveBeenCalledTimes(1);
      expect(parentComponent.searchCredentials).toHaveBeenCalledWith(expected);
      done();
    }, 0);

  });

  it('should emit Selected environment', async() => {
    const parentComponent: MainComponent = fixture.debugElement.injector.get(MainComponent);
    spyOn(parentComponent, 'applyEnvironmentFilter');
    let search: string = 'SPYder';
    component.applyEnvironment(search);

    fixture.detectChanges();

    expect(parentComponent.applyEnvironmentFilter).toHaveBeenCalledTimes(1);
    expect(parentComponent.applyEnvironmentFilter).toHaveBeenCalledWith('spyder');
  });

  it('should emit Selected key or component searched', async() => {
    const parentComponent: MainComponent = fixture.debugElement.injector.get(MainComponent);
    spyOn(parentComponent, 'applyFilter');
    let environment: string = 'PROD-INT';
    component.applyFilter(environment);

    fixture.detectChanges();

    expect(parentComponent.applyFilter).toHaveBeenCalledTimes(1);
    expect(parentComponent.applyFilter).toHaveBeenCalledWith('prod-int');
  });

  it('should load for localStorage on ngOnChanges', async() => {
    spyOn(component, 'loadFromLocalStorage');

    component.ngOnChanges({
      accounts: []
    });
    fixture.detectChanges();

    expect(component.loadFromLocalStorage()).toHaveBeenCalled();
  });

  it('should set application value on auto-complete', async() => {
    spyOn(component, 'searchCredentials');
    let event: any = {'option': {'value': 'APP'}};

    component.applicationHandler(event);
    fixture.detectChanges();

    expect(component.searchCredentials).toHaveBeenCalledTimes(1);
    expect(component.selected.application).toEqual('APP');
  });

  it('should set account value on auto-complete when region does not exist', async() => {
    spyOn(component, 'searchCredentials');
    let account: Account = new Account();
    account.alias = 'Prod';
    account.regions = [{ name: 'east-2'}];
    let event: any = {'option': {'value': account}};

    component.accountHandler(event);
    fixture.detectChanges();

    expect(component.searchCredentials).toHaveBeenCalledTimes(1);
    expect(component.selected.account.alias).toEqual('Prod');
    expect(component.selected.region).toEqual(undefined);
  });

  it('should set account value on auto-complete when region exists', async() => {
    spyOn(component, 'searchCredentials');
    let account: Account = new Account();
    account.alias = 'Prod';
    account.regions = [{ name: 'east-2'}];
    let event: any = {'option': {'value': account}};
    component.selected.region = 'east-2';

    component.accountHandler(event);
    fixture.detectChanges();

    expect(component.searchCredentials).toHaveBeenCalledTimes(1);
    expect(component.selected.account.alias).toEqual('Prod');
    expect(component.selected.region).toEqual('east-2');
  });


});
