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

package org.finra.fidelius.model.membership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Membership extends HashMap<String, List<String>> {

    public Membership(HashMap<String, List<String>> membership) {
        super(membership);
    }

    public Membership() {
        super();
    }

    public List<String> getMemberships(){
        List<String> memberships = new ArrayList<>();
        if(this != null && this.size() > 0) {
            memberships = this
                    .values()
                    .stream()
                    .collect(Collectors.toList())
                    .get(0);
        }
        return memberships;
    }

    @Override
    public String toString() {
        return "Membership{ " + getMemberships() + " }";
    }
}
