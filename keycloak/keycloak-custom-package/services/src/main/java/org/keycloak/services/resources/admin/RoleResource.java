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

package org.keycloak.services.resources.admin;

import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @resource Roles
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class RoleResource {
    protected RealmModel realm;

    public RoleResource(RealmModel realm) {
        this.realm = realm;
    }

    protected RoleRepresentation getRole(RoleModel roleModel) {
        return ModelToRepresentation.toRepresentation(roleModel);
    }

    protected void deleteRole(RoleModel role) {
        if (!role.getContainer().removeRole(role)) {
            throw new NotFoundException("Role not found");
        }
    }

    protected void updateRole(RoleRepresentation rep, RoleModel role) {
        role.setName(rep.getName());
        role.setDescription(rep.getDescription());
        if (rep.isScopeParamRequired() != null) role.setScopeParamRequired(rep.isScopeParamRequired());
    }

    protected void addComposites(AdminPermissionEvaluator auth, AdminEventBuilder adminEvent, UriInfo uriInfo, List<RoleRepresentation> roles, RoleModel role) {
        for (RoleRepresentation rep : roles) {
            RoleModel composite = realm.getRoleById(rep.getId());
            if (composite == null) {
                throw new NotFoundException("Could not find composite role");
            }
            auth.roles().requireMapComposite(composite);
            role.addCompositeRole(composite);
        }

        if (role.isClientRole()) {
            adminEvent.resource(ResourceType.CLIENT_ROLE);
        } else {
            adminEvent.resource(ResourceType.REALM_ROLE);
        }

        adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo).representation(roles).success();
    }

    protected Set<RoleRepresentation> getRoleComposites(RoleModel role) {
        if (!role.isComposite() || role.getComposites().size() == 0) return Collections.emptySet();

        Set<RoleRepresentation> composites = new HashSet<RoleRepresentation>(role.getComposites().size());
        for (RoleModel composite : role.getComposites()) {
            composites.add(ModelToRepresentation.toRepresentation(composite));
        }
        return composites;
    }

    protected Set<RoleRepresentation> getRealmRoleComposites(RoleModel role) {
        if (!role.isComposite() || role.getComposites().size() == 0) return Collections.emptySet();

        Set<RoleRepresentation> composites = new HashSet<RoleRepresentation>(role.getComposites().size());
        for (RoleModel composite : role.getComposites()) {
            if (composite.getContainer() instanceof RealmModel)
                composites.add(ModelToRepresentation.toRepresentation(composite));
        }
        return composites;
    }

    protected Set<RoleRepresentation> getClientRoleComposites(ClientModel app, RoleModel role) {
        if (!role.isComposite() || role.getComposites().size() == 0) return Collections.emptySet();

        Set<RoleRepresentation> composites = new HashSet<RoleRepresentation>(role.getComposites().size());
        for (RoleModel composite : role.getComposites()) {
            if (composite.getContainer().equals(app))
                composites.add(ModelToRepresentation.toRepresentation(composite));
        }
        return composites;
    }

    protected void deleteComposites(AdminEventBuilder adminEvent, UriInfo uriInfo, List<RoleRepresentation> roles, RoleModel role) {
        for (RoleRepresentation rep : roles) {
            RoleModel composite = realm.getRoleById(rep.getId());
            if (composite == null) {
                throw new NotFoundException("Could not find composite role");
            }
            role.removeCompositeRole(composite);
        }

        if (role.isClientRole()) {
            adminEvent.resource(ResourceType.CLIENT_ROLE);
        } else {
            adminEvent.resource(ResourceType.REALM_ROLE);
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).representation(roles).success();
    }
}
