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
  AfterViewInit, Component, OnInit, ViewChild, OnDestroy, ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import {
  MatDialog,
  MatDialogConfig,
  MatPaginator, MatSidenav, MatSnackBar, MatSort, MatTableDataSource
} from '@angular/material';
import { TdCollapseAnimation, TdDialogService, TdFadeInOutAnimation } from '@covalent/core';
import { IUser, UserService } from '../../services/user.service';
import { Credential, CredentialService, ICredential, Selected } from '../../services/credential.service';
import { isBoolean } from 'util';
import { Account, AccountService } from '../../services/account.service';
import { Permission, PermissionService } from '../../services/permission.service';
import { APPLICATION_LIST_LABEL_NAME } from '../../config/permissions';
import { AlertService } from '../../services/alert.service';
import { OverlayContainer } from '@angular/cdk/overlay';
import { HeartbeatService } from '../../services/heartbeat.service';
import { Observable } from 'rxjs/Observable';
import { HEARTBEAT_IN_MS } from '../../config/application';
import { TimerObservable } from 'rxjs/observable/TimerObservable';
import { Subscription } from 'rxjs/Subscription';
import { ChildActivationEnd, GuardsCheckEnd, NavigationEnd, NavigationStart, Router } from '@angular/router';
import { BrowserService } from '../../services/browser.service';
import { DeleteDialogComponent, IDialogResponse } from './delete-dialog/delete-dialog.component';

