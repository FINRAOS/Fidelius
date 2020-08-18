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

import {
  Component, Input, OnChanges, OnInit, AfterViewInit, OnDestroy, ChangeDetectorRef, ViewChildren, QueryList
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import { startWith } from 'rxjs/operators/startWith';
import { map } from 'rxjs/operators/map';
import { Selected } from '../../services/credential.service';
import { Account } from '../../services/account.service';
import { APPLICATION_LIST_LABEL_NAME } from  '../../config/permissions';
import {  MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material';
import { Subscription } from 'rxjs/Subscription';
import { MainComponent } from '../main/main.component';
import 'rxjs/add/observable/of';

@Component({
  selector: 'fidelius-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss'],
})
export class SearchComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {
  @Input() selected: Selected = new Selected();
  @Input() applicationList: string[] = [];
  @Input() accounts: Account[] = [];
  @Input() environments: string[] = [];
  @Input() disableAnimations: boolean;
  applicationListName: string = APPLICATION_LIST_LABEL_NAME;
  applicationOptions: Observable<string[] | null>;
  accountOptions: Observable<Account[] | null>;
  applicationControl: FormControl =  new FormControl();
  accountControl: FormControl =  new FormControl();
  subscription: Subscription;
  accountSubscription: Subscription;
  previousSelected: Selected = new Selected();

  @ViewChildren(MatAutocompleteTrigger) trigger: QueryList<MatAutocompleteTrigger>;

  constructor(private _changeDetectorRef: ChangeDetectorRef,
              private _parentComponent: MainComponent) {
  }

  ngOnInit(): void {
    this.applicationOptions = this.applicationControl.valueChanges
      .pipe(
        startWith(''),
        map((val: string) => this.filter(val)),
      );

    this.accountOptions = this.accountControl.valueChanges
      .pipe(
        startWith(''),
        map((val: string) => this.accountFilter(val)),
      );

    this.loadFromLocalStorage();
  }

  ngAfterViewInit(): void {
    this._subscribeToAccountClosingActions();
    this._subscribeToClosingActions();
  }

  ngOnDestroy(): void {
    if (this.subscription && !this.subscription.closed) {
      this.subscription.unsubscribe();
    }

    if (this.accountSubscription && !this.accountSubscription.closed){
      this.accountSubscription.unsubscribe();
    }
  }

  ngOnChanges(changes: any): void {
    if (changes.accounts) {
      this.loadFromLocalStorage();
    }
  }

  filter(val: string): string[] {
    return this.applicationList.filter((option: any) =>
      option.toString().toLowerCase().indexOf(val.toString().toLowerCase()) === 0);
  }

  accountFilter(val: any): Account[] {
    let search: string = typeof(val) === 'string' ? val.toLowerCase() : val.alias.toString().toLowerCase();
    return this.accounts.filter((option: any) =>
      option.alias.toString().toLowerCase().includes(search));
  }

  searchCredentials(): void {

    if (this.accounts.find(x => x.alias === this.accountControl.value)) {
      localStorage.setItem('account', this.selected.account.alias);
    } else {
      this.accountControl.setValue('');
    }

    if (this.selected.account && this.selected.region ) {
      localStorage.setItem('region', this.selected.region);
    }

    if (this.applicationList.includes(this.selected.application)){
      localStorage.setItem('application', this.selected.application);
    } else {
      this.selected.application = '';
    }

    if (this.selected.application !== this.previousSelected.application ||
        this.selected.region !== this.previousSelected.region ||
        this.selected.account !== this.previousSelected.account) {
        this._parentComponent.searchCredentials(this.selected);
    }
    this.previousSelected = Object.assign({}, this.selected);
  }

  applyFilter(filterValue: string): void {
    filterValue = filterValue.trim();
    filterValue = filterValue.toLowerCase();
    this._parentComponent.applyFilter(filterValue);

  }

  applyEnvironment(environment: string): void {
      environment = environment.trim();
      environment = environment.toLowerCase();
      this._parentComponent.applyEnvironmentFilter(environment);
  }

  loadFromLocalStorage(): void {
    this.selected.account = this.accounts.find(x => x.alias === localStorage.getItem('account'));

    if (!this.selected.account) {
      this.selected.account = new Account();
    }else {
      this.accountControl.setValue(this.selected.account.alias);
    }

    let region: any = this.selected.account.regions.length > 0 ? this.selected.account.regions.find( x => x.name  === localStorage.getItem('region')) : undefined;
    if (!region) {
      this.selected.region = undefined;
    } else {
      this.selected.region = region.name;
    }
    if ( this.applicationList.includes( localStorage.getItem('application') ) ) {
      this.selected.application = localStorage.getItem('application');
      this.applicationControl.setValue(this.selected.application);
    }
    this.searchCredentials();
  }

  accountHandler(event: MatAutocompleteSelectedEvent): void {
    this.selected.account = event.option.value;
    this.accountControl.setValue(event.option.value.alias);

    let region: any = this.selected.account.regions.find( x => x.name  === this.selected.region);
    if (!region) {
      this.selected.region = undefined;
    } else {
      this.selected.region = region.name;
    }

    this.searchCredentials();
  }

  applicationHandler(event: MatAutocompleteSelectedEvent): void {
    this.selected.application = event.option.value;
    this.searchCredentials();
  }

  private _subscribeToAccountClosingActions(): void {
    if (this.accountSubscription && !this.accountSubscription.closed) {
      this.accountSubscription.unsubscribe();
    }

    this.accountSubscription = this.trigger.first.panelClosingActions
      .subscribe((e: any) => {
          if (!e || !e.source) {
            if (this.accountControl.value && this.accounts.find(x => x.alias === this.accountControl.value)) {
              this.selected.account = this.accounts.find(x => x.alias === this.accountControl.value);
              let region: any = this.selected.account !== null  ? this.selected.account.regions.find( x => x.name  === this.selected.region) : undefined;
              if (!region) {
                this.selected.region = undefined;
              } else {
                this.selected.region = region.name;
              }

              this.searchCredentials();
            } else {
              this.accountControl.setErrors({'invalidAccount': true});
              this.searchCredentials();
            }
          }
        },
        (err: any) => this._subscribeToAccountClosingActions(),
        () => this._subscribeToAccountClosingActions());
  }

  private _subscribeToClosingActions(): void {
    if (this.subscription && !this.subscription.closed) {
      this.subscription.unsubscribe();
    }

    this.subscription = this.trigger.last.panelClosingActions
      .subscribe((e: any) => {
          if (!e || !e.source) {
            if (this.applicationControl.value  && this.applicationList.includes(this.applicationControl.value) ) {
              this.selected.application = this.applicationControl.value;
              this.searchCredentials();
            } else {
              this.applicationControl.setErrors({'invalidApplication': true});
              this.selected.application = undefined;
              this.searchCredentials();
            }
          }
        },
        (err: any) => this._subscribeToClosingActions(),
        () => this._subscribeToClosingActions());
  }


}
