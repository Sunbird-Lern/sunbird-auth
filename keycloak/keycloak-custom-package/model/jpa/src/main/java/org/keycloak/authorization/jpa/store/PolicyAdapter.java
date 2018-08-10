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
import org.keycloak.authorization.jpa.entities.PolicyEntity;
import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.authorization.jpa.entities.ScopeEntity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PolicyAdapter implements Policy, JpaModel<PolicyEntity> {
    private PolicyEntity entity;
    private EntityManager em;
    private StoreFactory storeFactory;

    public PolicyAdapter(PolicyEntity entity, EntityManager em, StoreFactory storeFactory) {
        this.entity = entity;
        this.em = em;
        this.storeFactory = storeFactory;
    }

    @Override
    public PolicyEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getType() {
        return entity.getType();
    }

    @Override
    public DecisionStrategy getDecisionStrategy() {
        return entity.getDecisionStrategy();
    }

    @Override
    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        entity.setDecisionStrategy(decisionStrategy);

    }

    @Override
    public Logic getLogic() {
        return entity.getLogic();
    }

    @Override
    public void setLogic(Logic logic) {
        entity.setLogic(logic);
    }

    @Override
    public Map<String, String> getConfig() {
        Map<String, String> result = new HashMap<String, String>();
        if (entity.getConfig() != null) result.putAll(entity.getConfig());
        return Collections.unmodifiableMap(result);
    }

    @Override
    public void setConfig(Map<String, String> config) {
        if (entity.getConfig() == null) {
            entity.setConfig(new HashMap<>());
        } else {
            entity.getConfig().clear();
        }
        entity.getConfig().putAll(config);
    }

    @Override
    public void removeConfig(String name) {
        if (entity.getConfig() == null) {
            return;
        }
        entity.getConfig().remove(name);
    }

    @Override
    public void putConfig(String name, String value) {
        if (entity.getConfig() == null) {
            entity.setConfig(new HashMap<>());
        }
        entity.getConfig().put(name, value);

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
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        entity.setDescription(description);

    }

    @Override
    public ResourceServer getResourceServer() {
        return storeFactory.getResourceServerStore().findById(entity.getResourceServer().getId());
    }

    @Override
    public Set<Policy> getAssociatedPolicies() {
        Set<Policy> result = new HashSet<>();
        for (PolicyEntity policy : entity.getAssociatedPolicies()) {
            Policy p = storeFactory.getPolicyStore().findById(policy.getId(), entity.getResourceServer().getId());
            result.add(p);
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Set<Resource> getResources() {
        Set<Resource> set = new HashSet<>();
        for (ResourceEntity res : entity.getResources()) {
            set.add(storeFactory.getResourceStore().findById(res.getId(), entity.getResourceServer().getId()));
        }
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Set<Scope> getScopes() {
        Set<Scope> set = new HashSet<>();
        for (ScopeEntity res : entity.getScopes()) {
            set.add(storeFactory.getScopeStore().findById(res.getId(), entity.getResourceServer().getId()));
        }
        return Collections.unmodifiableSet(set);
    }

    @Override
    public void addScope(Scope scope) {
        entity.getScopes().add(ScopeAdapter.toEntity(em, scope));
    }

    @Override
    public void removeScope(Scope scope) {
        entity.getScopes().remove(ScopeAdapter.toEntity(em, scope));

    }

    @Override
    public void addAssociatedPolicy(Policy associatedPolicy) {
        entity.getAssociatedPolicies().add(toEntity(em, associatedPolicy));
    }

    @Override
    public void removeAssociatedPolicy(Policy associatedPolicy) {
        entity.getAssociatedPolicies().remove(toEntity(em, associatedPolicy));

    }

    @Override
    public void addResource(Resource resource) {
        entity.getResources().add(ResourceAdapter.toEntity(em, resource));
    }

    @Override
    public void removeResource(Resource resource) {
        entity.getResources().remove(ResourceAdapter.toEntity(em, resource));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Policy)) return false;

        Policy that = (Policy) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public static PolicyEntity toEntity(EntityManager em, Policy policy) {
        if (policy instanceof PolicyAdapter) {
            return ((PolicyAdapter)policy).getEntity();
        } else {
            return em.getReference(PolicyEntity.class, policy.getId());
        }
    }



}
