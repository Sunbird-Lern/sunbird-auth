/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
 */

package org.keycloak.testsuite.console.page;

import javax.ws.rs.core.UriBuilder;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.console.page.AdminConsoleRealm.CONSOLE_REALM;

/**
 *
 * @author tkyjovsk
 */
public class AdminConsoleCreate extends AdminConsole {

    public static final String ENTITY = "entity";

    public AdminConsoleCreate() {
        setUriParameter(CONSOLE_REALM, TEST);
    }
    
    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("/");
    }
    
    @Override
    public String getUriFragment() {
        return "/create/{" + ENTITY + "}/{" + CONSOLE_REALM + "}";
    }

    public AdminConsoleCreate setEntity(String entity) {
        setUriParameter(ENTITY, entity);
        return this;
    }

    public String getEntity() {
        return getUriParameter(ENTITY).toString();
    }

    public AdminConsoleCreate setConsoleRealm(String consoleRealm) {
        setUriParameter(CONSOLE_REALM, consoleRealm);
        return this;
    }

    public String getConsoleRealm() {
        return getUriParameter(CONSOLE_REALM).toString();
    }

}
