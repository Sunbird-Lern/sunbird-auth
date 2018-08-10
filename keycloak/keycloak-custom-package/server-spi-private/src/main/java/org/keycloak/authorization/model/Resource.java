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

package org.keycloak.authorization.model;

import java.util.List;
import java.util.Set;

/**
 * Represents a resource, which is usually protected by a set of policies within a resource server.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface Resource {

    /**
     * Returns the unique identifier for this instance.
     *
     * @return the unique identifier for this instance
     */
    String getId();

    /**
     * Returns the resource's name.
     *
     * @return the name of this resource
     */
    String getName();

    /**
     * Sets a name for this resource. The name must be unique.
     *
     * @param name the name of this resource
     */
    void setName(String name);

    /**
     * Returns a {@link java.net.URI} that uniquely identify this resource.
     *
     * @return an {@link java.net.URI} for this resource or null if not defined.
     */
    String getUri();

    /**
     * Sets a {@link java.net.URI} that uniquely identify this resource.
     *
     * @param uri an {@link java.net.URI} for this resource
     */
    void setUri(String uri);

    /**
     * Returns a string representing the type of this resource.
     *
     * @return the type of this resource or null if not defined
     */
    String getType();

    /**
     * Sets a string representing the type of this resource.
     *
     * @return the type of this resource or null if not defined
     */
    void setType(String type);

    /**
     * Returns a {@link List} containing all the {@link Scope} associated with this resource.
     *
     * @return a list with all scopes associated with this resource
     */
     List<Scope> getScopes();

    /**
     * Returns an icon {@link java.net.URI} for this resource.
     *
     * @return a uri for an icon
     */
    String getIconUri();

    /**
     * Sets an icon {@link java.net.URI} for this resource.
     *
     * @return a uri for an icon
     */
    void setIconUri(String iconUri);

    /**
     * Returns the {@link ResourceServer} to where this resource belongs to.
     *
     * @return the resource server associated with this resource
     */
     ResourceServer getResourceServer();

    /**
     * Returns the resource's owner, which is usually an identifier that uniquely identifies the resource's owner.
     *
     * @return the owner of this resource
     */
    String getOwner();

    void updateScopes(Set<Scope> scopes);
}
