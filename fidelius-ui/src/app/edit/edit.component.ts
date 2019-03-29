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
  Component, OnInit, ViewChild, HostListener, ChangeDetectorRef,
} from '@angular/core';
import { AbstractControl, NgForm } from '@angular/forms';
import { MatSnackBar } from '@angular/material';
import { AlertService } from '../../services/alert.service';
import {
  Credential, CredentialService, IActiveDirectory
} from '../../services/credential.service';
import { MainComponent } from '../main/main.component';
import { BrowserService } from '../../services/browser.service';
import { APPLICATION_LIST_LABEL_NAME } from '../../config/permissions';

@Component({
  selector: 'fidelius-edit',
  templateUrl: './edit.component.html',
  styleUrls: ['./edit.component.scss'],
  providers: [],
})
export class EditComponent implements OnInit{
  credential: Credential = new Credential();
  hideSecret: boolean = true;
  sendingForm: boolean = false;
  defaultPasswordPattern: string = '.*';
  passwordPattern: string = '.*';
  activeDirectory: IActiveDirectory;
  secretTypes: string[] = ['Password', 'Active Directory', 'Other'];
  secretType: string;
  isIEOrEdge: boolean;
  isLoading: boolean = true;
  hasError: boolean = false;
  @ViewChild(NgForm) editForm: NgForm;
  APPLICATION_LIST_LABEL_NAME: string = APPLICATION_LIST_LABEL_NAME;

  constructor(private _credentialService: CredentialService,
              private _alertService: AlertService,
              private _snackBarService: MatSnackBar,
              private _changeDetectorRef: ChangeDetectorRef,
              private _parentComponent: MainComponent,
              private _browserService: BrowserService) {
  }

  ngOnInit(): void {
    this.getActiveDirectoryPasswordPattern();
    this.credential.environment = this._parentComponent.selectedCredential.environment;
    this.credential.component = this._parentComponent.selectedCredential.component;
    this.credential.shortKey = this._parentComponent.selectedCredential.shortKey;
    this.credential.longKey = this._parentComponent.selectedCredential.longKey;
    this.credential.account = this._parentComponent.selected.account.alias;
    this.credential.application = this._parentComponent.selected.application;
    this.credential.region = this._parentComponent.selected.region;
    this.isIEOrEdge = this._browserService.checkIfIEOrEdge();
    this.loadCredential();
  }

  @HostListener('window:beforeunload', ['$event'])
  doSomething($event: any): any {
    $event.returnValue = 'Data you have entered may not be saved.';
  }

  loadCredential(): void{
    this._credentialService.getCredential(this._parentComponent.selected, this.credential).subscribe( (credential: Credential) => {
    this.credential = credential;
    this.isLoading  = false;
    this._changeDetectorRef.detectChanges();
    }, (error: any) => {
      this.isLoading = false;
      this.hasError = true;
      this._alertService.openAlert(error);
      this._changeDetectorRef.detectChanges();
    });
  }

  getActiveDirectoryPasswordPattern(): void {
    this._credentialService.getActiveDirectoryPasswordValidation().subscribe( (activeDirectory: IActiveDirectory) => {
      this.activeDirectory = activeDirectory;
    });
  }

  setPasswordPattern(): void {
    if (this.secretType === 'Active Directory') {
      this.credential.isActiveDirectory = true;
      this.passwordPattern = this.activeDirectory.validActiveDirectoryRegularExpression;
      this.editForm.controls['secret'].reset();
    } else {
      this.credential.isActiveDirectory = false;
      this.passwordPattern = this.defaultPasswordPattern;
      this.editForm.controls['secret'].reset();
    }
    this._changeDetectorRef.detectChanges();
  }

  getLongKey(): string {
    let longKey: string = undefined;
    if ( this.credential.shortKey && this.credential.component) {
      longKey = this.credential.application + '.' +
        this.credential.component + '.' +
        this.credential.environment + '.' +
        this.credential.shortKey;
    } else if ( this.credential.shortKey && !this.credential.component) {
      longKey = this.credential.application + '.' +
        this.credential.environment + '.' +
        this.credential.shortKey;
    }
    return longKey;
  }

  isInputField(): boolean {
    return this.secretType === 'Password' || this.secretType === 'Active Directory';
  }

  validateSecret(): void {
    let form: AbstractControl = this.editForm.controls['secret'];
    if (!form.touched) {
      form.markAsTouched();
    }
  }

  closeSideNav(refresh: boolean): void {
    this._parentComponent.closeSideNavAndRefresh(refresh);
  }

  updateCredential(): void {
    this.sendingForm = true;
    this.credential.lastUpdatedDate = new Date().toISOString();
    this._credentialService.updateCredential(this.credential).subscribe( (credential: Credential) => {
      let message: string = 'Credential ' + credential.longKey + ' updated';
      this._snackBarService.open(message,  '', { duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom' });
      this.closeSideNav(true);
    }, (error: any) => {
      this.sendingForm = false;
      this._alertService.openAlert(error);
    });
  }
}
