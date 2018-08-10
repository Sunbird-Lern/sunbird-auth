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
package org.keycloak.services.resources.admin.permissions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.services.ForbiddenException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class RolePermissions implements RolePermissionEvaluator, RolePermissionManagement {
    private static final Logger logger = Logger.getLogger(RolePermissions.class);
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AuthorizationProvider authz;
    protected final MgmtPermissions root;

    public RolePermissions(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        this.session = session;
        this.realm = realm;
        this.authz = authz;
        this.root = root;
    }

    @Override
    public boolean isPermissionsEnabled(RoleModel role) {
        return mapRolePermission(role) != null;
    }

    @Override
    public void setPermissionsEnabled(RoleModel role, boolean enable) {
       if (enable) {
           initialize(role);
       } else {
           disablePermissions(role);
       }
    }

    private void disablePermissions(RoleModel role) {
        ResourceServer server = resourceServer(role);
        if (server == null) return;
        Policy policy = mapRolePermission(role);
        if (policy != null) authz.getStoreFactory().getPolicyStore().delete(policy.getId());
        policy = mapClientScopePermission(role);
        if (policy != null) authz.getStoreFactory().getPolicyStore().delete(policy.getId());
        policy = mapCompositePermission(role);
        if (policy != null) authz.getStoreFactory().getPolicyStore().delete(policy.getId());

        Resource resource = authz.getStoreFactory().getResourceStore().findByName(getRoleResourceName(role), server.getId());
        if (resource != null) authz.getStoreFactory().getResourceStore().delete(resource.getId());
    }

    @Override
    public Map<String, String> getPermissions(RoleModel role) {
        Map<String, String> scopes = new HashMap<>();
        scopes.put(RolePermissionManagement.MAP_ROLE_SCOPE, mapRolePermission(role).getId());
        scopes.put(RolePermissionManagement.MAP_ROLE_CLIENT_SCOPE_SCOPE, mapClientScopePermission(role).getId());
        scopes.put(RolePermissionManagement.MAP_ROLE_COMPOSITE_SCOPE, mapCompositePermission(role).getId());
        return scopes;
    }

    @Override
    public Policy mapRolePermission(RoleModel role) {
        ResourceServer server = resourceServer(role);
        if (server == null) return null;
        return  authz.getStoreFactory().getPolicyStore().findByName(getMapRolePermissionName(role), server.getId());
    }

    @Override
    public Policy mapCompositePermission(RoleModel role) {
        ResourceServer server = resourceServer(role);
        if (server == null) return null;

        return  authz.getStoreFactory().getPolicyStore().findByName(getMapCompositePermissionName(role), server.getId());
    }

    @Override
    public Policy mapClientScopePermission(RoleModel role) {
        ResourceServer server = resourceServer(role);
        if (server == null) return null;

        return  authz.getStoreFactory().getPolicyStore().findByName(getMapClientScopePermissionName(role), server.getId());
    }

    @Override
    public Resource resource(RoleModel role) {
        ResourceStore resourceStore = authz.getStoreFactory().getResourceStore();
        ResourceServer server = resourceServer(role);
        if (server == null) return null;
        return  resourceStore.findByName(getRoleResourceName(role), server.getId());
    }

    @Override
    public ResourceServer resourceServer(RoleModel role) {
        ClientModel client = getRoleClient(role);
        return root.resourceServer(client);
    }

    private boolean checkAdminRoles(RoleModel role) {
        if (AdminRoles.ALL_ROLES.contains(role.getName())) {
            if (root.admin().hasRole(role)) return true;

            ClientModel adminClient = root.getRealmManagementClient();
            if (adminClient.equals(role.getContainer())) {
                // if this is realm admin role, then check to see if admin has similar permissions
                // we do this so that the authz service is invoked
                if (role.getName().equals(AdminRoles.MANAGE_CLIENTS)) {
                    if (!root.clients().canManage()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.VIEW_CLIENTS)) {
                    if (!root.clients().canView()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.QUERY_CLIENTS)) {
                    return true;
                } else if (role.getName().equals(AdminRoles.QUERY_USERS)) {
                    return true;
                } else if (role.getName().equals(AdminRoles.QUERY_GROUPS)) {
                    return true;
                } else if (role.getName().equals(AdminRoles.MANAGE_AUTHORIZATION)) {
                    if (!root.realm().canManageAuthorization()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.VIEW_AUTHORIZATION)) {
                    if (!root.realm().canViewAuthorization()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.MANAGE_EVENTS)) {
                    if (!root.realm().canManageEvents()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.VIEW_EVENTS)) {
                    if (!root.realm().canViewEvents()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.MANAGE_USERS)) {
                    if (!root.users().canManage()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.VIEW_USERS)) {
                    if (!root.users().canView()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.MANAGE_IDENTITY_PROVIDERS)) {
                    if (!root.realm().canManageIdentityProviders()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.VIEW_IDENTITY_PROVIDERS)) {
                    if (!root.realm().canViewIdentityProviders()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.MANAGE_REALM)) {
                    if (!root.realm().canManageRealm()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(AdminRoles.VIEW_REALM)) {
                    if (!root.realm().canViewRealm()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else if (role.getName().equals(ImpersonationConstants.IMPERSONATION_ROLE)) {
                    if (!root.users().canImpersonate()) {
                        return adminConflictMessage(role);
                    } else {
                        return true;
                    }
                } else {
                    return adminConflictMessage(role);
                }

            } else {
                // now we need to check to see if this is a master admin role
                if (role.getContainer() instanceof RealmModel) {
                    RealmModel realm = (RealmModel)role.getContainer();
                    // If realm role is master admin role then abort
                    if (realm.getName().equals(Config.getAdminRealm())) {
                        return adminConflictMessage(role);
                    }
                } else {
                    ClientModel container = (ClientModel)role.getContainer();
                    // abort if this is an role in master realm and role is an admin role of any realm
                    if (container.getRealm().getName().equals(Config.getAdminRealm())
                            && container.getClientId().endsWith("-realm")) {
                        return adminConflictMessage(role);
                    }
                }
                return true;
            }

        }
        return true;

    }

    private boolean adminConflictMessage(RoleModel role) {
        logger.debug("Trying to assign admin privileges of role: " + role.getName() + " but admin doesn't have same privilege");
        return false;
    }

    /**
     * Is admin allowed to map this role?
     *
     * @param role
     * @return
     */
    @Override
    public boolean canMapRole(RoleModel role) {
        if (root.users().canManageDefault()) return checkAdminRoles(role);
        if (!root.isAdminSameRealm()) {
            return false;
        }

        if (role.getContainer() instanceof ClientModel) {
            if (root.clients().canMapRoles((ClientModel)role.getContainer())) return true;
        }
        if (!isPermissionsEnabled(role)){
            return false;
        }

        ResourceServer resourceServer = resourceServer(role);
        if (resourceServer == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapRolePermissionName(role), resourceServer.getId());
        if (policy == null || policy.getAssociatedPolicies().isEmpty()) {
            return false;
        }

        Resource roleResource = resource(role);
        Scope mapRoleScope = mapRoleScope(resourceServer);
        if (root.evaluatePermission(roleResource, mapRoleScope, resourceServer)) {
            return checkAdminRoles(role);
        } else {
            return false;
        }
    }

    @Override
    public void requireMapRole(RoleModel role) {
        if (!canMapRole(role)) {
            throw new ForbiddenException();
        }

    }

    @Override
    public boolean canList(RoleContainerModel container) {
        return root.hasAnyAdminRole();
    }

    @Override
    public void requireList(RoleContainerModel container) {
        if (!canList(container)) {
            throw new ForbiddenException();
        }

    }

    @Override
    public boolean canManage(RoleContainerModel container) {
        if (container instanceof RealmModel) {
            return root.realm().canManageRealm();
        } else {
            return root.clients().canConfigure((ClientModel)container);
        }
    }

    @Override
    public void requireManage(RoleContainerModel container) {
        if (!canManage(container)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canView(RoleContainerModel container) {
        if (container instanceof RealmModel) {
            return root.realm().canViewRealm();
        } else {
            return root.clients().canView((ClientModel)container);
        }
    }

    @Override
    public void requireView(RoleContainerModel container) {
        if (!canView(container)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canMapComposite(RoleModel role) {
        if (canManageDefault(role)) return checkAdminRoles(role);

        if (!root.isAdminSameRealm()) {
            return false;
        }
        if (role.getContainer() instanceof ClientModel) {
            if (root.clients().canMapCompositeRoles((ClientModel)role.getContainer())) return true;
        }
        if (!isPermissionsEnabled(role)){
            return false;
        }

        ResourceServer resourceServer = resourceServer(role);
        if (resourceServer == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapCompositePermissionName(role), resourceServer.getId());
        if (policy == null || policy.getAssociatedPolicies().isEmpty()) {
            return false;
        }

        Resource roleResource = resource(role);
        Scope scope = mapCompositeScope(resourceServer);
        if (root.evaluatePermission(roleResource, scope, resourceServer)) {
            return checkAdminRoles(role);
        } else {
            return false;
        }
    }

    @Override
    public void requireMapComposite(RoleModel role) {
        if (!canMapComposite(role)) {
            throw new ForbiddenException();
        }

    }


    @Override
    public boolean canMapClientScope(RoleModel role) {
        if (root.clients().canManageClientsDefault()) return true;
        if (!root.isAdminSameRealm()) {
            return false;
        }
        if (role.getContainer() instanceof ClientModel) {
            if (root.clients().canMapClientScopeRoles((ClientModel)role.getContainer())) return true;
        }
        if (!isPermissionsEnabled(role)){
            return false;
        }

        ResourceServer resourceServer = resourceServer(role);
        if (resourceServer == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapClientScopePermissionName(role), resourceServer.getId());
        if (policy == null || policy.getAssociatedPolicies().isEmpty()) {
            return false;
        }

        Resource roleResource = resource(role);
        Scope scope = mapClientScope(resourceServer);
        return root.evaluatePermission(roleResource, scope, resourceServer);
    }

    @Override
    public void requireMapClientScope(RoleModel role) {
        if (!canMapClientScope(role)) {
            throw new ForbiddenException();
        }
    }


    @Override
    public boolean canManage(RoleModel role) {
        if (role.getContainer() instanceof RealmModel) {
            return root.realm().canManageRealm();
        } else if (role.getContainer() instanceof ClientModel) {
            ClientModel client = (ClientModel)role.getContainer();
            return root.clients().canManage(client);
        }
        return false;
    }

    public boolean canManageDefault(RoleModel role) {
        if (role.getContainer() instanceof RealmModel) {
            return root.realm().canManageRealmDefault();
        } else if (role.getContainer() instanceof ClientModel) {
            ClientModel client = (ClientModel)role.getContainer();
            return root.clients().canManageClientsDefault();
        }
        return false;
    }

    @Override
    public void requireManage(RoleModel role) {
        if (!canManage(role)) {
            throw new ForbiddenException();
        }

    }

    @Override
    public boolean canView(RoleModel role) {
        if (role.getContainer() instanceof RealmModel) {
            return root.realm().canViewRealm();
        } else if (role.getContainer() instanceof ClientModel) {
            ClientModel client = (ClientModel)role.getContainer();
            return root.clients().canView(client);
        }
        return false;
    }

    @Override
    public void requireView(RoleModel role) {
        if (!canView(role)) {
            throw new ForbiddenException();
        }

    }

    private ClientModel getRoleClient(RoleModel role) {
        ClientModel client = null;
        if (role.getContainer() instanceof ClientModel) {
            client = (ClientModel)role.getContainer();
        } else {
            client = root.getRealmManagementClient();
        }
        return client;
    }

    @Override
    public Policy manageUsersPolicy(ResourceServer server) {
        RoleModel role = root.getRealmManagementClient().getRole(AdminRoles.MANAGE_USERS);
        return rolePolicy(server, role);
    }

    @Override
    public Policy viewUsersPolicy(ResourceServer server) {
        RoleModel role = root.getRealmManagementClient().getRole(AdminRoles.VIEW_USERS);
        return rolePolicy(server, role);
    }

    @Override
    public Policy rolePolicy(ResourceServer server, RoleModel role) {
        String policyName = Helper.getRolePolicyName(role);
        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(policyName, server.getId());
        if (policy != null) return policy;
        return Helper.createRolePolicy(authz, server, role, policyName);
    }

    private Scope mapRoleScope(ResourceServer server) {
        return authz.getStoreFactory().getScopeStore().findByName(MAP_ROLE_SCOPE, server.getId());
    }

    private Scope mapClientScope(ResourceServer server) {
        return authz.getStoreFactory().getScopeStore().findByName(MAP_ROLE_CLIENT_SCOPE_SCOPE, server.getId());
    }

    private Scope mapCompositeScope(ResourceServer server) {
        return authz.getStoreFactory().getScopeStore().findByName(MAP_ROLE_COMPOSITE_SCOPE, server.getId());
    }


    private void initialize(RoleModel role) {
        ResourceServer server = resourceServer(role);
        if (server == null) {
            ClientModel client = getRoleClient(role);
            server = root.findOrCreateResourceServer(client);
        }
        Scope mapRoleScope = mapRoleScope(server);
        if (mapRoleScope == null) {
            mapRoleScope = authz.getStoreFactory().getScopeStore().create(MAP_ROLE_SCOPE, server);
        }
        Scope mapClientScope = mapClientScope(server);
        if (mapClientScope == null) {
            mapClientScope = authz.getStoreFactory().getScopeStore().create(MAP_ROLE_CLIENT_SCOPE_SCOPE, server);
        }
        Scope mapCompositeScope = mapCompositeScope(server);
        if (mapCompositeScope == null) {
            mapCompositeScope = authz.getStoreFactory().getScopeStore().create(MAP_ROLE_COMPOSITE_SCOPE, server);
        }

        String roleResourceName = getRoleResourceName(role);
        Resource resource = authz.getStoreFactory().getResourceStore().findByName(roleResourceName, server.getId());
        if (resource == null) {
            resource = authz.getStoreFactory().getResourceStore().create(roleResourceName, server, server.getClientId());
            Set<Scope> scopeset = new HashSet<>();
            scopeset.add(mapClientScope);
            scopeset.add(mapCompositeScope);
            scopeset.add(mapRoleScope);
            resource.updateScopes(scopeset);
            resource.setType("Role");
        }
        Policy mapRolePermission = mapRolePermission(role);
        if (mapRolePermission == null) {
            mapRolePermission = Helper.addEmptyScopePermission(authz, server, getMapRolePermissionName(role), resource, mapRoleScope);
            mapRolePermission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        }

        Policy mapClientScopePermission = mapClientScopePermission(role);
        if (mapClientScopePermission == null) {
            mapClientScopePermission = Helper.addEmptyScopePermission(authz, server, getMapClientScopePermissionName(role), resource, mapClientScope);
            mapClientScopePermission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        }

        Policy mapCompositePermission = mapCompositePermission(role);
        if (mapCompositePermission == null) {
            mapCompositePermission = Helper.addEmptyScopePermission(authz, server, getMapCompositePermissionName(role), resource, mapCompositeScope);
            mapCompositePermission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        }
    }

    private String getMapRolePermissionName(RoleModel role) {
        return MAP_ROLE_SCOPE + ".permission." + role.getId();
    }

    private String getMapClientScopePermissionName(RoleModel role) {
        return MAP_ROLE_CLIENT_SCOPE_SCOPE + ".permission." + role.getId();
    }

    private String getMapCompositePermissionName(RoleModel role) {
        return MAP_ROLE_COMPOSITE_SCOPE + ".permission." + role.getId();
    }

    private ResourceServer sdfgetResourceServer(RoleModel role) {
        ClientModel client = getRoleClient(role);
        return root.findOrCreateResourceServer(client);
    }

    private static String getRoleResourceName(RoleModel role) {
        return "role.resource." + role.getId();
    }


}
