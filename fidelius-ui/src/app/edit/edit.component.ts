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
  Credential, CredentialService, IActiveDirectory, IMetadata, Metadata
} from '../../services/credential.service';
import { MainComponent } from '../main/main.component';
import { BrowserService } from '../../services/browser.service';
import { APPLICATION_LIST_LABEL_NAME } from '../../config/permissions';
import { concat } from 'rxjs';

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
  sourceNames: string[] = [];
  rotationUserManual: string = '';
  filteredSourceNames: string[] = [];
  secretType: string;
  isIEOrEdge: boolean;
  existingMetadata: boolean = false;
  isLoading: boolean = true;
  hasError: boolean = false;
  metadata: Metadata = new Metadata();
  editSecret: boolean = false;
  editMetadata: boolean = false;

  sourceTypes: string[] = ["-", "Aurora", "RDS", "Service Account"];

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
    this.getRotationUserManual();
    this.getSourceTypes();
    this.loadCredential();
    this.getMetadata();
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



  getMetadata():void {
    this._credentialService.getMetadata(this.credential).subscribe( (metadata: IMetadata) => {
      this.metadata = metadata;
      if(!this.metadata.source || !this.sourceTypes.includes(this.metadata.sourceType)){
        this.existingMetadata = false;
      }
      else{
        this.existingMetadata = true;
        this.sourceNameAuto();
      }
      this._changeDetectorRef.detectChanges();
    }, (error: any) => {
        this._alertService.openAlert(error);
        this._changeDetectorRef.detectChanges();
    });
  }
  getSourceTypes(){
    this._credentialService.getSourceTypes().subscribe((types: string[])=> {
      types.unshift("-");
      this.sourceTypes = types;
    });
  }

  getRotationUserManual(): void{
    this._credentialService.getRotationUserManual().subscribe((rotationUserManual: string)=> {
      this.rotationUserManual = rotationUserManual;
    });
  }

  getDisplayedSourceName(){
    if(!this.metadata.sourceType){
      return "Source Name"
    }

    if(this.metadata.sourceType.toLowerCase().includes("service account")){
      return "Service Account Name"
    }

    if(this.metadata.sourceType.toLowerCase().includes("rds")){
      return "Instance Identifier"
    }

    const instanceTypes = ["documentdb", "aurora", "redshift"]
    if(instanceTypes.some(type => this.metadata.sourceType.toLowerCase().includes(type))){
      return "Primary Cluster Identifier"
    }

    return "Source Name"
  }

  getActiveDirectoryPasswordPattern(): void {
    this._credentialService.getActiveDirectoryPasswordValidation().subscribe( (activeDirectory: IActiveDirectory) => {
      this.activeDirectory = activeDirectory;
    });
  }

  setPasswordPattern(): void {
    this.editSecret = true;
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
    this.metadata.lastUpdatedDate = new Date().toISOString();
    if(this.editSecret && this.editMetadata && this.metadata.sourceType != "-"){
      concat(
        this._credentialService.updateMetadata(this.metadata),
        this._credentialService.updateCredential(this.credential)
      ).subscribe( (result: any) => {
        let message: string = 'Credential ' + this.credential.longKey + ' updated';
        this._snackBarService.open(message,  '', { duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom', panelClass: "snackbar-success" });
        this.closeSideNav(true);
      }, (error: any) => {
        this.sendingForm = false;
        this._changeDetectorRef.detectChanges();
        this._alertService.openAlert(error);
      });
    }
    else if(this.editSecret && this.editMetadata && this.metadata.sourceType === "-" ){
      concat(
        this._credentialService.deleteMetadata(this.credential),
        this._credentialService.updateCredential(this.credential)
      ).subscribe( (result: any) => {
        let message: string = 'Credential ' + this.credential.longKey + ' updated';
        this._snackBarService.open(message,  '', { duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom', panelClass: "snackbar-success" });
        this.closeSideNav(true);
      }, (error: any) => {
        this.sendingForm = false;
        this._changeDetectorRef.detectChanges();
        this._alertService.openAlert(error);
      });
    }
    else if(this.editMetadata && this.metadata.sourceType === "-"){
      this._credentialService.deleteMetadata(this.credential).subscribe( () => {
        let message: string = 'Credential Metadata ' + this.credential.longKey + ' deleted';
        this._snackBarService.open(message,  '', { duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom', panelClass: "snackbar-success" });
        this.closeSideNav(true);
      }, (error: any) => {
        this.sendingForm = false;
        this._changeDetectorRef.detectChanges();
        this._alertService.openAlert(error);
      });
    }
    else if(this.editSecret){
      this._credentialService.updateCredential(this.credential).subscribe( (credential: Credential) => {
        let message: string = 'Credential ' + this.credential.longKey + ' updated';
        this._snackBarService.open(message,  '', { duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom', panelClass: "snackbar-success" });
        this.closeSideNav(true);
      }, (error: any) => {
        this.sendingForm = false;
        this._changeDetectorRef.detectChanges();
        this._alertService.openAlert(error);
      });
    }
    else{
      this._credentialService.updateMetadata(this.metadata).subscribe( (metadata: Metadata) => {
        let message: string = 'Credential Metadata ' + this.credential.longKey + ' updated';
        this._snackBarService.open(message,  '', { duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom', panelClass: "snackbar-success" });
        this.closeSideNav(true);
      }, (error: any) => {
        this.sendingForm = false;
        this._changeDetectorRef.detectChanges();
        this._alertService.openAlert(error);
      });
    }
  }

  sourceNameAuto(): void {
    if(this.metadata.sourceType !== undefined){
      this._credentialService.getSourceNames(this.credential.account, this.credential.region, this.metadata.sourceType, this.metadata.application).subscribe((sourceNames: string[])=>{
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
  formSourceNameAuto(): void{
    this.editMetadata = true;
    this.sourceNameAuto();
  }

  filterSourceName(event: any):void{
    const input = event.target.value;
    this.filteredSourceNames = this.sourceNames.filter(source => source.includes(input));
    this._changeDetectorRef.detectChanges();
  }
  sourceChange():void{
    this.editMetadata = true;
  }
}
