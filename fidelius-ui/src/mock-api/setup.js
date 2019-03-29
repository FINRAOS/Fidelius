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

// Updates mockDirectory full path
const updateJsonFile = require('update-json-file');
const filePath = 'src/mock-api/config.json';
const options = { defaultValue: () => ({}) };

updateJsonFile(filePath, (data) => {
  // factory function is run each time, so `data` is a new object each time
  data.mockDirectory = process.cwd() + '/src/mock-api/data/';
return data
}, options);


