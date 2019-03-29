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

package org.finra.fidelius.exceptions;

import org.springframework.http.HttpStatus;

public class FideliusException extends RuntimeException{

    private int status;
    private HttpStatus error;

    public FideliusException() {
    }


    public FideliusException(String message) {
        super(message);
    }

    public FideliusException(String message, Throwable cause) {
        super(message, cause);

    }

    public FideliusException(String message, HttpStatus httpstatus) {
        super(message);
        this.error = httpstatus;
        this.status = httpstatus.value();
    }

    public FideliusException(Throwable cause) {
        super(cause);
    }

    public int getStatus() {
        return status;
    }

    public HttpStatus getError() {
        return error;
    }
}
