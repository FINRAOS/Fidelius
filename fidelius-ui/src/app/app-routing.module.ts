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
import { Routes, RouterModule } from '@angular/router';

import { MainComponent } from './main/main.component';
import { AddComponent } from './add/add.component';
import { IntroComponent } from './intro/intro.component';
import { EditComponent } from './edit/edit.component';
import { RotateComponent } from './rotate/rotate.component';
import { CredentialInfoComponent } from './credential-info/credential-info.component';

const routes: Routes = [
  {
    path: '',
    component: MainComponent,
    children: [
      { path: 'add', component: AddComponent },
      { path: 'edit', component: EditComponent },
      { path: 'rotate', component: RotateComponent },
      { path: 'view', component: CredentialInfoComponent },
    ]
  },
  {
    path: 'intro',
    component: IntroComponent,
  }
];


@NgModule({
  imports: [
    RouterModule.forRoot(routes, { useHash: false }),
  ],
  exports: [
    RouterModule,
  ]
})
export class AppRoutingModule { }
export const routedComponents: any[] = [];
