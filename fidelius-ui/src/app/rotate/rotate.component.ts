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
  Credential, CredentialService, IActiveDirectory, IMetadata, Metadata, RotationDTO
} from '../../services/credential.service';
import { MainComponent } from '../main/main.component';
import { BrowserService } from '../../services/browser.service';
import { APPLICATION_LIST_LABEL_NAME } from '../../config/permissions';
import { Meta } from '@angular/platform-browser';

@Component({
  selector: 'fidelius-rotate',
  templateUrl: './rotate.component.html',
  styleUrls: ['./rotate.component.scss'],
  providers: [],
})
export class RotateComponent implements OnInit{
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
  metadata: Metadata = new Metadata();
  existingMetadata: boolean = false;

  sourceTypes: string[] = ["Aurora", "RDS", "Service Account"];
  sourceNames: string[] = [];
  filteredSourceNames: string[] = [];
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
    this.credential.environment = this._parentComponent.selectedCredential.environment;
    this.credential.component = this._parentComponent.selectedCredential.component;
    this.credential.shortKey = this._parentComponent.selectedCredential.shortKey;
    this.credential.longKey = this._parentComponent.selectedCredential.longKey;
    this.credential.account = this._parentComponent.selected.account.alias;
    this.credential.application = this._parentComponent.selected.application;
    this.credential.region = this._parentComponent.selected.region;
    this.isIEOrEdge = this._browserService.checkIfIEOrEdge();
    
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
      }  
      this._changeDetectorRef.detectChanges();
    }, (error: any) => {
        this._alertService.openAlert(error);
        this._changeDetectorRef.detectChanges();
    });
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

  rotateCredential(): void {
    this.sendingForm = true;
    
    this.credential.lastUpdatedDate = new Date().toISOString();
    if(this.existingMetadata){
      this.rotate();
    }
    else{
      this.metadata.lastUpdatedDate = this.credential.lastUpdatedDate;
      this._credentialService.updateMetadata(this.metadata).subscribe((response: any) => {
        this.rotate();
      } ,(error: any) => {
        this.sendingForm = false;
        let message: string = 'Metadata' + this.credential.longKey + 'failed to update: ' + error;
        this._snackBarService.open(message,  'DISMISS', { horizontalPosition: 'center', verticalPosition: 'bottom', panelClass: "snackbar-error" });
        this._alertService.openAlert(error);
      });
    }
    
    
  }

  rotate(): void {
    let rotationDTO: RotationDTO = new RotationDTO();
    rotationDTO.account = this.credential.account;
    rotationDTO.sourceType = this.metadata.sourceType;
    rotationDTO.source = this.metadata.source;
    rotationDTO.shortKey = this.credential.shortKey;
    rotationDTO.application = this.credential.application;
    rotationDTO.environment = this.credential.environment;
    rotationDTO.component = this.credential.component;
    rotationDTO.region = this.credential.region;
    this._credentialService.rotateCredential(rotationDTO).subscribe(()=> {
      let message: string = 'Credential ' + this.credential.longKey + ' rotated';
      this._snackBarService.open(message,  '', { duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom', panelClass: "snackbar-success" });
      this.sendingForm = false;
      this.closeSideNav(true);
    }, (error: any) => {
      this.sendingForm = false;
      let message: string = 'Credential ' + this.credential.longKey + ' failed to rotate: ' + error.status + " " + error.statusText;
      this._snackBarService.open(message,  'DISMISS', { horizontalPosition: 'center', verticalPosition: 'bottom', panelClass: "snackbar-error" });
    });
  }

  sourceNameAuto(): void {
    if(!this.existingMetadata && this.metadata.sourceType !== undefined){
      this._credentialService.getSourceNames(this.credential.account, this.credential.region, this.metadata.sourceType).subscribe((sourceNames: string[])=>{
        this.sourceNames = sourceNames;
        this.filteredSourceNames = sourceNames;
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
