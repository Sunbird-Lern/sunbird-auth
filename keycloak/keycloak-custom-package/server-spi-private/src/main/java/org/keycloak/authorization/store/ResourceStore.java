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
package org.keycloak.authorization.store;

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;

import java.util.List;
import java.util.Map;

/**
 * A {@link ResourceStore} is responsible to manage the persistence of {@link Resource} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface ResourceStore {

    /**
     * <p>Creates a {@link Resource} instance backed by this persistent storage implementation.
     *
     * @param name the name of this resource. It must be unique.
     * @param resourceServer the resource server to where the given resource belongs to
     * @param owner the owner of this resource or null if the resource server is the owner
     * @return an instance backed by the underlying storage implementation
     */
    Resource create(String name, ResourceServer resourceServer, String owner);

    /**
     * Removes a {@link Resource} instance, with the given {@code id} from the persistent storage.
     *
     * @param id the identifier of an existing resource instance
     */
    void delete(String id);

    /**
     * Returns a {@link Resource} instance based on its identifier.
     *
     * @param id the identifier of an existing resource instance
     * @return the resource instance with the given identifier or null if no instance was found
     */
    Resource findById(String id, String resourceServerId);

    /**
     * Finds all {@link Resource} instances with the given {@code ownerId}.
     *
     * @param ownerId the identifier of the owner
     * @return a list with all resource instances owned by the given owner
     */
    List<Resource> findByOwner(String ownerId, String resourceServerId);

    /**
     * Finds all {@link Resource} instances with the given uri.
     *
     * @param uri the identifier of the uri
     * @return a list with all resource instances owned by the given owner
     */
    List<Resource> findByUri(String uri, String resourceServerId);

    /**
     * Finds all {@link Resource} instances associated with a given resource server.
     *
     * @param resourceServerId the identifier of the resource server
     * @return a list with all resources associated with the given resource server
     */
    List<Resource> findByResourceServer(String resourceServerId);

    /**
     * Finds all {@link Resource} instances associated with a given resource server.
     *
     * @param attributes a map holding the attributes that will be used as a filter
     * @param resourceServerId the identifier of the resource server
     * @return a list with all resources associated with the given resource server
     */
    List<Resource> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult);

    /**
     * Finds all {@link Resource} associated with a given scope.
     *
     * @param id one or more scope identifiers
     * @return a list of resources associated with the given scope(s)
     */
    List<Resource> findByScope(List<String> id, String resourceServerId);

    /**
     * Find a {@link Resource} by its name.
     *
     * @param name the name of the resource
     * @param resourceServerId the identifier of the resource server
     * @return a resource with the given name
     */
    Resource findByName(String name, String resourceServerId);

    /**
     * Finds all {@link Resource} with the given type.
     *
     * @param type the type of the resource
     * @return a list of resources with the given type
     */
    List<Resource> findByType(String type, String resourceServerId);
}
