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

import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DeleteDialogComponent, IDialogResponse } from './delete-dialog.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material';
import { Credential, CredentialService } from '../../../services/credential.service';
import { Observable } from "rxjs/Observable";

describe('DeleteDialogComponent', () => {
  let component: DeleteDialogComponent;
  let fixture: ComponentFixture<DeleteDialogComponent>;

  class MockTdDialogService {
    openAlert(): any {
      return true;
    }

    close(response: IDialogResponse): void{}
  }

  class MockCredentialService {
    deleteCredential(credential: Credential): void{}
  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DeleteDialogComponent ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      imports: [MatDialogModule],
      providers: [
        {provide: MatDialogRef, useClass: MockTdDialogService },
        {provide: CredentialService, useClass: MockCredentialService },
        {provide: MAT_DIALOG_DATA, useValue: { application: 'test', region: 'east', credential: {longKey: ''} }}]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DeleteDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it(`should successfully delete`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: MatDialogRef<DeleteDialogComponent> = fixture.debugElement.injector.get(MatDialogRef);
    spyOn(dialogService, 'close').and.callThrough();
    spyOn(credentialService, 'deleteCredential').and.callFake(() => {
      return Observable.throw({status: 404});
    });

    component.deleteCredential();
    fixture.detectChanges();

    expect(credentialService.deleteCredential).toHaveBeenCalledTimes(1);
    expect(dialogService.close).toHaveBeenCalledTimes(1);
    expect(component.deletingCredential).toBeFalsy();

  });

  it(`should display error on delete`, async() => {
    const credentialService: CredentialService = fixture.debugElement.injector.get(CredentialService);
    const dialogService: MatDialogRef<DeleteDialogComponent> = fixture.debugElement.injector.get(MatDialogRef);
    spyOn(dialogService, 'close').and.callFake(()=>{
      return Observable.throw({status: 500});
    });
    spyOn(credentialService, 'deleteCredential').and.callFake(() => {
      return Observable.throw({status: 404});
    });

    component.deleteCredential();
    fixture.detectChanges();

    expect(credentialService.deleteCredential).toHaveBeenCalledTimes(1);
    expect(dialogService.close).toHaveBeenCalledTimes(1);
    expect(dialogService.close).toHaveBeenCalledWith({outcome: 'error', data: {status: 404}});
    expect(component.deletingCredential).toBeFalsy();

  });

});
