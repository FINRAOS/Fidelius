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

package org.finra.fidelius.controllers;

import org.finra.fidelius.services.auth.FideliusRole;
import org.finra.fidelius.services.auth.FideliusRoleService;
import org.finra.fidelius.services.user.model.FideliusUserEntry;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Authorization Controller for Fidelius
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Inject
    private FideliusRoleService fideliusRoleService;

    @ResponseBody
    @GetMapping(value="/role")
    public Map<String, Object> getRole() {
        FideliusUserEntry user = fideliusRoleService.getUserProfile();
        FideliusRole role = fideliusRoleService.getRole();

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        result.put("role", role);
        result.put("accessInstructions", fideliusRoleService.getAccessInstructions());

        switch (role) {
            case MASTER:
                result.put("memberships", fideliusRoleService.getMasterMemberships());
                return result;
            case OPS:
                result.put("memberships", fideliusRoleService.getOpsMemberships());
                return result;
            case DEV: {
                result.put("memberships", fideliusRoleService.getDevMemberships());
                return result;
            }
        }
        return result;
    }
}
