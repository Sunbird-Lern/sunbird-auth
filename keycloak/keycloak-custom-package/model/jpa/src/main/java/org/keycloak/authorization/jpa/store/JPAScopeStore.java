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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.jpa.entities.ResourceServerEntity;
import org.keycloak.authorization.jpa.entities.ScopeEntity;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAScopeStore implements ScopeStore {

    private final EntityManager entityManager;
    private final AuthorizationProvider provider;

    public JPAScopeStore(EntityManager entityManager, AuthorizationProvider provider) {
        this.entityManager = entityManager;
        this.provider = provider;
    }

    @Override
    public Scope create(final String name, final ResourceServer resourceServer) {
        ScopeEntity entity = new ScopeEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setName(name);
        entity.setResourceServer(ResourceServerAdapter.toEntity(entityManager, resourceServer));

        this.entityManager.persist(entity);

        return new ScopeAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public void delete(String id) {
        ScopeEntity scope = entityManager.find(ScopeEntity.class, id);

        if (scope != null) {
            this.entityManager.remove(scope);
        }
    }

    @Override
    public Scope findById(String id, String resourceServerId) {
        if (id == null) {
            return null;
        }

        ScopeEntity entity = entityManager.find(ScopeEntity.class, id);
        if (entity == null) return null;

        return new ScopeAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public Scope findByName(String name, String resourceServerId) {
        try {
            TypedQuery<String> query = entityManager.createNamedQuery("findScopeIdByName", String.class);

            query.setParameter("serverId", resourceServerId);
            query.setParameter("name", name);
            String id = query.getSingleResult();
            return provider.getStoreFactory().getScopeStore().findById(id, resourceServerId);
        } catch (NoResultException nre) {
            return null;
        }
    }

    @Override
    public List<Scope> findByResourceServer(final String serverId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findScopeIdByResourceServer", String.class);

        query.setParameter("serverId", serverId);

        List<String> result = query.getResultList();
        List<Scope> list = new LinkedList<>();
        for (String id : result) {
            list.add(provider.getStoreFactory().getScopeStore().findById(id, serverId));
        }
        return list;
    }

    @Override
    public List<Scope> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ScopeEntity> querybuilder = builder.createQuery(ScopeEntity.class);
        Root<ScopeEntity> root = querybuilder.from(ScopeEntity.class);
        querybuilder.select(root.get("id"));
        List<Predicate> predicates = new ArrayList();

        predicates.add(builder.equal(root.get("resourceServer").get("id"), resourceServerId));

        attributes.forEach((name, value) -> {
            if ("id".equals(name)) {
                predicates.add(root.get(name).in(value));
            } else {
                predicates.add(builder.like(builder.lower(root.get(name)), "%" + value[0].toLowerCase() + "%"));
            }
        });

        querybuilder.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(root.get("name")));

        Query query = entityManager.createQuery(querybuilder);

        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResult != -1) {
            query.setMaxResults(maxResult);
        }

        List result = query.getResultList();
        List<Scope> list = new LinkedList<>();
        for (Object id : result) {
            list.add(provider.getStoreFactory().getScopeStore().findById((String)id, resourceServerId));
        }
        return list;

    }
}
