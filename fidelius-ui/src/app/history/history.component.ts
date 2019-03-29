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
  AfterViewInit, ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges,
  ViewChild,
  HostBinding,
} from '@angular/core';
import { NgForm } from '@angular/forms';
import { MatInput, MatTableDataSource, MatPaginator, MatSort, MatSortable } from '@angular/material';
import { Credential, ICredential, CredentialService, Selected, History, IHistory } from '../../services/credential.service';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'fidelius-history',
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.scss'],
  providers: [CredentialService, AlertService],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistoryComponent implements OnInit {
  @Input() selected: Selected = new Selected();
  @Input() credential: Credential = new Credential();
  @ViewChild(NgForm) showForm: NgForm;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  loading: boolean = false;
  dataSource: any = undefined;
  displayedColumns: any = ['revision', 'updatedBy', 'updatedDate'];

  constructor( private _credentialService: CredentialService,
               private _alertService: AlertService) {
  }

  ngOnInit(): void {
    this.credential.application = this.selected.application;
    this.credential.account = this.selected.account.alias;
    this.credential.region = this.selected.region;
    this.sort.sort(<MatSortable>{
      id: 'revision',
      start: 'desc',
    });
    this.dataSource = new MatTableDataSource<IHistory[]>();
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    this.getHistory();
  }

  getHistory(): void {
    this.loading = true;
    this._credentialService.getCredentialHistory(this.credential).subscribe( (history: IHistory[]) => {
      this.loading = false;
      this.dataSource.data = history;
      this.dataSource.paginator = this.paginator;
      this.dataSource.sort = this.sort;
    }, (error: any) => {
      this._alertService.openAlert(error);
    });
  }

}
