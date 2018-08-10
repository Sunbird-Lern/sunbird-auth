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
package org.keycloak.services.managers;

import org.keycloak.Config;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.DefaultRequiredActions;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.services.clientregistration.policy.DefaultClientRegistrationPolicies;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Per request object
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmManager {

    protected KeycloakSession session;
    protected RealmProvider model;
    protected String contextPath = "";

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public RealmManager(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
    }

    public KeycloakSession getSession() {
        return session;
    }

    public RealmModel getKeycloakAdminstrationRealm() {
        return getRealm(Config.getAdminRealm());
    }

    public RealmModel getRealm(String id) {
        return model.getRealm(id);
    }

    public RealmModel getRealmByName(String name) {
        return model.getRealmByName(name);
    }

    public RealmModel createRealm(String name) {
        return createRealm(name, name);
    }

    public RealmModel createRealm(String id, String name) {
        if (id == null) id = KeycloakModelUtils.generateId();
        RealmModel realm = model.createRealm(id, name);
        realm.setName(name);

        // setup defaults
        setupRealmDefaults(realm);

        setupMasterAdminManagement(realm);
        setupRealmAdminManagement(realm);
        setupAccountManagement(realm);
        setupBrokerService(realm);
        setupAdminConsole(realm);
        setupAdminConsoleLocaleMapper(realm);
        setupAdminCli(realm);
        setupImpersonationService(realm);
        setupAuthenticationFlows(realm);
        setupRequiredActions(realm);
        setupOfflineTokens(realm);
        setupAuthorizationServices(realm);
        setupClientRegistrations(realm);

        fireRealmPostCreate(realm);

        return realm;
    }

    protected void setupAuthenticationFlows(RealmModel realm) {
        if (realm.getAuthenticationFlows().size() == 0) DefaultAuthenticationFlows.addFlows(realm);
    }

    protected void setupRequiredActions(RealmModel realm) {
        if (realm.getRequiredActionProviders().size() == 0) DefaultRequiredActions.addActions(realm);
    }

    protected void setupOfflineTokens(RealmModel realm) {
        KeycloakModelUtils.setupOfflineTokens(realm);
    }

    protected void setupAdminConsole(RealmModel realm) {
        ClientModel adminConsole = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (adminConsole == null) adminConsole = KeycloakModelUtils.createClient(realm, Constants.ADMIN_CONSOLE_CLIENT_ID);
        adminConsole.setName("${client_" + Constants.ADMIN_CONSOLE_CLIENT_ID + "}");
        String baseUrl = contextPath + "/admin/" + realm.getName() + "/console";
        adminConsole.setBaseUrl(baseUrl + "/index.html");
        adminConsole.setEnabled(true);
        adminConsole.setPublicClient(true);
        adminConsole.addRedirectUri(baseUrl + "/*");
        adminConsole.setFullScopeAllowed(false);
        adminConsole.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        RoleModel adminRole;
        if (realm.getName().equals(Config.getAdminRealm())) {
            adminRole = realm.getRole(AdminRoles.ADMIN);
        } else {
            String realmAdminApplicationClientId = getRealmAdminClientId(realm);
            ClientModel realmAdminApp = realm.getClientByClientId(realmAdminApplicationClientId);
            adminRole = realmAdminApp.getRole(AdminRoles.REALM_ADMIN);
        }
    }

    protected void setupAdminConsoleLocaleMapper(RealmModel realm) {
        ClientModel adminConsole = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        ProtocolMapperModel localeMapper = adminConsole.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, OIDCLoginProtocolFactory.LOCALE);

        if (localeMapper == null) {
            localeMapper = ProtocolMapperUtils.findLocaleMapper(session);
            if (localeMapper != null) {
                adminConsole.addProtocolMapper(localeMapper);
            }
        }
    }

    public void setupAdminCli(RealmModel realm) {
        ClientModel adminCli = realm.getClientByClientId(Constants.ADMIN_CLI_CLIENT_ID);
        if (adminCli == null) {
            adminCli = KeycloakModelUtils.createClient(realm, Constants.ADMIN_CLI_CLIENT_ID);
            adminCli.setName("${client_" + Constants.ADMIN_CLI_CLIENT_ID + "}");
            adminCli.setEnabled(true);
            adminCli.setPublicClient(true);
            adminCli.setFullScopeAllowed(false);
            adminCli.setStandardFlowEnabled(false);
            adminCli.setDirectAccessGrantsEnabled(true);
            adminCli.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            RoleModel adminRole;
            if (realm.getName().equals(Config.getAdminRealm())) {
                adminRole = realm.getRole(AdminRoles.ADMIN);
            } else {
                String realmAdminApplicationClientId = getRealmAdminClientId(realm);
                ClientModel realmAdminApp = realm.getClientByClientId(realmAdminApplicationClientId);
                adminRole = realmAdminApp.getRole(AdminRoles.REALM_ADMIN);
            }
        }

    }
    public void addQueryCompositeRoles(ClientModel realmAccess) {
        RoleModel queryClients = realmAccess.getRole(AdminRoles.QUERY_CLIENTS);
        RoleModel queryUsers = realmAccess.getRole(AdminRoles.QUERY_USERS);
        RoleModel queryGroups = realmAccess.getRole(AdminRoles.QUERY_GROUPS);

        RoleModel viewClients = realmAccess.getRole(AdminRoles.VIEW_CLIENTS);
        viewClients.addCompositeRole(queryClients);
        RoleModel viewUsers = realmAccess.getRole(AdminRoles.VIEW_USERS);
        viewUsers.addCompositeRole(queryUsers);
        viewUsers.addCompositeRole(queryGroups);
    }


    public String getRealmAdminClientId(RealmModel realm) {
        return Constants.REALM_MANAGEMENT_CLIENT_ID;
    }

    public String getRealmAdminClientId(RealmRepresentation realm) {
        return Constants.REALM_MANAGEMENT_CLIENT_ID;
    }



    protected void setupRealmDefaults(RealmModel realm) {
        realm.setBrowserSecurityHeaders(BrowserSecurityHeaders.defaultHeaders);

        // brute force
        realm.setBruteForceProtected(false); // default settings off for now todo set it on
        realm.setPermanentLockout(false);
        realm.setMaxFailureWaitSeconds(900);
        realm.setMinimumQuickLoginWaitSeconds(60);
        realm.setWaitIncrementSeconds(60);
        realm.setQuickLoginCheckMilliSeconds(1000);
        realm.setMaxDeltaTimeSeconds(60 * 60 * 12); // 12 hours
        realm.setFailureFactor(30);
        realm.setSslRequired(SslRequired.EXTERNAL);
        realm.setOTPPolicy(OTPPolicy.DEFAULT_POLICY);
        realm.setLoginWithEmailAllowed(true);

        realm.setEventsListeners(Collections.singleton("jboss-logging"));
    }

    public boolean removeRealm(RealmModel realm) {

        ClientModel masterAdminClient = realm.getMasterAdminClient();
        boolean removed = model.removeRealm(realm.getId());
        if (removed) {
            if (masterAdminClient != null) {
                new ClientManager(this).removeClient(getKeycloakAdminstrationRealm(), masterAdminClient);
            }

            UserSessionProvider sessions = session.sessions();
            if (sessions != null) {
                sessions.onRealmRemoved(realm);
            }

            UserSessionPersisterProvider sessionsPersister = session.getProvider(UserSessionPersisterProvider.class);
            if (sessionsPersister != null) {
                sessionsPersister.onRealmRemoved(realm);
            }

            AuthenticationSessionProvider authSessions = session.authenticationSessions();
            if (authSessions != null) {
                authSessions.onRealmRemoved(realm);
            }

          // Refresh periodic sync tasks for configured storageProviders
            List<UserStorageProviderModel> storageProviders = realm.getUserStorageProviders();
            UserStorageSyncManager storageSync = new UserStorageSyncManager();
            for (UserStorageProviderModel provider : storageProviders) {
                storageSync.notifyToRefreshPeriodicSync(session, realm, provider, true);
            }

        }
        return removed;
    }

    public void updateRealmEventsConfig(RealmEventsConfigRepresentation rep, RealmModel realm) {
        realm.setEventsEnabled(rep.isEventsEnabled());
        realm.setEventsExpiration(rep.getEventsExpiration() != null ? rep.getEventsExpiration() : 0);
        if (rep.getEventsListeners() != null) {
            realm.setEventsListeners(new HashSet<>(rep.getEventsListeners()));
        }
        if(rep.getEnabledEventTypes() != null) {
            realm.setEnabledEventTypes(new HashSet<>(rep.getEnabledEventTypes()));
        }
        if(rep.isAdminEventsEnabled() != null) {
            realm.setAdminEventsEnabled(rep.isAdminEventsEnabled());
        }
        if(rep.isAdminEventsDetailsEnabled() != null){
            realm.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());
        }
    }


    public void setupMasterAdminManagement(RealmModel realm) {
        // Need to refresh masterApp for current realm
        String adminRealmId = Config.getAdminRealm();
        RealmModel adminRealm = model.getRealm(adminRealmId);
        ClientModel masterApp = adminRealm.getClientByClientId(KeycloakModelUtils.getMasterRealmAdminApplicationClientId(realm.getName()));
        if (masterApp != null) {
            realm.setMasterAdminClient(masterApp);
        }  else {
            createMasterAdminManagement(realm);
        }
    }

    private void createMasterAdminManagement(RealmModel realm) {
        RealmModel adminRealm;
        RoleModel adminRole;

        if (realm.getName().equals(Config.getAdminRealm())) {
            adminRealm = realm;

            adminRole = realm.addRole(AdminRoles.ADMIN);

            RoleModel createRealmRole = realm.addRole(AdminRoles.CREATE_REALM);
            adminRole.addCompositeRole(createRealmRole);
            createRealmRole.setDescription("${role_" + AdminRoles.CREATE_REALM + "}");
            createRealmRole.setScopeParamRequired(false);
        } else {
            adminRealm = model.getRealm(Config.getAdminRealm());
            adminRole = adminRealm.getRole(AdminRoles.ADMIN);
        }
        adminRole.setDescription("${role_"+AdminRoles.ADMIN+"}");
        adminRole.setScopeParamRequired(false);

        ClientModel realmAdminApp = KeycloakModelUtils.createClient(adminRealm, KeycloakModelUtils.getMasterRealmAdminApplicationClientId(realm.getName()));
        // No localized name for now
        realmAdminApp.setName(realm.getName() + " Realm");
        realmAdminApp.setBearerOnly(true);
        realm.setMasterAdminClient(realmAdminApp);

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel role = realmAdminApp.addRole(r);
            role.setDescription("${role_"+r+"}");
            role.setScopeParamRequired(false);
            adminRole.addCompositeRole(role);
        }
        addQueryCompositeRoles(realmAdminApp);
    }

    private void checkMasterAdminManagementRoles(RealmModel realm) {
        RealmModel adminRealm = model.getRealmByName(Config.getAdminRealm());
        RoleModel adminRole = adminRealm.getRole(AdminRoles.ADMIN);

        ClientModel masterAdminClient = realm.getMasterAdminClient();
        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel found = masterAdminClient.getRole(r);
            if (found == null) {
                addAndSetAdminRole(r, masterAdminClient, adminRole);
            }
        }
        addQueryCompositeRoles(masterAdminClient);
    }


    private void setupRealmAdminManagement(RealmModel realm) {
        if (realm.getName().equals(Config.getAdminRealm())) { return; } // don't need to do this for master realm

        String realmAdminClientId = getRealmAdminClientId(realm);
        ClientModel realmAdminClient = realm.getClientByClientId(realmAdminClientId);
        if (realmAdminClient == null) {
            realmAdminClient = KeycloakModelUtils.createClient(realm, realmAdminClientId);
            realmAdminClient.setName("${client_" + realmAdminClientId + "}");
        }
        RoleModel adminRole = realmAdminClient.addRole(AdminRoles.REALM_ADMIN);
        adminRole.setDescription("${role_" + AdminRoles.REALM_ADMIN + "}");
        adminRole.setScopeParamRequired(false);
        realmAdminClient.setBearerOnly(true);
        realmAdminClient.setFullScopeAllowed(false);
        realmAdminClient.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            addAndSetAdminRole(r, realmAdminClient, adminRole);
        }
        addQueryCompositeRoles(realmAdminClient);
    }

    private void addAndSetAdminRole(String roleName, ClientModel parentClient, RoleModel parentRole) {
        RoleModel role = parentClient.addRole(roleName);
        role.setDescription("${role_" + roleName + "}");
        role.setScopeParamRequired(false);
        parentRole.addCompositeRole(role);
    }


    private void checkRealmAdminManagementRoles(RealmModel realm) {
        if (realm.getName().equals(Config.getAdminRealm())) { return; } // don't need to do this for master realm

        String realmAdminClientId = getRealmAdminClientId(realm);
        ClientModel realmAdminClient = realm.getClientByClientId(realmAdminClientId);
        RoleModel adminRole = realmAdminClient.getRole(AdminRoles.REALM_ADMIN);

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel found = realmAdminClient.getRole(r);
            if (found == null) {
                addAndSetAdminRole(r, realmAdminClient, adminRole);
            }
        }
        addQueryCompositeRoles(realmAdminClient);
    }


    private void setupAccountManagement(RealmModel realm) {
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (client == null) {
            client = KeycloakModelUtils.createClient(realm, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            client.setName("${client_" + Constants.ACCOUNT_MANAGEMENT_CLIENT_ID + "}");
            client.setEnabled(true);
            client.setFullScopeAllowed(false);
            String base = contextPath + "/realms/" + realm.getName() + "/account";
            String redirectUri = base + "/*";
            client.addRedirectUri(redirectUri);
            client.setBaseUrl(base);
            client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            for (String role : AccountRoles.ALL) {
                client.addDefaultRole(role);
                RoleModel roleModel = client.getRole(role);
                roleModel.setDescription("${role_" + role + "}");
                roleModel.setScopeParamRequired(false);
            }
            RoleModel manageAccountLinks = client.addRole(AccountRoles.MANAGE_ACCOUNT_LINKS);
            manageAccountLinks.setDescription("${role_" + AccountRoles.MANAGE_ACCOUNT_LINKS + "}");
            manageAccountLinks.setScopeParamRequired(false);
            RoleModel manageAccount = client.getRole(AccountRoles.MANAGE_ACCOUNT);
            manageAccount.addCompositeRole(manageAccountLinks);
        }
    }

    public void setupImpersonationService(RealmModel realm) {
        ImpersonationConstants.setupImpersonationService(session, realm);
    }

    public void setupBrokerService(RealmModel realm) {
        ClientModel client = realm.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
        if (client == null) {
            client = KeycloakModelUtils.createClient(realm, Constants.BROKER_SERVICE_CLIENT_ID);
            client.setEnabled(true);
            client.setName("${client_" + Constants.BROKER_SERVICE_CLIENT_ID + "}");
            client.setFullScopeAllowed(false);
            client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            for (String role : Constants.BROKER_SERVICE_ROLES) {
                RoleModel roleModel = client.addRole(role);
                roleModel.setDescription("${role_"+ role.toLowerCase().replaceAll("_", "-") +"}");
                roleModel.setScopeParamRequired(false);
            }
        }
    }

    public RealmModel importRealm(RealmRepresentation rep) {
        return importRealm(rep, false);
    }


    /**
     * if "skipUserDependent" is true, then import of any models, which needs users already imported in DB, will be skipped. For example authorization
     */
    public RealmModel importRealm(RealmRepresentation rep, boolean skipUserDependent) {
        String id = rep.getId();
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }
        RealmModel realm = model.createRealm(id, rep.getRealm());
        realm.setName(rep.getRealm());

        // setup defaults

        setupRealmDefaults(realm);

        boolean postponeMasterClientSetup = postponeMasterClientSetup(rep);
        if (!postponeMasterClientSetup) {
            setupMasterAdminManagement(realm);
        }

        if (!hasRealmAdminManagementClient(rep)) setupRealmAdminManagement(realm);
        if (!hasAccountManagementClient(rep)) setupAccountManagement(realm);

        boolean postponeImpersonationSetup = false;
        if (hasRealmAdminManagementClient(rep)) {
            postponeImpersonationSetup = true;
        } else {
            setupImpersonationService(realm);
        }


        if (!hasBrokerClient(rep)) setupBrokerService(realm);
        if (!hasAdminConsoleClient(rep)) setupAdminConsole(realm);

        boolean postponeAdminCliSetup = false;
        if (!hasAdminCliClient(rep)) {
            if (hasRealmAdminManagementClient(rep)) {
                postponeAdminCliSetup = true;
            } else {
                setupAdminCli(realm);
            }
        }

        if (!hasRealmRole(rep, Constants.OFFLINE_ACCESS_ROLE)) setupOfflineTokens(realm);

        RepresentationToModel.importRealm(session, rep, realm, skipUserDependent);

        setupAdminConsoleLocaleMapper(realm);

        if (postponeMasterClientSetup) {
            setupMasterAdminManagement(realm);
        }

        // Assert all admin roles are available once import took place. This is needed due to import from previous version where JSON file may not contain all admin roles
        checkMasterAdminManagementRoles(realm);
        checkRealmAdminManagementRoles(realm);

        // Could happen when migrating from older version and I have exported JSON file, which contains "realm-management" client but not "impersonation" client
        // I need to postpone impersonation because it needs "realm-management" client and its roles set
        if (postponeImpersonationSetup) {
            setupImpersonationService(realm);
            String realmAdminClientId = getRealmAdminClientId(realm);
         }

        if (postponeAdminCliSetup) {
            setupAdminCli(realm);
        }

        setupAuthenticationFlows(realm);
        setupRequiredActions(realm);

        // Refresh periodic sync tasks for configured storageProviders
        List<UserStorageProviderModel> storageProviders = realm.getUserStorageProviders();
        UserStorageSyncManager storageSync = new UserStorageSyncManager();
        for (UserStorageProviderModel provider : storageProviders) {
            storageSync.notifyToRefreshPeriodicSync(session, realm, provider, false);
        }

        setupAuthorizationServices(realm);
        setupClientRegistrations(realm);

        fireRealmPostCreate(realm);

        return realm;
    }

    private boolean postponeMasterClientSetup(RealmRepresentation rep) {
        if (!Config.getAdminRealm().equals(rep.getRealm())) {
            return false;
        }

        return hasRealmAdminManagementClient(rep);
    }

    private boolean hasRealmAdminManagementClient(RealmRepresentation rep) {
        String realmAdminClientId =  Config.getAdminRealm().equals(rep.getRealm()) ?  KeycloakModelUtils.getMasterRealmAdminApplicationClientId(rep.getRealm()) : getRealmAdminClientId(rep);
        return hasClient(rep, realmAdminClientId);
    }

    private boolean hasAccountManagementClient(RealmRepresentation rep) {
        return hasClient(rep, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
    }

    private boolean hasBrokerClient(RealmRepresentation rep) {
        return hasClient(rep, Constants.BROKER_SERVICE_CLIENT_ID);
    }

    private boolean hasAdminConsoleClient(RealmRepresentation rep) {
        return hasClient(rep, Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    private boolean hasAdminCliClient(RealmRepresentation rep) {
        return hasClient(rep, Constants.ADMIN_CLI_CLIENT_ID);
    }

    private boolean hasClient(RealmRepresentation rep, String clientId) {
        if (rep.getClients() != null) {
            for (ClientRepresentation clientRep : rep.getClients()) {
                if (clientRep.getClientId() != null && clientRep.getClientId().equals(clientId)) {
                    return true;
                }
            }
        }

        // TODO: Just for compatibility with old versions. Should be removed later...
        if (rep.getApplications() != null) {
            for (ApplicationRepresentation clientRep : rep.getApplications()) {
                if (clientRep.getName().equals(clientId)) {
                    return true;
                }
            }
        }
        if (rep.getOauthClients() != null) {
            for (OAuthClientRepresentation clientRep : rep.getOauthClients()) {
                if (clientRep.getName().equals(clientId)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasRealmRole(RealmRepresentation rep, String roleName) {
        if (rep.getRoles() == null || rep.getRoles().getRealm() == null) {
            return false;
        }

        for (RoleRepresentation role : rep.getRoles().getRealm()) {
            if (roleName.equals(role.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Query users based on a search string:
     * <p/>
     * "Bill Burke" first and last name
     * "bburke@redhat.com" email
     * "Burke" lastname or username
     *
     * @param searchString
     * @param realmModel
     * @return
     */
    public List<UserModel> searchUsers(String searchString, RealmModel realmModel) {
        if (searchString == null) {
            return Collections.emptyList();
        }
        return session.users().searchForUser(searchString.trim(), realmModel);
    }

    private void setupAuthorizationServices(RealmModel realm) {
        KeycloakModelUtils.setupAuthorizationServices(realm);
    }

    private void setupClientRegistrations(RealmModel realm) {
        DefaultClientRegistrationPolicies.addDefaultPolicies(realm);
    }

    private void fireRealmPostCreate(RealmModel realm) {
        session.getKeycloakSessionFactory().publish(new RealmModel.RealmPostCreateEvent() {
            @Override
            public RealmModel getCreatedRealm() {
                return realm;
            }
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

    }

}
