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

import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, NgControl, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AppComponent } from './app.component';

import {
  MatButtonModule, MatListModule, MatCardModule, MatMenuModule, MatInputModule, MatIconModule, MatSelectModule, MatDialogModule, MatSnackBarModule,
  MatToolbarModule, MatTabsModule, MatSidenavModule, MatTooltipModule, MatRadioModule, MatAutocompleteModule, MatTableModule, MatPaginatorModule,
  MatSortModule, MatProgressBarModule, MatCheckboxModule,
} from '@angular/material';

import {
  CovalentCommonModule, CovalentLayoutModule, CovalentMediaModule, CovalentDialogsModule, CovalentNotificationsModule, CovalentMenuModule, CovalentMessageModule,
} from '@covalent/core';
import { AppRoutingModule, routedComponents } from './app-routing.module';
import { MainComponent } from './main/main.component';
import { AddComponent } from './add/add.component';
import { EditComponent } from './edit/edit.component';
import { ShowComponent } from './show/show.component';
import { ClipboardModule } from 'ngx-clipboard';
import { SearchComponent } from './search/search.component';
import { CredentialService } from '../services/credential.service';
import { AccountService } from '../services/account.service';
import { PermissionService } from '../services/permission.service';
import { IntroComponent } from './intro/intro.component';
import { HistoryComponent } from './history/history.component';
import { CredentialInfoComponent } from './credential-info/credential-info.component';
import { OverlayContainer } from '@angular/cdk/overlay';
import { BowserService } from 'ngx-bowser';
import { WindowService } from 'ngx-bowser/dist/src';
import { CredentialsTableComponent } from './credentials-table/credentials-table.component';
import { LoadingComponent } from './loading/loading.component';
import { DeleteDialogComponent } from './main/delete-dialog/delete-dialog.component';
import {CookieService} from 'ngx-cookie-service';
import {FlexLayoutModule} from '@angular/flex-layout';
import {BrowserService} from '../services/browser.service';
import { GroupByPipe } from './pipes/group-by.pipe';

@NgModule({
  imports:      [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    FlexLayoutModule,
    ReactiveFormsModule,
    AppRoutingModule,
    HttpClientModule,
    /** Material Modules */
    MatButtonModule,
    MatListModule,
    MatIconModule,
    MatCardModule,
    MatCheckboxModule,
    MatMenuModule,
    MatInputModule,
    MatSelectModule,
    MatDialogModule,
    MatSnackBarModule,
    MatToolbarModule,
    MatTabsModule,
    MatSidenavModule,
    MatTooltipModule,
    MatRadioModule,
    MatAutocompleteModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressBarModule,
    /** Covalent Modules */
    CovalentCommonModule,
    CovalentLayoutModule,
    CovalentMediaModule,
    CovalentDialogsModule,
    CovalentNotificationsModule,
    CovalentMenuModule,
    CovalentMessageModule,
    ClipboardModule,
  ],
  declarations: [ AppComponent,
    routedComponents,
    MainComponent,
    AddComponent,
    EditComponent,
    ShowComponent,
    SearchComponent,
    IntroComponent,
    HistoryComponent,
    CredentialInfoComponent,
    CredentialsTableComponent,
    LoadingComponent,
    DeleteDialogComponent,
    GroupByPipe
     ],
  providers: [CredentialService,
    AccountService,
    PermissionService, BowserService, WindowService, CookieService, BrowserService],
  entryComponents: [ DeleteDialogComponent, ],
  bootstrap:    [ AppComponent ]
})
export class AppModule {
  constructor( private _overlayContainer: OverlayContainer){
    this._overlayContainer.getContainerElement().classList.add('darkTheme');
  }
}
