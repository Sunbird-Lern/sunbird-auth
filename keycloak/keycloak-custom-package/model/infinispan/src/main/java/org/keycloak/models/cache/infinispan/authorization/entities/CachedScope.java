/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.keycloak.models.cache.infinispan.authorization.entities;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedScope extends AbstractRevisioned implements InResourceServer {

    private String resourceServerId;
    private String name;
    private String iconUri;

    public CachedScope(Long revision, Scope scope) {
        super(revision, scope.getId());
        this.name = scope.getName();
        this.iconUri = scope.getIconUri();
        this.resourceServerId = scope.getResourceServer().getId();
    }

    public String getName() {
        return this.name;
    }

    public String getIconUri() {
        return this.iconUri;
    }

    @Override
    public String getResourceServerId() {
        return this.resourceServerId;
    }

}
