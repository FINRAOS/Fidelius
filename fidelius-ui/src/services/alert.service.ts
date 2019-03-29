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

import { Injectable } from '@angular/core';
import { TdDialogService } from '@covalent/core';

@Injectable()
export class AlertService {

  constructor(private _dialogService: TdDialogService) { }

  openAlert(error: any): void {
    if (error.status === 401 || error.status === 0) {
      let message: string = 'Your session has expired. Please refresh your browser.';
      this._dialogService.openConfirm({
        message: message,
        title: 'Session Expired',
        cancelButton: 'Close',
        acceptButton: 'Refresh',
      }).afterClosed().subscribe((refresh: boolean) => {
        if (refresh) {
          window.location.reload(true);
        }
      });
    } else if (error.status === 404 && error.error.error == undefined) {
      let message: string = 'Error while processing request. Please contact Fidelius admin.';
      this._dialogService.openAlert({
        message: message,
        title: 'ERROR',
        closeButton: 'OK',
      });
    } else if (error.status === 504) {
      let message: string = 'Error while processing request. Please try again later.';
      this._dialogService.openAlert({
        message: message,
        title: '504 ERROR',
        closeButton: 'OK',
      });
    }else {
      this._dialogService.openAlert({
        message: error.error.message,
        title: 'Error: ' + error.status + ' ' + error.error.error,
        closeButton: 'Ok',
      });
    }
  }
}
