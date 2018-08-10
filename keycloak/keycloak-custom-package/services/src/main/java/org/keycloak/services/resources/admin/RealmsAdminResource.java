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

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Top level resource for Admin REST API
 *
 * @resource Realms Admin
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmsAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmsAdminResource.class);
    protected AdminAuth auth;
    protected TokenManager tokenManager;

    @Context
    protected KeycloakSession session;

    @Context
    protected KeycloakApplication keycloak;

    @Context
    protected ClientConnection clientConnection;

    public RealmsAdminResource(AdminAuth auth, TokenManager tokenManager) {
        this.auth = auth;
        this.tokenManager = tokenManager;
    }

    public static final CacheControl noCache = new CacheControl();

    static {
        noCache.setNoCache(true);
    }

    /**
     * Get accessible realms
     *
     * Returns a list of accessible realms. The list is filtered based on what realms the caller is allowed to view.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<RealmRepresentation> getRealms() {
        List<RealmRepresentation> reps = new ArrayList<RealmRepresentation>();
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {
            addRealmRep(reps, realm);
        }
        if (reps.isEmpty()) {
            throw new ForbiddenException();
        }

        logger.debug(("getRealms()"));
        return reps;
    }

    protected void addRealmRep(List<RealmRepresentation> reps, RealmModel realm) {
        if (AdminPermissions.realms(session, auth).canView(realm)) {
            reps.add(ModelToRepresentation.toRepresentation(realm, false));
        } else if (AdminPermissions.realms(session, auth).isAdmin(realm)) {
            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());
            reps.add(rep);
        }
    }

    /**
     * Import a realm
     *
     * Imports a realm from a full representation of that realm.  Realm name must be unique.
     *
     * @param uriInfo
     * @param rep JSON representation of the realm
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importRealm(@Context final UriInfo uriInfo, final RealmRepresentation rep) {
        RealmManager realmManager = new RealmManager(session);
        realmManager.setContextPath(keycloak.getContextPath());
        AdminPermissions.realms(session, auth).requireCreateRealm();

        logger.debugv("importRealm: {0}", rep.getRealm());

        try {
            RealmModel realm = realmManager.importRealm(rep);
            grantPermissionsToRealmCreator(realm);

            URI location = AdminRoot.realmsUrl(uriInfo).path(realm.getName()).build();
            logger.debugv("imported realm success, sending back: {0}", location.toString());

            return Response.created(location).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Realm with same name exists");
        }
    }

    private void grantPermissionsToRealmCreator(RealmModel realm) {
        if (auth.hasRealmRole(AdminRoles.ADMIN)) {
            return;
        }

        RealmModel adminRealm = new RealmManager(session).getKeycloakAdminstrationRealm();
        ClientModel realmAdminApp = realm.getMasterAdminClient();
        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel role = realmAdminApp.getRole(r);
            auth.getUser().grantRole(role);
        }
    }

    /**
     * Base path for the admin REST API for one particular realm.
     *
     * @param headers
     * @param name realm name (not id!)
     * @return
     */
    @Path("{realm}")
    public RealmAdminResource getRealmAdmin(@Context final HttpHeaders headers,
                                            @PathParam("realm") final String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) throw new NotFoundException("Realm not found.");

        if (!auth.getRealm().equals(realmManager.getKeycloakAdminstrationRealm())
                && !auth.getRealm().equals(realm)) {
            throw new ForbiddenException();
        }
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);

        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session, clientConnection);
        session.getContext().setRealm(realm);

        RealmAdminResource adminResource = new RealmAdminResource(realmAuth, realm, tokenManager, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(adminResource);
        //resourceContext.initResource(adminResource);
        return adminResource;
    }

}
