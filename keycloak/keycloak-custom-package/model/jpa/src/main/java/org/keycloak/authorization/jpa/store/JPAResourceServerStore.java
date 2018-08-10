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
package org.keycloak.authorization.jpa.store;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.jpa.entities.PolicyEntity;
import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.authorization.jpa.entities.ResourceServerEntity;
import org.keycloak.authorization.jpa.entities.ScopeEntity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAResourceServerStore implements ResourceServerStore {

    private final EntityManager entityManager;
    private final AuthorizationProvider provider;

    public JPAResourceServerStore(EntityManager entityManager, AuthorizationProvider provider) {
        this.entityManager = entityManager;
        this.provider = provider;
    }

    @Override
    public ResourceServer create(String clientId) {
        ResourceServerEntity entity = new ResourceServerEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setClientId(clientId);

        this.entityManager.persist(entity);

        return new ResourceServerAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public void delete(String id) {
        ResourceServerEntity entity = entityManager.find(ResourceServerEntity.class, id);
        if (entity == null) return;
        //This didn't work, had to loop through and remove each policy individually
        //entityManager.createNamedQuery("deletePolicyByResourceServer")
        //        .setParameter("serverId", id).executeUpdate();

        {
            TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByServerId", String.class);
            query.setParameter("serverId", id);
            List<String> result = query.getResultList();
            for (String policyId : result) {
                entityManager.remove(entityManager.getReference(PolicyEntity.class, policyId));
            }
        }

        //entityManager.createNamedQuery("deleteResourceByResourceServer")
        //        .setParameter("serverId", id).executeUpdate();
        {
            TypedQuery<String> query = entityManager.createNamedQuery("findResourceIdByServerId", String.class);

            query.setParameter("serverId", id);

            List<String> result = query.getResultList();
            List<Resource> list = new LinkedList<>();
            for (String resourceId : result) {
                entityManager.remove(entityManager.getReference(ResourceEntity.class, resourceId));
            }
        }

        //entityManager.createNamedQuery("deleteScopeByResourceServer")
        //        .setParameter("serverId", id).executeUpdate();
        {
            TypedQuery<String> query = entityManager.createNamedQuery("findScopeIdByResourceServer", String.class);

            query.setParameter("serverId", id);

            List<String> result = query.getResultList();
            for (String scopeId : result) {
                entityManager.remove(entityManager.getReference(ScopeEntity.class, scopeId));
            }
        }

        this.entityManager.remove(entity);
        entityManager.flush();
        entityManager.detach(entity);
    }

    @Override
    public ResourceServer findById(String id) {
        ResourceServerEntity entity = entityManager.find(ResourceServerEntity.class, id);
        if (entity == null) return null;
        return new ResourceServerAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public ResourceServer findByClient(final String clientId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findResourceServerIdByClient", String.class);

        query.setParameter("clientId", clientId);
        try {
            String id = query.getSingleResult();
            return provider.getStoreFactory().getResourceServerStore().findById(id);
        } catch (NoResultException ex) {
            return null;
        }
    }
}
