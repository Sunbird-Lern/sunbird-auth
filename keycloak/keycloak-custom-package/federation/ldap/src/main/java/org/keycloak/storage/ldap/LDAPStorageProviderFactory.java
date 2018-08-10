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

package org.keycloak.storage.ldap;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.impl.KerberosServerSubjectAuthenticator;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPConfigDecorator;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.msad.MSADUserAccountControlStorageMapperFactory;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.utils.CredentialHelper;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LDAPStorageProviderFactory implements UserStorageProviderFactory<LDAPStorageProvider>, ImportSynchronization {


    private static final Logger logger = Logger.getLogger(LDAPStorageProviderFactory.class);
    public static final String PROVIDER_NAME = LDAPConstants.LDAP_PROVIDER;

    private LDAPIdentityStoreRegistry ldapStoreRegistry;

    protected static final List<ProviderConfigProperty> configProperties;

    static {
        configProperties = getConfigProps(null);
    }

    private static List<ProviderConfigProperty> getConfigProps(ComponentModel parent) {
        boolean readOnly = false;
        if (parent != null) {
            LDAPConfig config = new LDAPConfig(parent.getConfig());
            readOnly = config.getEditMode() != UserStorageProvider.EditMode.WRITABLE;
        }


        return ProviderConfigurationBuilder.create()
                .property().name(LDAPConstants.EDIT_MODE)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(UserStorageProviderModel.IMPORT_ENABLED)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("true")
                .add()
                .property().name(LDAPConstants.SYNC_REGISTRATIONS)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("false")
                .add()
                .property().name(LDAPConstants.VENDOR)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.USERNAME_LDAP_ATTRIBUTE)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.RDN_LDAP_ATTRIBUTE)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.UUID_LDAP_ATTRIBUTE)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.USER_OBJECT_CLASSES)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.CONNECTION_URL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.USERS_DN)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.AUTH_TYPE)
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("simple")
                .add()
                .property().name(LDAPConstants.BIND_DN)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.BIND_CREDENTIAL)
                .type(ProviderConfigProperty.PASSWORD)
                .secret(true)
                .add()
                .property().name(LDAPConstants.CUSTOM_USER_SEARCH_FILTER)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.SEARCH_SCOPE)
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("1")
                .add()
                .property().name(LDAPConstants.USE_TRUSTSTORE_SPI)
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("ldapsOnly")
                .add()
                .property().name(LDAPConstants.CONNECTION_POOLING)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("true")
                .add()
                .property().name(LDAPConstants.CONNECTION_TIMEOUT)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.READ_TIMEOUT)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(LDAPConstants.PAGINATION)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("true")
                .add()
                .property().name(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("false")
                .add()
                .property().name(KerberosConstants.SERVER_PRINCIPAL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(KerberosConstants.KEYTAB)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(KerberosConstants.KERBEROS_REALM)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(KerberosConstants.DEBUG)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("false")
                .add()
                .property().name(KerberosConstants.USE_KERBEROS_FOR_PASSWORD_AUTHENTICATION)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("false")
                .add()
                .property().name(KerberosConstants.SERVER_PRINCIPAL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .build();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public LDAPStorageProvider create(KeycloakSession session, ComponentModel model) {
        Map<ComponentModel, LDAPConfigDecorator> configDecorators = getLDAPConfigDecorators(session, model);

        LDAPIdentityStore ldapIdentityStore = this.ldapStoreRegistry.getLdapStore(session, model, configDecorators);
        return new LDAPStorageProvider(this, session, model, ldapIdentityStore);
    }


    // Check if it's some performance overhead to create this map in every request. But probably not...
    protected Map<ComponentModel, LDAPConfigDecorator> getLDAPConfigDecorators(KeycloakSession session, ComponentModel ldapModel) {
        RealmModel realm = session.realms().getRealm(ldapModel.getParentId());
        List<ComponentModel> mapperComponents = realm.getComponents(ldapModel.getId(), LDAPStorageMapper.class.getName());

        Map<ComponentModel, LDAPConfigDecorator> result = new HashMap<>();
        for (ComponentModel mapperModel : mapperComponents) {
            LDAPStorageMapperFactory mapperFactory = (LDAPStorageMapperFactory) session.getKeycloakSessionFactory().getProviderFactory(LDAPStorageMapper.class, mapperModel.getProviderId());
            if (mapperFactory instanceof LDAPConfigDecorator) {
                result.put(mapperModel, (LDAPConfigDecorator) mapperFactory);
            }
        }

        return result;
    }


    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        LDAPConfig cfg = new LDAPConfig(config.getConfig());
        String customFilter = cfg.getCustomUserSearchFilter();
        LDAPUtils.validateCustomLdapFilter(customFilter);

        String connectionTimeout = cfg.getConnectionTimeout();
        if (connectionTimeout != null && !connectionTimeout.isEmpty()) {
            try {
                Long.parseLong(connectionTimeout);
            } catch (NumberFormatException nfe) {
                throw new ComponentValidationException("ldapErrorConnectionTimeoutNotNumber");
            }
        }

        String readTimeout = cfg.getReadTimeout();
        if (readTimeout != null && !readTimeout.isEmpty()) {
            try {
                Long.parseLong(readTimeout);
            } catch (NumberFormatException nfe) {
                throw new ComponentValidationException("ldapErrorReadTimeoutNotNumber");
            }
        }

    }

    @Override
    public void init(Config.Scope config) {
        this.ldapStoreRegistry = new LDAPIdentityStoreRegistry();
    }

    @Override
    public void close() {
        this.ldapStoreRegistry = null;
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    // Best effort to create appropriate mappers according to our LDAP config
    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        LDAPConfig ldapConfig = new LDAPConfig(model.getConfig());

        boolean activeDirectory = ldapConfig.isActiveDirectory();
        UserStorageProvider.EditMode editMode = ldapConfig.getEditMode();
        String readOnly = String.valueOf(editMode == UserStorageProvider.EditMode.READ_ONLY || editMode == UserStorageProvider.EditMode.UNSYNCED);
        String usernameLdapAttribute = ldapConfig.getUsernameLdapAttribute();

        String alwaysReadValueFromLDAP = String.valueOf(editMode== UserStorageProvider.EditMode.READ_ONLY || editMode== UserStorageProvider.EditMode.WRITABLE);

        ComponentModel mapperModel;
        mapperModel = KeycloakModelUtils.createComponentModel("username", model.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, UserModel.USERNAME,
                UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, usernameLdapAttribute,
                UserAttributeLDAPStorageMapper.READ_ONLY, readOnly,
                UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
        realm.addComponentModel(mapperModel);

        // CN is typically used as RDN for Active Directory deployments
        if (ldapConfig.getRdnLdapAttribute().equalsIgnoreCase(LDAPConstants.CN)) {

            if (usernameLdapAttribute.equalsIgnoreCase(LDAPConstants.CN)) {

                // For AD deployments with "cn" as username, we will map "givenName" to first name
                mapperModel = KeycloakModelUtils.createComponentModel("first name", model.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                        UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                        UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, LDAPConstants.GIVENNAME,
                        UserAttributeLDAPStorageMapper.READ_ONLY, readOnly,
                        UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                        UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
                realm.addComponentModel(mapperModel);

            } else {
                if (editMode == UserStorageProvider.EditMode.WRITABLE) {

                    // For AD deployments with "sAMAccountName" as username and writable, we need to map "cn" as username as well (this is needed so we can register new users from KC into LDAP) and we will map "givenName" to first name.
                    mapperModel = KeycloakModelUtils.createComponentModel("first name", model.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                            UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                            UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, LDAPConstants.GIVENNAME,
                            UserAttributeLDAPStorageMapper.READ_ONLY, readOnly,
                            UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                            UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
                    realm.addComponentModel(mapperModel);

                    mapperModel = KeycloakModelUtils.createComponentModel("username-cn", model.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                            UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, UserModel.USERNAME,
                            UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, LDAPConstants.CN,
                            UserAttributeLDAPStorageMapper.READ_ONLY, readOnly,
                            UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                            UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
                    realm.addComponentModel(mapperModel);
                } else {

                    // For read-only LDAP, we map "cn" as full name
                    mapperModel = KeycloakModelUtils.createComponentModel("full name", model.getId(), FullNameLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                            FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE, LDAPConstants.CN,
                            FullNameLDAPStorageMapper.READ_ONLY, readOnly,
                            FullNameLDAPStorageMapper.WRITE_ONLY, "false");
                    realm.addComponentModel(mapperModel);
                }
            }
        } else {
            mapperModel = KeycloakModelUtils.createComponentModel("first name", model.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                    UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                    UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, LDAPConstants.CN,
                    UserAttributeLDAPStorageMapper.READ_ONLY, readOnly,
                    UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                    UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
            realm.addComponentModel(mapperModel);
        }

        mapperModel = KeycloakModelUtils.createComponentModel("last name", model.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, UserModel.LAST_NAME,
                UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, LDAPConstants.SN,
                UserAttributeLDAPStorageMapper.READ_ONLY, readOnly,
                UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
        realm.addComponentModel(mapperModel);

        mapperModel = KeycloakModelUtils.createComponentModel("email", model.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, UserModel.EMAIL,
                UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, LDAPConstants.EMAIL,
                UserAttributeLDAPStorageMapper.READ_ONLY, readOnly,
                UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "false");
        realm.addComponentModel(mapperModel);

        String createTimestampLdapAttrName = activeDirectory ? "whenCreated" : LDAPConstants.CREATE_TIMESTAMP;
        String modifyTimestampLdapAttrName = activeDirectory ? "whenChanged" : LDAPConstants.MODIFY_TIMESTAMP;

        // map createTimeStamp as read-only
        mapperModel = KeycloakModelUtils.createComponentModel("creation date", model.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, LDAPConstants.CREATE_TIMESTAMP,
                UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, createTimestampLdapAttrName,
                UserAttributeLDAPStorageMapper.READ_ONLY, "true",
                UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "false");
        realm.addComponentModel(mapperModel);

        // map modifyTimeStamp as read-only
        mapperModel = KeycloakModelUtils.createComponentModel("modify date", model.getId(), UserAttributeLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, LDAPConstants.MODIFY_TIMESTAMP,
                UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, modifyTimestampLdapAttrName,
                UserAttributeLDAPStorageMapper.READ_ONLY, "true",
                UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, alwaysReadValueFromLDAP,
                UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "false");
        realm.addComponentModel(mapperModel);

        // MSAD specific mapper for account state propagation
        if (activeDirectory) {
            mapperModel = KeycloakModelUtils.createComponentModel("MSAD account controls", model.getId(), MSADUserAccountControlStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName());
            realm.addComponentModel(mapperModel);
        }
        checkKerberosCredential(session, realm, model);
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        checkKerberosCredential(session, realm, newModel);

    }

    @Override
    public void preRemove(KeycloakSession session, RealmModel realm, ComponentModel model) {
        String allowKerberosCfg = model.getConfig().getFirst(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION);
        if (Boolean.valueOf(allowKerberosCfg)) {
            CredentialHelper.setOrReplaceAuthenticationRequirement(session, realm, CredentialRepresentation.KERBEROS,
                    AuthenticationExecutionModel.Requirement.DISABLED, null);
        }
    }

    @Override
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        syncMappers(sessionFactory, realmId, model);

        logger.infof("Sync all users from LDAP to local store: realm: %s, federation provider: %s", realmId, model.getName());

        LDAPQuery userQuery = createQuery(sessionFactory, realmId, model);
        SynchronizationResult syncResult = syncImpl(sessionFactory, userQuery, realmId, model);

        // TODO: Remove all existing keycloak users, which have federation links, but are not in LDAP. Perhaps don't check users, which were just added or updated during this sync?

        logger.infof("Sync all users finished: %s", syncResult.getStatus());
        return syncResult;
    }

    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        syncMappers(sessionFactory, realmId, model);

        logger.infof("Sync changed users from LDAP to local store: realm: %s, federation provider: %s, last sync time: " + lastSync, realmId, model.getName());

        // Sync newly created and updated users
        LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
        Condition createCondition = conditionsBuilder.greaterThanOrEqualTo(LDAPConstants.CREATE_TIMESTAMP, lastSync);
        Condition modifyCondition = conditionsBuilder.greaterThanOrEqualTo(LDAPConstants.MODIFY_TIMESTAMP, lastSync);
        Condition orCondition = conditionsBuilder.orCondition(createCondition, modifyCondition);

        LDAPQuery userQuery = createQuery(sessionFactory, realmId, model);
        userQuery.addWhereCondition(orCondition);
        SynchronizationResult result = syncImpl(sessionFactory, userQuery, realmId, model);

        logger.infof("Sync changed users finished: %s", result.getStatus());
        return result;
    }

    protected void syncMappers(KeycloakSessionFactory sessionFactory, final String realmId, final ComponentModel model) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getProvider(UserStorageProvider.class, model);
                List<ComponentModel> mappers = realm.getComponents(model.getId(), LDAPStorageMapper.class.getName());
                for (ComponentModel mapperModel : mappers) {
                    LDAPStorageMapper ldapMapper = session.getProvider(LDAPStorageMapper.class, mapperModel);
                    SynchronizationResult syncResult = ldapMapper.syncDataFromFederationProviderToKeycloak(realm);
                    if (syncResult.getAdded() > 0 || syncResult.getUpdated() > 0 || syncResult.getRemoved() > 0 || syncResult.getFailed() > 0) {
                        logger.infof("Sync of federation mapper '%s' finished. Status: %s", mapperModel.getName(), syncResult.toString());
                    }
                }
            }

        });
    }

    protected SynchronizationResult syncImpl(KeycloakSessionFactory sessionFactory, LDAPQuery userQuery, final String realmId, final ComponentModel fedModel) {

        final SynchronizationResult syncResult = new SynchronizationResult();

        LDAPConfig ldapConfig = new LDAPConfig(fedModel.getConfig());
        boolean pagination = ldapConfig.isPagination();
        if (pagination) {
            int pageSize = ldapConfig.getBatchSizeForSync();

            boolean nextPage = true;
            while (nextPage) {
                userQuery.setLimit(pageSize);
                final List<LDAPObject> users = userQuery.getResultList();
                nextPage = userQuery.getPaginationContext() != null;
                SynchronizationResult currentPageSync = importLdapUsers(sessionFactory, realmId, fedModel, users);
                syncResult.add(currentPageSync);
            }
        } else {
            // LDAP pagination not available. Do everything in single transaction
            final List<LDAPObject> users = userQuery.getResultList();
            SynchronizationResult currentSync = importLdapUsers(sessionFactory, realmId, fedModel, users);
            syncResult.add(currentSync);
        }

        return syncResult;
    }

    private LDAPQuery createQuery(KeycloakSessionFactory sessionFactory, final String realmId, final ComponentModel model) {
        class QueryHolder {
            LDAPQuery query;
        }

        final QueryHolder queryHolder = new QueryHolder();
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                LDAPStorageProvider ldapFedProvider = (LDAPStorageProvider)session.getProvider(UserStorageProvider.class, model);
                RealmModel realm = session.realms().getRealm(realmId);
                queryHolder.query = LDAPUtils.createQueryForUserSearch(ldapFedProvider, realm);
            }

        });
        return queryHolder.query;
    }

    protected SynchronizationResult importLdapUsers(KeycloakSessionFactory sessionFactory, final String realmId, final ComponentModel fedModel, List<LDAPObject> ldapUsers) {
        final SynchronizationResult syncResult = new SynchronizationResult();

        class BooleanHolder {
            private boolean value = true;
        }
        final BooleanHolder exists = new BooleanHolder();

        for (final LDAPObject ldapUser : ldapUsers) {

            try {

                // Process each user in it's own transaction to avoid global fail
                KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                    @Override
                    public void run(KeycloakSession session) {
                        LDAPStorageProvider ldapFedProvider = (LDAPStorageProvider)session.getProvider(UserStorageProvider.class, fedModel);
                        RealmModel currentRealm = session.realms().getRealm(realmId);

                        String username = LDAPUtils.getUsername(ldapUser, ldapFedProvider.getLdapIdentityStore().getConfig());
                        exists.value = true;
                        LDAPUtils.checkUuid(ldapUser, ldapFedProvider.getLdapIdentityStore().getConfig());
                        UserModel currentUser = session.userLocalStorage().getUserByUsername(username, currentRealm);

                        if (currentUser == null) {

                            // Add new user to Keycloak
                            exists.value = false;
                            ldapFedProvider.importUserFromLDAP(session, currentRealm, ldapUser);
                            syncResult.increaseAdded();

                        } else {
                            if ((fedModel.getId().equals(currentUser.getFederationLink())) && (ldapUser.getUuid().equals(currentUser.getFirstAttribute(LDAPConstants.LDAP_ID)))) {

                                // Update keycloak user
                                List<ComponentModel> federationMappers = currentRealm.getComponents(fedModel.getId(), LDAPStorageMapper.class.getName());
                                List<ComponentModel> sortedMappers = ldapFedProvider.getMapperManager().sortMappersDesc(federationMappers);
                                for (ComponentModel mapperModel : sortedMappers) {
                                    LDAPStorageMapper ldapMapper = ldapFedProvider.getMapperManager().getMapper(mapperModel);
                                    ldapMapper.onImportUserFromLDAP(ldapUser, currentUser, currentRealm, false);
                                }

                                logger.debugf("Updated user from LDAP: %s", currentUser.getUsername());
                                syncResult.increaseUpdated();
                            } else {
                                logger.warnf("User '%s' is not updated during sync as he already exists in Keycloak database but is not linked to federation provider '%s'", username, fedModel.getName());
                                syncResult.increaseFailed();
                            }
                        }
                    }

                });
            } catch (ModelException me) {
                logger.error("Failed during import user from LDAP", me);
                syncResult.increaseFailed();

                // Remove user if we already added him during this transaction
                if (!exists.value) {
                    KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                        @Override
                        public void run(KeycloakSession session) {
                            LDAPStorageProvider ldapFedProvider = (LDAPStorageProvider)session.getProvider(UserStorageProvider.class, fedModel);
                            RealmModel currentRealm = session.realms().getRealm(realmId);
                            String username = null;
                            try {
                                username = LDAPUtils.getUsername(ldapUser, ldapFedProvider.getLdapIdentityStore().getConfig());
                            } catch (ModelException ignore) {
                            }

                            if (username != null) {
                                UserModel existing = session.userLocalStorage().getUserByUsername(username, currentRealm);
                                if (existing != null) {
                                    UserCache userCache = session.userCache();
                                    if (userCache != null) {
                                        userCache.evict(currentRealm, existing);
                                    }
                                    session.userLocalStorage().removeUser(currentRealm, existing);
                                }
                            }
                        }

                    });
                }
            }
        }

        return syncResult;
    }

    protected SPNEGOAuthenticator createSPNEGOAuthenticator(String spnegoToken, CommonKerberosConfig kerberosConfig) {
        KerberosServerSubjectAuthenticator kerberosAuth = createKerberosSubjectAuthenticator(kerberosConfig);
        return new SPNEGOAuthenticator(kerberosConfig, kerberosAuth, spnegoToken);
    }

    protected KerberosServerSubjectAuthenticator createKerberosSubjectAuthenticator(CommonKerberosConfig kerberosConfig) {
        return new KerberosServerSubjectAuthenticator(kerberosConfig);
    }

    protected KerberosUsernamePasswordAuthenticator createKerberosUsernamePasswordAuthenticator(CommonKerberosConfig kerberosConfig) {
        return new KerberosUsernamePasswordAuthenticator(kerberosConfig);
    }

    public static boolean checkKerberosCredential(KeycloakSession session, RealmModel realm, ComponentModel model) {
        String allowKerberosCfg = model.getConfig().getFirst(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION);
        if (Boolean.valueOf(allowKerberosCfg)) {
            CredentialHelper.setOrReplaceAuthenticationRequirement(session, realm, CredentialRepresentation.KERBEROS,
                    AuthenticationExecutionModel.Requirement.ALTERNATIVE, AuthenticationExecutionModel.Requirement.DISABLED);
            return true;
        }
        return false;
    }

 }
