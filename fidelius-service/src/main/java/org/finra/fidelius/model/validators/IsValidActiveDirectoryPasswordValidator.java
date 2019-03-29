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

package org.finra.fidelius.model.validators;

import org.finra.fidelius.model.Credential;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class IsValidActiveDirectoryPasswordValidator implements ConstraintValidator<IsValidActiveDirectoryPassword, Credential> {

    @Value("${fidelius.validActiveDirectoryRegularExpression:(.*?)}")
    protected String validActiveDirectoryRegularExpression = "[^\\s]+";

    @Override
    public void initialize(IsValidActiveDirectoryPassword constraintAnnotation) {}

    @Override
    public boolean isValid(Credential value, ConstraintValidatorContext context) {
        if(value.getIsActiveDirectory() == null)
            return true;

        if (value.getIsActiveDirectory() && !Pattern.matches(validActiveDirectoryRegularExpression, value.getSecret())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Invalid Active Directory password")
                .addPropertyNode("secret").addConstraintViolation();
            return false;
         }

        return true;
    }
}
