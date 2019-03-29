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
  Component, EventEmitter, Input, OnChanges, OnInit, Output, ViewChild, AfterViewInit, OnDestroy,
  ChangeDetectionStrategy, ChangeDetectorRef
} from '@angular/core';
import { FormControl, NgForm } from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import { startWith } from 'rxjs/operators/startWith';
import { map } from 'rxjs/operators/map';
import { Selected } from '../../services/credential.service';
import { Account } from '../../services/account.service';
import { APPLICATION_LIST_LABEL_NAME } from  '../../config/permissions';
import { MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material';
import { Subscription } from 'rxjs/Subscription';
import { MainComponent } from '../main/main.component';

@Component({
  selector: 'fidelius-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss'],
})
export class SearchComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {
  @Input() selected: Selected = new Selected();
  @Input() agsList: string[] = [];
  @Input() accounts: Account[] = [];
  @Input() environments: string[] = [];
  @Input() disableAnimations: boolean;
  applicationListName: string = APPLICATION_LIST_LABEL_NAME;
  applicationOptions: Observable<string[] | null>;
  applicationControl: FormControl =  new FormControl();
  subscription: Subscription;
  previousSelected: Selected = new Selected();
  @ViewChild(MatAutocompleteTrigger) trigger: MatAutocompleteTrigger;

  constructor(private _changeDetectorRef: ChangeDetectorRef,
              private _parentComponent: MainComponent) {
  }

  ngOnInit(): void {
    this.applicationOptions = this.applicationControl.valueChanges
      .pipe(
        startWith(''),
        map((val: string) => this.filter(val)),
      );
    this.loadFromLocalStorage();
  }

  ngAfterViewInit(): void {
    this._subscribeToClosingActions();
  }

  ngOnDestroy(): void {
    if (this.subscription && !this.subscription.closed) {
      this.subscription.unsubscribe();
    }
  }

  ngOnChanges(changes: any): void {
    if (changes.accounts) {
      this.loadFromLocalStorage();
    }
  }

  filter(val: string): string[] {
    return this.agsList.filter((option: any) =>
      option.toString().toLowerCase().indexOf(val.toString().toLowerCase()) === 0);
  }

  searchCredentials(): void {
      if (this.selected.account && this.selected.region && (this.selected.application && this.agsList.includes(this.selected.application))) {
        localStorage.setItem('account', this.selected.account.alias);
        localStorage.setItem('region', this.selected.region);
        localStorage.setItem('application', this.selected.application);
      } else {
        this.selected.application = '';
      }

      if (this.selected.application !== this.previousSelected.application ||
          this.selected.region !== this.previousSelected.region ||
          this.selected.account !== this.previousSelected.account) {
        this._parentComponent.searchCredentials(this.selected);
        this.previousSelected = Object.assign({}, this.selected);
      }
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
    }
    let region: any = this.selected.account.regions.find( x => x.name  === localStorage.getItem('region'));
    if (!region) {
      this.selected.region = undefined;
    } else {
      this.selected.region = region.name;
    }
    if ( this.agsList.includes( localStorage.getItem('application') ) ) {
      this.selected.application = localStorage.getItem('application');
      this.applicationControl.setValue(this.selected.application);
    }
    this.searchCredentials();
  }

  handler(event: MatAutocompleteSelectedEvent): void {
    this.selected.application = event.option.value;
    this.applicationControl.setValue(event.option.value);
    this.searchCredentials();
  }

  private _subscribeToClosingActions(): void {
    if (this.subscription && !this.subscription.closed) {
      this.subscription.unsubscribe();
    }

    this.subscription = this.trigger.panelClosingActions
      .subscribe((e: any) => {
          if (!e || !e.source) {
            if (this.applicationControl.value  && this.agsList.includes(this.applicationControl.value) ) {
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
