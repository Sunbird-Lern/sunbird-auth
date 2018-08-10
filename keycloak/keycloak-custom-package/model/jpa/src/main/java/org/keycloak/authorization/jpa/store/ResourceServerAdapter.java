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
import org.keycloak.authorization.jpa.entities.ResourceServerEntity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

import javax.persistence.EntityManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceServerAdapter implements ResourceServer, JpaModel<ResourceServerEntity> {
    private ResourceServerEntity entity;
    private EntityManager em;
    private StoreFactory storeFactory;

    public ResourceServerAdapter(ResourceServerEntity entity, EntityManager em, StoreFactory storeFactory) {
        this.entity = entity;
        this.em = em;
        this.storeFactory = storeFactory;
    }

    @Override
    public ResourceServerEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getClientId() {
        return entity.getClientId();
    }

    @Override
    public boolean isAllowRemoteResourceManagement() {
        return entity.isAllowRemoteResourceManagement();
    }

    @Override
    public void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement) {
        entity.setAllowRemoteResourceManagement(allowRemoteResourceManagement);

    }

    @Override
    public PolicyEnforcementMode getPolicyEnforcementMode() {
        return entity.getPolicyEnforcementMode();
    }

    @Override
    public void setPolicyEnforcementMode(PolicyEnforcementMode enforcementMode) {
        entity.setPolicyEnforcementMode(enforcementMode);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ResourceServer)) return false;

        ResourceServer that = (ResourceServer) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public static ResourceServerEntity toEntity(EntityManager em, ResourceServer resource) {
        if (resource instanceof ResourceAdapter) {
            return ((ResourceServerAdapter)resource).getEntity();
        } else {
            return em.getReference(ResourceServerEntity.class, resource.getId());
        }
    }


}
