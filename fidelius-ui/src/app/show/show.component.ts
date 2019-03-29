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
  ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges,
  ViewChild
} from '@angular/core';
import { NgForm } from '@angular/forms';
import { Credential, CredentialService, Selected } from '../../services/credential.service';
import { AlertService } from '../../services/alert.service';
import { ClipboardService } from 'ngx-clipboard';
import { MatSnackBar } from '@angular/material';
import { APPLICATION_LIST_LABEL_NAME } from '../../config/permissions';


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
  secretPlaceholderText: string = 'loadingSecret.................';
  @ViewChild(NgForm) showForm: NgForm;
  APPLICATION_LIST_NAME: string = APPLICATION_LIST_LABEL_NAME;


constructor( private _credentialService: CredentialService,
               private _clipboardService: ClipboardService,
               private _snackBarService: MatSnackBar,
               private _alertService: AlertService,
               private _changeDetectorRef: ChangeDetectorRef,
               ) {
  }

  ngOnInit(): void {
    this.credential.account = this.selected.account.alias;
    this.credential.application = this.selected.application;
    this.credential.secret = this.secretPlaceholderText;
    this.credential.region = this.selected.region;
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
}
