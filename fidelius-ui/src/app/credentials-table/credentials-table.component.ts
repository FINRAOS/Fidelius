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
  AfterViewInit, ChangeDetectorRef, Component, Input, OnInit,
  ViewChild, OnChanges, ChangeDetectionStrategy,
} from '@angular/core';
import { MatPaginator, MatSort, MatTableDataSource } from '@angular/material';
import { Credential, ICredential } from '../../services/credential.service';
import { Permission } from '../../services/permission.service';
import { MainComponent } from '../main/main.component';

@Component({
  selector: 'fidelius-credentials-table',
  templateUrl: './credentials-table.component.html',
  styleUrls: ['./credentials-table.component.scss'],
})
export class CredentialsTableComponent implements OnInit, OnChanges {
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  @Input() dataSource: MatTableDataSource<ICredential[]>;
  @Input() authorizations: Permission = new Permission();

  displayedColumns: any = ['shortKey', 'environment', 'component', 'lastUpdatedBy', 'lastUpdatedDate', 'Actions'];

  constructor( private _changeDetectorRef: ChangeDetectorRef,
               private _parentComponent: MainComponent) {
  }

  ngOnInit(): void {
    this.dataSource = this._parentComponent.dataSource;
    this.authorizations = this._parentComponent.authorizations;
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    this.dataSource.sortingDataAccessor = (data: any, sortHeaderId: string): string => {
      if (typeof data[sortHeaderId] === 'string') {
        return data[sortHeaderId].toLocaleLowerCase();
      }

      return data[sortHeaderId];
    };
  }

  ngOnChanges(): void {
    this.dataSource = this._parentComponent.dataSource;
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    this.dataSource.sortingDataAccessor = (data: any, sortHeaderId: string): string => {
      if (typeof data[sortHeaderId] === 'string') {
        return data[sortHeaderId].toLocaleLowerCase();
      }

      return data[sortHeaderId];
    };
    this.dataSource.paginator.firstPage();
  }

  openSideNav(credential: ICredential, route: string, tab: number): void{
    this._parentComponent.openSideNav(credential, route, tab);
  }

  openDelete(credential: Credential): void {
    this._parentComponent.confirmDelete(credential);
  }

  trackByCredential(index: number, item: Credential): string{
    return item.longKey;
  }

}
