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
  HostBinding, ChangeDetectorRef,
} from '@angular/core';
import { NgForm } from '@angular/forms';
import { MatInput, MatTableDataSource, MatPaginator, MatSort, MatSortable } from '@angular/material';
import { Credential, ICredential, CredentialService, Selected, History, IHistory } from '../../services/credential.service';
import { AlertService } from '../../services/alert.service';
import { animate, state, style, transition, trigger } from '@angular/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ClipboardService } from 'ngx-clipboard';


@Component({
  selector: 'fidelius-history',
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.scss'],
  providers: [CredentialService, AlertService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('detailExpand', [
      state('collapsed, void', style({height: '0px', minHeight: '0', display: 'none'})),
      state('expanded', style({height: '150px'})),
      state('copying', style({height: '20px'})),
      transition('* <=> *', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ],
})
export class HistoryComponent implements OnInit {
  @Input() selected: Selected = new Selected();
  @Input() credential: Credential = new Credential();
  @Input() enableView: boolean;
  @ViewChild(NgForm) showForm: NgForm;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  loading: boolean = false;
  dataSource: any = undefined;
  displayedColumns: any = ['revision', 'updatedBy', 'updatedDate'];

  constructor( private _credentialService: CredentialService,
               private _alertService: AlertService,
               private _snackBarService: MatSnackBar,
               private _clipboardService: ClipboardService,
               private _changeDetector: ChangeDetectorRef) {
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
      this.sort.sortChange.subscribe(() => {
        if (this.dataSource && this.dataSource.data) {
          this.dataSource.data.forEach(element => {
            element.expanded = false;
            element.showSecret = false;
          });
        }
        this._changeDetector.detectChanges();
      });
    }, (error: any) => {
      this._alertService.openAlert(error);
    });
  }
  async toggleView(element: any): Promise<void> {
    element.expanded = !element.expanded;
    element.showSecret = !element.showSecret;
    if (element.expanded) {
      element.loading = true;
      this._changeDetector.detectChanges();
      if (!element.secret) {
        element.secret = await this.decryptSecret(element);
      }
      element.loading = false;
      this._changeDetector.detectChanges();
    } else {
      element.secret = undefined;
    }
  }

  async decryptSecret(element: any): Promise<string> {
    const { secret } = await this._credentialService.getSecret(this.credential, element.revision).toPromise();
    return secret;
  }

  async copySecret(element: any): Promise<void> {
    if (!element.secret) {
      element.loading = true;
      element.expanded = true;
      this._changeDetector.detectChanges();
      element.secret = await this.decryptSecret(element);
      element.loading = false;
      element.expanded = false;
      this._changeDetector.detectChanges();
    }
    this._clipboardService.copyFromContent(element.secret);
    this._snackBarService.open('Copied to clipboard', '',  {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
      panelClass: 'snackbar-success'
    });

  }


}
