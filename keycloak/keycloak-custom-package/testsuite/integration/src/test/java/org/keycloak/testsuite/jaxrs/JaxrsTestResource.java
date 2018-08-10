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

package org.keycloak.testsuite.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Path("res")
public class JaxrsTestResource {

    @Context
    protected SecurityContext securityContext;

    @GET
    @Produces("application/json")
    public SimpleRepresentation get() {
        return new SimpleRepresentation("get", securityContext.getUserPrincipal().getName(), securityContext.isUserInRole("user"),
                securityContext.isUserInRole("admin"), securityContext.isUserInRole("jaxrs-app-user"));
    }

    @POST
    @Produces("application/json")
    public SimpleRepresentation post() {
        return new SimpleRepresentation("post", securityContext.getUserPrincipal().getName(), securityContext.isUserInRole("user"),
                securityContext.isUserInRole("admin"), securityContext.isUserInRole("jaxrs-app-user"));
    }

    public static class SimpleRepresentation {
        private String method;
        private String principal;
        private Boolean hasUserRole;
        private Boolean hasAdminRole;
        private Boolean hasJaxrsAppRole;

        public SimpleRepresentation() {
        }

        public SimpleRepresentation(String method, String principal, boolean hasUserRole, boolean hasAdminRole,
                                    boolean hasJaxrsAppRole) {
            this.method = method;
            this.principal = principal;
            this.hasUserRole = hasUserRole;
            this.hasAdminRole = hasAdminRole;
            this.hasJaxrsAppRole = hasJaxrsAppRole;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPrincipal() {
            return principal;
        }

        public void setPrincipal(String principal) {
            this.principal = principal;
        }

        public Boolean getHasUserRole() {
            return hasUserRole;
        }

        public void setHasUserRole(Boolean hasUserRole) {
            this.hasUserRole = hasUserRole;
        }

        public Boolean getHasAdminRole() {
            return hasAdminRole;
        }

        public void setHasAdminRole(Boolean hasAdminRole) {
            this.hasAdminRole = hasAdminRole;
        }

        public Boolean getHasJaxrsAppRole() {
            return hasJaxrsAppRole;
        }

        public void setHasJaxrsAppRole(Boolean hasJaxrsAppRole) {
            this.hasJaxrsAppRole = hasJaxrsAppRole;
        }
    }
}
