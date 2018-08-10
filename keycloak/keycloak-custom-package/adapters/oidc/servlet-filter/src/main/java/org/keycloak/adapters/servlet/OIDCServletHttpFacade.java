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

package org.keycloak.adapters.servlet;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCServletHttpFacade extends ServletHttpFacade implements OIDCHttpFacade {

    public OIDCServletHttpFacade(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public KeycloakSecurityContext getSecurityContext() {
        return (KeycloakSecurityContext)request.getAttribute(KeycloakSecurityContext.class.getName());
    }
}
