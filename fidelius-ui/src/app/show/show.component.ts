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
  ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild
} from '@angular/core';
import { NgForm } from '@angular/forms';
import { Credential, CredentialService, Selected, IHistory, Metadata, RotationDTO } from '../../services/credential.service';
import { AlertService } from '../../services/alert.service';
import { ClipboardService } from 'ngx-clipboard';
import { MatSnackBar } from '@angular/material';
import { APPLICATION_LIST_LABEL_NAME } from '../../config/permissions';
import { MainComponent } from '../main/main.component';
import { BrowserService } from '../../services/browser.service';



@Component({
  selector: 'fidelius-show',
  templateUrl: './show.component.html',
  styleUrls: ['./show.component.scss'],
  providers: [ ClipboardService, AlertService ],
  changeDetection: ChangeDetectionStrategy.OnPush,

})
export class ShowComponent implements OnInit {
  @Input() selected: Selected = new Selected();
  @Input() credential: Credential = new Credential();
  @Input() canViewSecret: boolean = false;
  hideSecret: boolean = true;
  copyingSecret: boolean = false;
  loadedSecret: boolean = false;
  loadingSecret: boolean = false;
  isIEOrEdge: boolean;
  showError: boolean = true;
  history: IHistory;
  metadata: Metadata = new Metadata();
  sourceType: string = "-"
  sourceName: string = "-"
  secretPlaceholderText: string = 'loadingSecret.................';
  rotating: boolean = false;
  @ViewChild(NgForm) showForm: NgForm;
  APPLICATION_LIST_NAME: string = APPLICATION_LIST_LABEL_NAME;


constructor( private _credentialService: CredentialService,
               private _clipboardService: ClipboardService,
               private _snackBarService: MatSnackBar,
               private _alertService: AlertService,
               private _parentComponent: MainComponent,
               private _changeDetectorRef: ChangeDetectorRef,
               private _browserService: BrowserService,
               ) {
  }

  ngOnInit(): void {
    this.credential.account = this.selected.account.alias;
    this.credential.application = this.selected.application;
    this.credential.secret = this.secretPlaceholderText;
    this.credential.region = this.selected.region;
    this.isIEOrEdge = this._browserService.checkIfIEOrEdge();
    this.getHistory();
    this.getMetadata();
    this._changeDetectorRef.detectChanges();
  }

  copy(): void {
    this.copyingSecret = true;
    this._clipboardService.copyFromContent(this.credential.secret);
    let message: string = 'Copied to clipboard';
    this._snackBarService.open(message, '',  { duration: 3000, horizontalPosition: 'center', verticalPosition: 'bottom' });
    this.copyingSecret = !this.copyingSecret;
  }

  showSecret(): void {
    if ( this.hideSecret) {
      this.loadSecret();
    } else {
      this.credential.secret = this.secretPlaceholderText;
      this._changeDetectorRef.detectChanges();
    }
    this.hideSecret = !this.hideSecret;
  }

  loadAndCopySecret(): void {
    this.loadingSecret = true;
    this._credentialService.getSecret(this.credential).subscribe( (secret: Credential) => {
      this.credential.secret = secret.secret;
      this.loadingSecret = false;
      this.copy();
      this.credential.secret = this.secretPlaceholderText;
      this._changeDetectorRef.detectChanges();
    }, (error: any) => {
      this.credential.secret = this.secretPlaceholderText;
      this.loadedSecret = false;
      this.loadingSecret = false;
      this._alertService.openAlert(error);
      this._changeDetectorRef.detectChanges();
    });
  }

  loadSecret(): void {
    this.loadingSecret = true;
    this._credentialService.getSecret(this.credential).subscribe( (secret: Credential) => {
      this.loadedSecret = true;
      this.credential.secret = secret.secret;
      this.loadingSecret = false;
      this._changeDetectorRef.detectChanges();
    }, (error: any) => {
      this.credential.secret = this.secretPlaceholderText;
      this.loadedSecret = false;
      this.loadingSecret = false;
      this._alertService.openAlert(error);
      this._changeDetectorRef.detectChanges();
    });
  }

  getHistory(): void {
    this._credentialService.getCredentialHistory(this.credential).subscribe( (history: IHistory[]) => {
      history.sort((a, b) => a.revision < b.revision ? 1 : -1);
      this.history = history[0];
      this._changeDetectorRef.detectChanges();
    }, (error: any) => {
      this._alertService.openAlert(error);
      this._changeDetectorRef.detectChanges();
    });
  }

  getMetadata():void {
    this._credentialService.getMetadata(this.credential).subscribe( (metadata: Metadata) => {
      this.metadata = metadata;
      this._changeDetectorRef.detectChanges();
    }, (error: any) => {
      this.metadata = null;
      this._changeDetectorRef.detectChanges();
    });
  }

  rotateSecret():void{
    if(this.metadata.source && this.metadata.sourceType ){
      this.rotateCredential();
    }
    else{
      this._parentComponent.openSideNav(this.credential, "rotate", 0);
    }
    
  }

  rotateCredential(): void {
    this.rotating = true;
    let rotationDTO: RotationDTO = new RotationDTO();
    rotationDTO.account = this.credential.account;
    rotationDTO.sourceType = this.metadata.sourceType;
    rotationDTO.source = this.metadata.source;
    rotationDTO.shortKey = this.credential.shortKey;
    rotationDTO.application = this.credential.application;
    rotationDTO.environment = this.credential.environment;
    rotationDTO.component = this.credential.component;
    rotationDTO.region = this.credential.region;
    this.credential.lastUpdatedDate = new Date().toISOString();
    this._credentialService.rotateCredential(rotationDTO).subscribe( (data: any) => {
      this.rotating = false;
      let message: string = 'Credential ' + this.credential.longKey + ' rotated';
      this._snackBarService.open(message,  '', {  horizontalPosition: 'center', verticalPosition: 'bottom', panelClass: ["snackbar-success"], duration: 3000 });
      this._changeDetectorRef.detectChanges();
    }, (error: any) => {
      console.log(error)
      this.rotating = false;
      let message: string = 'Credential ' + this.credential.longKey + ' failed to rotate: ' + error.status + " " + error.statusText;      
      this._snackBarService.open(message,  'DISMISS', { horizontalPosition: 'center', verticalPosition: 'bottom', panelClass: ["snackbar-error"] });
      this._changeDetectorRef.detectChanges();
    });
  }


  editSecret():void{
    this._parentComponent.openSideNav(this.credential, "edit", 0);

  }

  dismissError():void{
    this.showError = false;
  }
}