@Component({
  selector: 'fidelius-main',
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.scss'],
  animations: [TdFadeInOutAnimation(), TdCollapseAnimation()],
  providers: [UserService, CredentialService, AlertService, HeartbeatService],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainComponent implements OnInit, OnDestroy {
  darkTheme: boolean = true;
  loaded: boolean = false;
  loading: boolean = false;
  error: boolean = false;
  isSideNavOpened: boolean = false;
  isIEOrEdge: boolean = false;
  isSideNavCloseDisabled: boolean = false;
  selected: Selected = new Selected();
  selectedCredential: ICredential;
  authorizations: Permission = new Permission();
  accounts: Account[] = [];
  environments: string[] = [];
  credentials: ICredential[];
  allCredentials: ICredential[];
  user: IUser;
  heartbeatSub: Subscription = undefined;
  sideNavSubscription: Subscription = undefined;
  dataSource: any = new MatTableDataSource<ICredential[]>();
  APPLICATION_LIST_LABEL_NAME: string = APPLICATION_LIST_LABEL_NAME;

  @ViewChild(MatSidenav) sideNav: MatSidenav;

  constructor(private _userService: UserService,
              private _credentialService: CredentialService,
              private _accountService: AccountService,
              private _permissionService: PermissionService,
              private _snackBarService: MatSnackBar,
              private _dialog: MatDialog,
              private _alertService: AlertService,
              private _overlayContainer: OverlayContainer,
              private _heartbeat: HeartbeatService,
              private _browserService: BrowserService,
              private _router: Router,
              private _changeDetectorRef: ChangeDetectorRef) {
    this.isIEOrEdge = this._browserService.checkIfIEOrEdge();

    if ( isBoolean(JSON.parse(localStorage.getItem('darkTheme'))) ) {
      this.darkTheme = JSON.parse(localStorage.getItem('darkTheme'));
    }
    this.setTheme(this.darkTheme);

    // Close sidenav when navigating away from sidenav
    this.sideNavSubscription = this._router.events.subscribe((event: NavigationStart) => {
      if (event instanceof NavigationStart) {
        if(event.url === '/'){
          this.sideNav.close();
          this._changeDetectorRef.detectChanges();
        }
      }
    });
  }

  loadAccounts(): void {
    this._accountService.getAccounts().subscribe( (accounts: Account[]) => {
      this.accounts = accounts;
      this._changeDetectorRef.detectChanges();
    });
  }

  loadUser(): void {
    this._userService.getUserRole().subscribe((data: IUser) => {
      this.user = data;
      this._changeDetectorRef.detectChanges();
    }, (error: any) => {
      this.error = true;
      this._alertService.openAlert(error);
    });
  }

  ngOnInit(): void {
    this.loadAccounts();
    this.loadUser();
    let timer: Observable<number> = TimerObservable.create(HEARTBEAT_IN_MS, HEARTBEAT_IN_MS);
    this.heartbeatSub = timer.subscribe(() => {
      this._heartbeat.heartbeat_poll().subscribe( (response: any) => { /* Do nothing */ }, (error: any) => {
        console.error('ERROR: Heartbeat poll received an error!\n ' + JSON.stringify(error));
      });
    });
  }

  ngOnDestroy(): void {
    this.heartbeatSub.unsubscribe();
    this.sideNavSubscription.unsubscribe();
  }

  applyFilter(filterValue: string): void {
    this.dataSource.filter = filterValue;
    this._changeDetectorRef.detectChanges();
  }

  applyEnvironmentFilter(environment: string): void {
    this.credentials = this.allCredentials;
    if (environment === 'all') {
      this.dataSource.data = this.credentials;
      this._changeDetectorRef.detectChanges();
    } else {
      this.dataSource.data = this.credentials
        .filter((credential: Credential) => {
          return credential.environment !== null && credential.environment.toLowerCase() === environment;
        });

    }
  }

  toggleTheme(): void {
    this.darkTheme = !this.darkTheme;
    this.setTheme(this.darkTheme);
  }

  setTheme(darkTheme: Boolean): void {
    if ( darkTheme ) {
      this._overlayContainer.getContainerElement().classList.remove('light-theme');
      this._overlayContainer.getContainerElement().classList.add('dark-theme');
    } else {
      this._overlayContainer.getContainerElement().classList.remove('dark-theme');
      this._overlayContainer.getContainerElement().classList.add('light-theme');
    }
    localStorage.setItem('darkTheme', this.darkTheme.toString());
  }

  checkAuthorization(): void {
      this.authorizations = this._permissionService.getAuthorizations(this.user, this.selected.account);
  }

  getUniqueEnvironments(credentials: ICredential[]): string[] {
    let uniqueEnvironments: string[] = credentials
      .map( (credential: any) => credential.environment)
      .filter((x: ICredential, i: number, environments: any) => environments.indexOf(x) === i)
      .filter((environment: string) => environment !== null)
      .sort();
    uniqueEnvironments.unshift('ALL');

    return uniqueEnvironments;
  }

  searchCredentials(selected: Selected): void {
    this.loading = true;
    this.checkAuthorization();
    if (selected !== undefined && selected.application !== '' && selected.region !== undefined) {
      this.loading = true;
      this._credentialService.getCredentials(selected).subscribe((credentials: ICredential[]) => {
          if (credentials) {
            this.environments = this.getUniqueEnvironments(credentials);
            this.allCredentials = credentials;
            let localEnvironment: string = localStorage.getItem('environment');
            if (this.environments.includes(localEnvironment) && localEnvironment !== ''){
              this.selected.environment = localEnvironment;
            } else {
              this.selected.environment = 'all';
              localStorage.setItem('environment', 'all');
            }
            if(localEnvironment.toLowerCase() !== 'all') {
              credentials = credentials.filter((credential: Credential) => {
                return credential.environment !== null && credential.environment.toLowerCase() === localEnvironment;
              });
            }


            this.selected.key = localStorage.getItem('key');


            this.dataSource = new MatTableDataSource(credentials);
            this.dataSource.filter = this.selected.key;
            this.credentials = credentials;
            this._changeDetectorRef.detectChanges();
            this._router.navigate(['']);
          } else {
            this.selected.environment = undefined;
            this.selected.key = undefined;
            this._changeDetectorRef.detectChanges();
          }
          this.loading = false;
          this.loaded = true;
        }, (error: any) => {
          this.loading = false;
          this.loaded = true;
          this._alertService.openAlert(error);
          this.dataSource = new MatTableDataSource<ICredential[]>();
          this._changeDetectorRef.detectChanges();
      });
    } else {
        this.dataSource = new MatTableDataSource<ICredential[]>();
        this.environments = [];
        this.loading = false;
        this.loaded = false;
        this._changeDetectorRef.detectChanges();
    }
  }
  
  confirmDelete(credential: Credential): void {
    let config: MatDialogConfig = {
      data: {
        application: this.selected.application,
        account: this.selected.account.alias,
        region: this.selected.region,
        credential: credential,
      },
      width: '500px',
      disableClose: false,
    };
    const dialog = this._dialog.open(DeleteDialogComponent, config);

    dialog.afterClosed().subscribe((result: IDialogResponse) => {
      switch (result.outcome){
        case 'success':
      let message: string = 'Credential ' + credential.longKey + ' deleted';
      this._snackBarService.open( message, '' , { duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom' });
      this.searchCredentials(this.selected);
          break;
        case 'error':
          this._alertService.openAlert(result.data);
          break;
        default:
          // do nothing for canceled
      }
    });
  }

  openSideNav(credential: ICredential, route: string, tab: number): void {
    this.sideNav.open();
    this.isSideNavOpened = true;
    this.isSideNavCloseDisabled = false;

    this.selectedCredential = Object.assign({}, credential);
    switch(route){
      case 'add':
        this._router.navigate(['add']);
        this.isSideNavCloseDisabled = true;
        break;
      case 'edit':
        this._router.navigate(['edit']);
        this.isSideNavCloseDisabled = true;
        break;
      case 'rotate':
        this._router.navigate(['rotate']);
        this.isSideNavCloseDisabled = true;
        break;
      case 'view':
        this._router.navigate(['view', {tab: tab}]);
        this.isSideNavCloseDisabled = false;
        break;
    }
    this._changeDetectorRef.detectChanges();
  }

  closeSideNav(): void {
    this.isSideNavOpened = false;
    this.selectedCredential = undefined;
    this._router.navigate(['']);
    this.sideNav.close();
    this._changeDetectorRef.detectChanges();
  }

  closeSideNavAndRefresh(refresh: boolean): void {
    if ( refresh ) {
      this.searchCredentials(this.selected);
    }
    this.selectedCredential = undefined;
    this.closeSideNav();
  }

  refreshTable(): void {
    this.searchCredentials(this.selected);
  }
}
