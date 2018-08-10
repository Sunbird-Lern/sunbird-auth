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
package org.keycloak.authorization.jpa.store;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.authorization.jpa.entities.ScopeEntity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.jpa.JpaModel;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdapter implements Resource, JpaModel<ResourceEntity> {
    private ResourceEntity entity;
    private EntityManager em;
    private StoreFactory storeFactory;

    public ResourceAdapter(ResourceEntity entity, EntityManager em, StoreFactory storeFactory) {
        this.entity = entity;
        this.em = em;
        this.storeFactory = storeFactory;
    }

    @Override
    public ResourceEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);

    }

    @Override
    public String getUri() {
        return entity.getUri();
    }

    @Override
    public void setUri(String uri) {
        entity.setUri(uri);

    }

    @Override
    public String getType() {
        return entity.getType();
    }

    @Override
    public void setType(String type) {
        entity.setType(type);

    }

    @Override
    public List<Scope> getScopes() {
        List<Scope> scopes = new LinkedList<>();
        for (ScopeEntity scope : entity.getScopes()) {
            scopes.add(storeFactory.getScopeStore().findById(scope.getId(), entity.getResourceServer().getId()));
        }

        return Collections.unmodifiableList(scopes);
    }

    @Override
    public String getIconUri() {
        return entity.getIconUri();
    }

    @Override
    public void setIconUri(String iconUri) {
        entity.setIconUri(iconUri);

    }

    @Override
    public ResourceServer getResourceServer() {
        return storeFactory.getResourceServerStore().findById(entity.getResourceServer().getId());
    }

    @Override
    public String getOwner() {
        return entity.getOwner();
    }

    @Override
    public void updateScopes(Set<Scope> toUpdate) {
        Set<String> ids = new HashSet<>();
        for (Scope scope : toUpdate) {
            ids.add(scope.getId());
        }
        Iterator<ScopeEntity> it = entity.getScopes().iterator();
        while (it.hasNext()) {
            ScopeEntity next = it.next();
            if (!ids.contains(next.getId())) it.remove();
            else ids.remove(next.getId());
        }
        for (String addId : ids) {
            entity.getScopes().add(em.getReference(ScopeEntity.class, addId));
        }
    }


    public static ResourceEntity toEntity(EntityManager em, Resource resource) {
        if (resource instanceof ResourceAdapter) {
            return ((ResourceAdapter)resource).getEntity();
        } else {
            return em.getReference(ResourceEntity.class, resource.getId());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Resource)) return false;

        Resource that = (Resource) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }


}
