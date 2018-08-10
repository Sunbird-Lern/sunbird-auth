/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.protocol.oidc;

import org.keycloak.provider.Provider;

import javax.ws.rs.core.Response;

/**
 * Provides introspection for a determined OAuth2 token type.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface TokenIntrospectionProvider extends Provider {

    /**
     * Introspect the <code>token</code>.
     *
     * @param token the token to introspect.
     * @return the response with the information about the token
     */
    Response introspect(String token);
}
