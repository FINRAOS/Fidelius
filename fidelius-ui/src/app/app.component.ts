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

import {Component} from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import { TdMediaService } from '@covalent/core';
import { Router } from '@angular/router';
import {BrowserService} from '../services/browser.service';

@Component({
  selector: 'fidelius-app',
  templateUrl: './app.component.html',
  styleUrls: [ './app.component.css' ],
})
export class AppComponent  {
  isIEorEdge: boolean;

  constructor(public media: TdMediaService,
              private _iconRegistry: MatIconRegistry,
              private _domSanitizer: DomSanitizer,
              private _browserService: BrowserService,
              private _router: Router) {

    this.isIEorEdge = this._browserService.checkIfIEOrEdge();
    if (this.isIEorEdge) {
      localStorage.setItem('firstTimeLoad', 'false'); // intro is unsupported in IE, so skip it.
    }
    this._iconRegistry.addSvgIconInNamespace('assets', 'logo',
      this._domSanitizer.bypassSecurityTrustResourceUrl('assets/icons/logo.svg'));

    if (!this.isIEorEdge && !localStorage.getItem('firstTimeLoad')) {
      this._router.navigate(['/intro']);
    } else {
      this._router.navigate(['/']);
    }
  }
}
