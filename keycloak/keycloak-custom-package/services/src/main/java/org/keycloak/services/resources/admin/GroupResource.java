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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

/**
 * @resource Groups
 * @author Bill Burke
 */
public class GroupResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final GroupModel group;

    public GroupResource(RealmModel realm, GroupModel group, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.GROUP);
        this.group = group;
    }

    @Context private UriInfo uriInfo;

     /**
     *
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GroupRepresentation getGroup() {
        this.auth.groups().requireView(group);

        GroupRepresentation rep = ModelToRepresentation.toGroupHierarchy(group, true);

        rep.setAccess(auth.groups().getAccess(group));

        return rep;
    }

    /**
     * Update group, ignores subgroups.
     *
     * @param rep
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateGroup(GroupRepresentation rep) {
        this.auth.groups().requireManage(group);

        updateGroup(rep, group);
        adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(rep).success();


    }

    @DELETE
    public void deleteGroup() {
        this.auth.groups().requireManage(group);

        realm.removeGroup(group);
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }


    /**
     * Set or create child.  This will just set the parent if it exists.  Create it and set the parent
     * if the group doesn't exist.
     *
     * @param rep
     */
    @POST
    @Path("children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addChild(GroupRepresentation rep) {
        this.auth.groups().requireManage(group);

        for (GroupModel group : group.getSubGroups()) {
            if (group.getName().equals(rep.getName())) {
                return ErrorResponse.exists("Parent already contains subgroup named '" + rep.getName() + "'");
            }
        }

        Response.ResponseBuilder builder = Response.status(204);
        GroupModel child = null;
        if (rep.getId() != null) {
            child = realm.getGroupById(rep.getId());
            if (child == null) {
                throw new NotFoundException("Could not find child by id");
            }
            adminEvent.operation(OperationType.UPDATE);
        } else {
            child = realm.createGroup(rep.getName());
            updateGroup(rep, child);
            URI uri = uriInfo.getBaseUriBuilder()
                                           .path(uriInfo.getMatchedURIs().get(2))
                                           .path(child.getId()).build();
            builder.status(201).location(uri);
            rep.setId(child.getId());
            adminEvent.operation(OperationType.CREATE);

        }
        realm.moveGroup(child, group);
        adminEvent.resourcePath(uriInfo).representation(rep).success();

        GroupRepresentation childRep = ModelToRepresentation.toGroupHierarchy(child, true);
        return builder.type(MediaType.APPLICATION_JSON_TYPE).entity(childRep).build();
    }

    public static void updateGroup(GroupRepresentation rep, GroupModel model) {
        if (rep.getName() != null) model.setName(rep.getName());

        if (rep.getAttributes() != null) {
            Set<String> attrsToRemove = new HashSet<>(model.getAttributes().keySet());
            attrsToRemove.removeAll(rep.getAttributes().keySet());
            for (Map.Entry<String, List<String>> attr : rep.getAttributes().entrySet()) {
                model.setAttribute(attr.getKey(), attr.getValue());
            }

            for (String attr : attrsToRemove) {
                model.removeAttribute(attr);
            }
        }
    }

    @Path("role-mappings")
    public RoleMapperResource getRoleMappings() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.groups().requireManage(group);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.groups().requireView(group);
        RoleMapperResource resource =  new RoleMapperResource(realm, auth, group, adminEvent, manageCheck, viewCheck);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;

    }

    /**
     * Get users
     *
     * Returns a list of users, filtered according to query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @return
     */
    @GET
    @NoCache
    @Path("members")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserRepresentation> getMembers(@QueryParam("first") Integer firstResult,
                                               @QueryParam("max") Integer maxResults) {
        this.auth.groups().requireViewMembers(group);


        firstResult = firstResult != null ? firstResult : 0;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        List<UserRepresentation> results = new ArrayList<UserRepresentation>();
        List<UserModel> userModels = session.users().getGroupMembers(realm, group, firstResult, maxResults);

        for (UserModel user : userModels) {
            results.add(ModelToRepresentation.toRepresentation(session, realm, user));
        }
        return results;
    }

    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     * @return
     */
    @Path("management/permissions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ManagementPermissionReference getManagementPermissions() {
        auth.groups().requireView(group);

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (!permissions.groups().isPermissionsEnabled(group)) {
            return new ManagementPermissionReference();
        }
        return toMgmtRef(group, permissions);
    }

    public static ManagementPermissionReference toMgmtRef(GroupModel group, AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.groups().resource(group).getId());
        ref.setScopePermissions(permissions.groups().getPermissions(group));
        return ref;
    }


    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     *
     * @return initialized manage permissions reference
     */
    @Path("management/permissions")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public ManagementPermissionReference setManagementPermissionsEnabled(ManagementPermissionReference ref) {
        auth.groups().requireManage(group);
        if (ref.isEnabled()) {
            AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
            permissions.groups().setPermissionsEnabled(group, ref.isEnabled());
            return toMgmtRef(group, permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }

}

