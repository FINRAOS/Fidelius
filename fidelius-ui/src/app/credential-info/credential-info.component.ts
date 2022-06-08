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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { Selected, Credential, CredentialService } from '../../services/credential.service';
import { Permission } from '../../services/permission.service';
import { MainComponent } from '../main/main.component';
import { ActivatedRoute } from '@angular/router';
import { BrowserService } from '../../services/browser.service';
import { AlertService } from '../../services/alert.service';
import { MatTabGroup } from '@angular/material/tabs';
import { HistoryComponent } from '../history/history.component';

@Component({
  selector: 'fidelius-credential-info',
  templateUrl: './credential-info.component.html',
  styleUrls: ['./credential-info.component.scss'],
  providers: [],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CredentialInfoComponent implements OnInit {
  selected: Selected = new Selected();
  credential: Credential = new Credential();
  selectedTab: number = 0;
  authorizations: Permission = new Permission();
  isIEOrEdge: boolean;
  isLoading: boolean = true;
  hasError: boolean = false;
  @ViewChild('tabGroup') tabGroup: MatTabGroup;
  @ViewChild(HistoryComponent) history: HistoryComponent;

  constructor(private _parentComponent: MainComponent,
              private _credentialService: CredentialService,
              private _alertService: AlertService,
              private _route: ActivatedRoute,
              private _changeDetectorRef: ChangeDetectorRef,
              private _browserService: BrowserService) {
  }

  ngOnInit(): void {
    this.selected =  Object.assign({}, this._parentComponent.selected);
    this.credential.account = this._parentComponent.selected.account.alias;
    this.credential.application = this._parentComponent.selected.application;
    this.credential.region = this._parentComponent.selected.region;
    this.credential.environment = this._parentComponent.selectedCredential.environment;
    this.credential.component = this._parentComponent.selectedCredential.component;
    this.credential.shortKey = this._parentComponent.selectedCredential.shortKey;
    this.credential.longKey = this._parentComponent.selectedCredential.longKey;
    this.authorizations = this._parentComponent.authorizations;
    this.selectedTab = this._route.snapshot.params.tab;
    this.isIEOrEdge = this._browserService.checkIfIEOrEdge();
    if(this.selected.application !== "") {
      this.loadCredential();
    }
  }

  loadCredential(): void {
    this._credentialService.getCredential(this.selected, this.credential).subscribe((credential: Credential) => {
      this.credential = credential;
      this.isLoading = false;
      this._changeDetectorRef.detectChanges();
    }, (error: any) => {
      this.isLoading = false;
      this.hasError = true;
      this._alertService.openAlert(error);
      this._changeDetectorRef.detectChanges();
    });
  }

  loadHistory(): void {
    this.history.getHistory()
  }

  closeSideNav(refresh: boolean): void {
    this._parentComponent.closeSideNavAndRefresh(refresh);
    this._changeDetectorRef.detectChanges();
  }

}
