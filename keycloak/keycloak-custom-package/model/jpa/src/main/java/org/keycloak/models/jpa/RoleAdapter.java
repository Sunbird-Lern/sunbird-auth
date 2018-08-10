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

package org.keycloak.models.jpa;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleAdapter implements RoleModel, JpaModel<RoleEntity> {
    protected RoleEntity role;
    protected EntityManager em;
    protected RealmModel realm;
    protected KeycloakSession session;

    public RoleAdapter(KeycloakSession session, RealmModel realm, EntityManager em, RoleEntity role) {
        this.em = em;
        this.realm = realm;
        this.role = role;
        this.session = session;
    }

    public RoleEntity getEntity() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    @Override
    public String getName() {
        return role.getName();
    }

    @Override
    public String getDescription() {
        return role.getDescription();
    }

    @Override
    public void setDescription(String description) {
        role.setDescription(description);
    }

    @Override
    public boolean isScopeParamRequired() {
        return role.isScopeParamRequired();
    }

    @Override
    public void setScopeParamRequired(boolean scopeParamRequired) {
        role.setScopeParamRequired(scopeParamRequired);
    }

    @Override
    public String getId() {
        return role.getId();
    }

    @Override
    public void setName(String name) {
        role.setName(name);
    }

    @Override
    public boolean isComposite() {
        return getComposites().size() > 0;
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        RoleEntity entity = RoleAdapter.toRoleEntity(role, em);
        for (RoleEntity composite : getEntity().getCompositeRoles()) {
            if (composite.equals(entity)) return;
        }
        getEntity().getCompositeRoles().add(entity);
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        RoleEntity entity = RoleAdapter.toRoleEntity(role, em);
        getEntity().getCompositeRoles().remove(entity);
    }

    @Override
    public Set<RoleModel> getComposites() {
        Set<RoleModel> set = new HashSet<RoleModel>();

        for (RoleEntity composite : getEntity().getCompositeRoles()) {
            set.add(new RoleAdapter(session, realm, em, composite));

            // todo I want to do this, but can't as you get stack overflow
            // set.add(session.realms().getRoleById(composite.getId(), realm));
        }
        return set;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return this.equals(role) || KeycloakModelUtils.searchFor(role, this, new HashSet<>());
    }

    @Override
    public boolean isClientRole() {
        return role.isClientRole();
    }

    @Override
    public String getContainerId() {
        if (isClientRole()) return role.getClient().getId();
        else return realm.getId();
    }


    @Override
    public RoleContainerModel getContainer() {
        if (role.isClientRole()) {
            return realm.getClientById(role.getClient().getId());

        } else {
            return realm;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RoleModel)) return false;

        RoleModel that = (RoleModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public static RoleEntity toRoleEntity(RoleModel model, EntityManager em) {
        if (model instanceof RoleAdapter) {
            return ((RoleAdapter)model).getEntity();
        }
        return em.getReference(RoleEntity.class, model.getId());
    }
}
