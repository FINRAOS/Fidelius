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
  ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, HostListener, Input, OnInit, Output,
  ViewChild,
  AfterViewInit,
} from '@angular/core';
import { AbstractControl, FormControl, NgForm } from '@angular/forms';
import { MatSnackBar } from '@angular/material';
import { Credential, CredentialService, IActiveDirectory, Selected, ICredential, Metadata } from '../../services/credential.service';
import { AlertService } from '../../services/alert.service';
import { MainComponent } from '../main/main.component';
import { BrowserService } from '../../services/browser.service';
import { APPLICATION_LIST_LABEL_NAME } from '../../config/permissions';
import { THIS_EXPR } from '@angular/compiler/src/output/output_ast';


@Component({
  selector: 'fidelius-add',
  templateUrl: './add.component.html',
  styleUrls: ['./add.component.scss'],
  providers: [AlertService],
})
export class AddComponent implements OnInit {
  credential: Credential = new Credential();
  metadata: Metadata = new Metadata();
  hideSecret: boolean = true;
  sendingForm: boolean = false;
  isDuplicateCredential: boolean = false;
  defaultPasswordPattern: string = '.*';
  passwordPattern: string = '.*';
  activeDirectory: IActiveDirectory;
  @ViewChild(NgForm) addForm: NgForm;
  secretTypes: string[] = ['Password', 'Active Directory', 'Other'];
  secretType: string ;
  sourceTypes: string[] = ["-", "Aurora", "RDS", "Service Account"];
  sourceNames: string[] = [];
  filteredSourceNames: string[] = [];

  isIEOrEdge: boolean;
  APPLICATION_LIST_LABEL_NAME: string = APPLICATION_LIST_LABEL_NAME;

  constructor(private _credentialService: CredentialService,
              private _alertService: AlertService,
              private _snackBarService: MatSnackBar,
              private _changeDetectorRef: ChangeDetectorRef,
              private _parentComponent: MainComponent,
              private _browserService: BrowserService) {}

  ngOnInit(): void {
    this.credential.account = this._parentComponent.selected.account.alias;
    this.credential.application = this._parentComponent.selected.application;
    this.credential.environment = '';
    this.credential.lastUpdatedDate = new Date().toISOString();
    this.credential.region = this._parentComponent.selected.region;
    this.metadata.account = this._parentComponent.selected.account.alias;
    this.metadata.application = this._parentComponent.selected.application;
    this.metadata.lastUpdatedDate = new Date().toISOString();
    this.metadata.region = this._parentComponent.selected.region;

    this.getActiveDirectoryPasswordPattern();
    this.isIEOrEdge = this._browserService.checkIfIEOrEdge();
  }

  @HostListener('window:beforeunload', ['$event'])
  doSomething($event: any): any {
    $event.returnValue = 'Data you have entered may not be saved.';
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
      this.addForm.controls['secret'].reset();
    } else {
      this.credential.isActiveDirectory = false;
      this.passwordPattern = this.defaultPasswordPattern;
      this.addForm.controls['secret'].reset();
    }
    this._changeDetectorRef.detectChanges();
  }

  validateSecret(): void {
    let form: AbstractControl = this.addForm.controls['secret'];
    if (!form.touched) {
      form.markAsTouched();
    }
  }

  isInputField(): boolean {
    return this.secretType === 'Password' || this.secretType === 'Active Directory';
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

  closeSideNav(refresh: boolean): void {
    this._parentComponent.closeSideNavAndRefresh(refresh);
  }

  addCredential(): void {
    this.isDuplicateCredential = false;
    if (this.credential.component === ''){
      this.credential.component = undefined;
    }
    // Send the new credential
    this.sendingForm = true;
    this._credentialService.createCredential(this.credential).subscribe((credential: Credential) => {
      this.metadata.component = this.credential.component;
      this.metadata.shortKey = this.credential.shortKey;
      this.metadata.environment = this.credential.environment;
      this._credentialService.createMetadata(this.metadata).subscribe((metadata:Metadata)=>{
        this.sendingForm = false;
        let message: string = 'Credential ' + this.getLongKey() + ' created';
        this._snackBarService.open(message, '', {
          duration: 3000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
        this.closeSideNav(true);
        this._changeDetectorRef.detectChanges();
      }, (error: any) => {
        this.sendingForm = false;
        let message: string = 'Credential ' + this.getLongKey() + ' created, but Metadata failed to create';
        this._snackBarService.open(message, '', {
          duration: 3000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
        this._alertService.openAlert(error);
        this.closeSideNav(true);
        this._changeDetectorRef.detectChanges();
      });
      
    }, (error: any) => {
      this.sendingForm = false;
      if (error.status === 400 && error.error.message === 'Credential already exists!') {
        this.isDuplicateCredential = true;
        this._changeDetectorRef.detectChanges();
      } else {
        this._alertService.openAlert(error);
        this._changeDetectorRef.detectChanges();
      }
    });
  }

  sourceNameAuto(): void {
    if(this.metadata.sourceType !== undefined){
      this._credentialService.getSourceNames(this.credential.account, this.credential.region, this.metadata.sourceType).subscribe((sourceNames: string[])=>{
        this.sourceNames = sourceNames;
        this.filteredSourceNames = sourceNames;
        this._changeDetectorRef.detectChanges();
      }, (error: any) => {
        this.sourceNames = [];
        this.filteredSourceNames = [];
        this._changeDetectorRef.detectChanges();

      });
    }
  }

  filterSourceName(event: any):void{
    const input = event.target.value;
    this.filteredSourceNames = this.sourceNames.filter(source => source.includes(input));    
    this._changeDetectorRef.detectChanges();
  }


}
