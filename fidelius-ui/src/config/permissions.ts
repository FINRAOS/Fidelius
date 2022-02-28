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

export const APPLICATION_LIST_LABEL_NAME: string = 'Application';

export const PERMISSIONS: any = {
  'permissions': {
  'accounts': {
    'dev': {
      'roles': {
        'DEV': [
          'createCredential',
          'updateCredential',
          'rotateCredential',
          'viewCredential',
          'viewCredentialSecret',
          'viewCredentialHistory',
          'deleteCredential',
        ],
        'OPS': [
          'createCredential',
          'updateCredential',
          'rotateCredential',
          'viewCredential',
          'viewCredentialSecret',
          'viewCredentialHistory',
          'deleteCredential',
        ],
        'MASTER': [
          'createCredential',
          'updateCredential',
          'rotateCredential',
          'viewCredential',
          'viewCredentialSecret',
          'viewCredentialHistory',
          'deleteCredential',
        ],
      },
    },
    'qa': {
      'roles': {
        'DEV': [
          'createCredential',
          'updateCredential',
          'rotateCredential',
          'viewCredential',
          'viewCredentialSecret',
          'viewCredentialHistory',
          'deleteCredential',
        ],
        'OPS': [
          'createCredential',
          'updateCredential',
          'rotateCredential',
          'viewCredential',
          'viewCredentialSecret',
          'viewCredentialHistory',
          'deleteCredential',
        ],
        'MASTER': [
          'createCredential',
          'updateCredential',
          'rotateCredential',
          'viewCredential',
          'viewCredentialSecret',
          'viewCredentialHistory',
          'deleteCredential',
        ],
      },
    },
    'prod': {
      'roles': {
        'DEV': [
          'viewCredential',
          'viewCredentialHistory',
        ],
        'OPS': [
          'createCredential',
          'updateCredential',
          'rotateCredential',
          'viewCredential',
          'viewCredentialSecret',
          'viewCredentialHistory',
        ],
        'MASTER': [
          'createCredential',
          'updateCredential',
          'rotateCredential',
          'viewCredential',
          'viewCredentialSecret',
          'viewCredentialHistory',
          'deleteCredential',
        ],
      },
    },
  },
},
};
