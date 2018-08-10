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
package org.keycloak.representations.idm.authorization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AbstractPolicyRepresentation {

    private String id;
    private String name;
    private String description;
    private String type;
    private Set<String> policies;
    private Set<String> resources;
    private Set<String> scopes;
    private Logic logic = Logic.POSITIVE;
    private DecisionStrategy decisionStrategy = DecisionStrategy.UNANIMOUS;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DecisionStrategy getDecisionStrategy() {
        return this.decisionStrategy;
    }

    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        this.decisionStrategy = decisionStrategy;
    }

    public Logic getLogic() {
        return logic;
    }

    public void setLogic(Logic logic) {
        this.logic = logic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getPolicies() {
        return policies;
    }

    public void setPolicies(Set<String> policies) {
        this.policies = policies;
    }

    public void addPolicy(String... id) {
        if (this.policies == null) {
            this.policies = new HashSet<>();
        }
        this.policies.addAll(Arrays.asList(id));
    }

    public Set<String> getResources() {
        return resources;
    }

    public void setResources(Set<String> resources) {
        this.resources = resources;
    }

    public void addResource(String id) {
        if (this.resources == null) {
            this.resources = new HashSet<>();
        }
        this.resources.add(id);
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public void addScope(String... id) {
        if (this.scopes == null) {
            this.scopes = new HashSet<>();
        }
        this.scopes.addAll(Arrays.asList(id));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractPolicyRepresentation policy = (AbstractPolicyRepresentation) o;
        return Objects.equals(getId(), policy.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}