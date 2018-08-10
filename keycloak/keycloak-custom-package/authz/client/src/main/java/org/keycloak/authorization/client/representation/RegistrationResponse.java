/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.client.representation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RegistrationResponse {

    private final ResourceRepresentation resourceDescription;

    public RegistrationResponse(ResourceRepresentation resourceDescription) {
        this.resourceDescription = resourceDescription;
    }

    public RegistrationResponse() {
        this(null);
    }

    @JsonUnwrapped
    public ResourceRepresentation getResourceDescription() {
        return this.resourceDescription;
    }

    public String getId() {
        if (this.resourceDescription != null) {
            return this.resourceDescription.getId();
        }

        return null;
    }
}
