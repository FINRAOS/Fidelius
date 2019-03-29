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

import { Component, Inject} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';
import { Credential, CredentialService} from '../../../services/credential.service';

@Component({
  selector: 'fidelius-delete-dialog',
  templateUrl: './delete-dialog.component.html',
  styleUrls: ['./delete-dialog.component.scss'],
})

export class DeleteDialogComponent {
  deletingCredential: boolean = false;

  constructor(
    public dialogRef: MatDialogRef<DeleteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: IDialogData,
    private _credentialService: CredentialService) {}

  deleteCredential(): void {
    this.data.credential.application = this.data.application;
    this.data.credential.account = this.data.account;
    this.data.credential.region = this.data.region;
    this.data.credential.secret = 'loadingSecret';
    this.deletingCredential = true;
    let response: IDialogResponse;
    this._credentialService.deleteCredential(this.data.credential).subscribe( (response: any) => {
      response = {
        outcome: 'success',
        data: response,
      };
      this.deletingCredential = false;
      this.dialogRef.close(response);
    }, (error: any) => {
      response = {
        outcome: 'error',
        data: error,
      };
      this.deletingCredential = false;
      this.dialogRef.close(response);
    });
  }

  cancel(): void {
    let response: IDialogResponse = {
      outcome: 'canceled',
    };
    this.dialogRef.close(response);
  }
}

export interface IDialogResponse {
  outcome: string;
  data?: any;
}

export interface IDialogData {
  application: string;
  account: string;
  region: string;
  credential: Credential;
}
