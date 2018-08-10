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

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.entities.CredentialAttributeEntity;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.FederatedIdentityEntity;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserConsentEntity;
import org.keycloak.models.jpa.entities.UserConsentProtocolMapperEntity;
import org.keycloak.models.jpa.entities.UserConsentRoleEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.DefaultRoles;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageProvider;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaUserProvider implements UserProvider, UserCredentialStore {

    private static final String EMAIL = "email";
    private static final String USERNAME = "username";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    private final KeycloakSession session;
    protected EntityManager em;

    public JpaUserProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }

        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setCreatedTimestamp(System.currentTimeMillis());
        entity.setUsername(username.toLowerCase());
        entity.setRealmId(realm.getId());
        em.persist(entity);
        em.flush();
        UserAdapter userModel = new UserAdapter(session, realm, em, entity);

        if (addDefaultRoles) {
            DefaultRoles.addDefaultRoles(realm, userModel);

            for (GroupModel g : realm.getDefaultGroups()) {
                userModel.joinGroupImpl(g); // No need to check if user has group as it's new user
            }
        }

        if (addDefaultRequiredActions){
            for (RequiredActionProviderModel r : realm.getRequiredActionProviders()) {
                if (r.isEnabled() && r.isDefaultAction()) {
                    userModel.addRequiredAction(r.getAlias());
                }
            }
        }

        return userModel;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return addUser(realm, KeycloakModelUtils.generateId(), username.toLowerCase(), true, true);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        UserEntity userEntity = em.find(UserEntity.class, user.getId());
        if (userEntity == null) return false;
        removeUser(userEntity);
        return true;
    }

    private void removeUser(UserEntity user) {
        String id = user.getId();
        em.createNamedQuery("deleteUserRoleMappingsByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserGroupMembershipsByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteFederatedIdentityByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserConsentRolesByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserConsentProtMappersByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserConsentsByUser").setParameter("user", user).executeUpdate();
        em.flush();
        // not sure why i have to do a clear() here.  I was getting some messed up errors that Hibernate couldn't
        // un-delete the UserEntity.
        em.clear();
        user = em.find(UserEntity.class, id);
        if (user != null) {
            em.remove(user);
        }

        em.flush();
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel identity) {
        FederatedIdentityEntity entity = new FederatedIdentityEntity();
        entity.setRealmId(realm.getId());
        entity.setIdentityProvider(identity.getIdentityProvider());
        entity.setUserId(identity.getUserId());
        entity.setUserName(identity.getUserName().toLowerCase());
        entity.setToken(identity.getToken());
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        entity.setUser(userEntity);
        em.persist(entity);
        em.flush();
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        FederatedIdentityEntity federatedIdentity = findFederatedIdentity(federatedUser, federatedIdentityModel.getIdentityProvider());

        federatedIdentity.setToken(federatedIdentityModel.getToken());

        em.persist(federatedIdentity);
        em.flush();
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String identityProvider) {
        FederatedIdentityEntity entity = findFederatedIdentity(user, identityProvider);
        if (entity != null) {
            em.remove(entity);
            em.flush();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void addConsent(RealmModel realm, String userId, UserConsentModel consent) {
        String clientId = consent.getClient().getId();

        UserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientId);
        if (consentEntity != null) {
            throw new ModelDuplicateException("Consent already exists for client [" + clientId + "] and user [" + userId + "]");
        }

        long currentTime = Time.currentTimeMillis();

        consentEntity = new UserConsentEntity();
        consentEntity.setId(KeycloakModelUtils.generateId());
        consentEntity.setUser(em.getReference(UserEntity.class, userId));
        consentEntity.setClientId(clientId);
        consentEntity.setCreatedDate(currentTime);
        consentEntity.setLastUpdatedDate(currentTime);
        em.persist(consentEntity);
        em.flush();

        updateGrantedConsentEntity(consentEntity, consent);
    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientId) {
        UserConsentEntity entity = getGrantedConsentEntity(userId, clientId);
        return toConsentModel(realm, entity);
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, String userId) {
        TypedQuery<UserConsentEntity> query = em.createNamedQuery("userConsentsByUser", UserConsentEntity.class);
        query.setParameter("userId", userId);
        List<UserConsentEntity> results = query.getResultList();

        List<UserConsentModel> consents = new ArrayList<UserConsentModel>();
        for (UserConsentEntity entity : results) {
            UserConsentModel model = toConsentModel(realm, entity);
            consents.add(model);
        }
        return consents;
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        String clientId = consent.getClient().getId();

        UserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientId);
        if (consentEntity == null) {
            throw new ModelException("Consent not found for client [" + clientId + "] and user [" + userId + "]");
        }

        updateGrantedConsentEntity(consentEntity, consent);
    }

    public boolean revokeConsentForClient(RealmModel realm, String userId, String clientId) {
        UserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientId);
        if (consentEntity == null) return false;

        em.remove(consentEntity);
        em.flush();
        return true;
    }


    private UserConsentEntity getGrantedConsentEntity(String userId, String clientId) {
        TypedQuery<UserConsentEntity> query = em.createNamedQuery("userConsentByUserAndClient", UserConsentEntity.class);
        query.setParameter("userId", userId);
        query.setParameter("clientId", clientId);
        List<UserConsentEntity> results = query.getResultList();
        if (results.size() > 1) {
            throw new ModelException("More results found for user [" + userId + "] and client [" + clientId + "]");
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            return null;
        }
    }

    private UserConsentModel toConsentModel(RealmModel realm, UserConsentEntity entity) {
        if (entity == null) {
            return null;
        }

        ClientModel client = realm.getClientById(entity.getClientId());
        if (client == null) {
            throw new ModelException("Client with id " + entity.getClientId() + " is not available");
        }
        UserConsentModel model = new UserConsentModel(client);
        model.setCreatedDate(entity.getCreatedDate());
        model.setLastUpdatedDate(entity.getLastUpdatedDate());

        Collection<UserConsentRoleEntity> grantedRoleEntities = entity.getGrantedRoles();
        if (grantedRoleEntities != null) {
            for (UserConsentRoleEntity grantedRole : grantedRoleEntities) {
                RoleModel grantedRoleModel = realm.getRoleById(grantedRole.getRoleId());
                if (grantedRoleModel != null) {
                    model.addGrantedRole(grantedRoleModel);
                }
            }
        }

        Collection<UserConsentProtocolMapperEntity> grantedProtocolMapperEntities = entity.getGrantedProtocolMappers();
        if (grantedProtocolMapperEntities != null) {

            ClientTemplateModel clientTemplate = null;
            if (client.useTemplateMappers()) {
                clientTemplate = client.getClientTemplate();
            }

            for (UserConsentProtocolMapperEntity grantedProtMapper : grantedProtocolMapperEntities) {
                ProtocolMapperModel protocolMapper = client.getProtocolMapperById(grantedProtMapper.getProtocolMapperId());

                // Fallback to client template
                if (protocolMapper == null) {
                    if (clientTemplate != null) {
                        protocolMapper = clientTemplate.getProtocolMapperById(grantedProtMapper.getProtocolMapperId());
                    }
                }

                if (protocolMapper != null) {
                    model.addGrantedProtocolMapper(protocolMapper);
                }
            }
        }

        return model;
    }

    // Update roles and protocolMappers to given consentEntity from the consentModel
    private void updateGrantedConsentEntity(UserConsentEntity consentEntity, UserConsentModel consentModel) {
        Collection<UserConsentProtocolMapperEntity> grantedProtocolMapperEntities = consentEntity.getGrantedProtocolMappers();
        Collection<UserConsentProtocolMapperEntity> mappersToRemove = new HashSet<UserConsentProtocolMapperEntity>(grantedProtocolMapperEntities);

        for (ProtocolMapperModel protocolMapper : consentModel.getGrantedProtocolMappers()) {
            UserConsentProtocolMapperEntity grantedProtocolMapperEntity = new UserConsentProtocolMapperEntity();
            grantedProtocolMapperEntity.setUserConsent(consentEntity);
            grantedProtocolMapperEntity.setProtocolMapperId(protocolMapper.getId());

            // Check if it's already there
            if (!grantedProtocolMapperEntities.contains(grantedProtocolMapperEntity)) {
                em.persist(grantedProtocolMapperEntity);
                em.flush();
                grantedProtocolMapperEntities.add(grantedProtocolMapperEntity);
            } else {
                mappersToRemove.remove(grantedProtocolMapperEntity);
            }
        }
        // Those mappers were no longer on consentModel and will be removed
        for (UserConsentProtocolMapperEntity toRemove : mappersToRemove) {
            grantedProtocolMapperEntities.remove(toRemove);
            em.remove(toRemove);
        }

        Collection<UserConsentRoleEntity> grantedRoleEntities = consentEntity.getGrantedRoles();
        Set<UserConsentRoleEntity> rolesToRemove = new HashSet<UserConsentRoleEntity>(grantedRoleEntities);
        for (RoleModel role : consentModel.getGrantedRoles()) {
            UserConsentRoleEntity consentRoleEntity = new UserConsentRoleEntity();
            consentRoleEntity.setUserConsent(consentEntity);
            consentRoleEntity.setRoleId(role.getId());

            // Check if it's already there
            if (!grantedRoleEntities.contains(consentRoleEntity)) {
                em.persist(consentRoleEntity);
                em.flush();
                grantedRoleEntities.add(consentRoleEntity);
            } else {
                rolesToRemove.remove(consentRoleEntity);
            }
        }
        // Those roles were no longer on consentModel and will be removed
        for (UserConsentRoleEntity toRemove : rolesToRemove) {
            grantedRoleEntities.remove(toRemove);
            em.remove(toRemove);
        }

        consentEntity.setLastUpdatedDate(Time.currentTimeMillis());

        em.flush();
    }


    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        int num = em.createNamedQuery("grantRoleToAllUsers")
                .setParameter("realmId", realm.getId())
                .setParameter("roleId", role.getId())
                .executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm) {
        int num = em.createNamedQuery("deleteUserConsentRolesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserConsentProtMappersByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserConsentsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserRoleMappingsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserRequiredActionsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedIdentityByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteCredentialAttributeByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteCredentialsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserAttributesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserGroupMembershipByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUsersByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
    }

    @Override
    public void removeImportedUsers(RealmModel realm, String storageProviderId) {
        int num = em.createNamedQuery("deleteUserRoleMappingsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserRequiredActionsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteFederatedIdentityByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteCredentialAttributeByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteCredentialsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserAttributesByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserGroupMembershipsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserConsentProtMappersByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserConsentRolesByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserConsentsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUsersByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
    }

    @Override
    public void unlinkUsers(RealmModel realm, String storageProviderId) {
        em.createNamedQuery("unlinkUsers")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        em.createNamedQuery("deleteUserConsentRolesByRole").setParameter("roleId", role.getId()).executeUpdate();
        em.createNamedQuery("deleteUserRoleMappingsByRole").setParameter("roleId", role.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        em.createNamedQuery("deleteUserConsentProtMappersByClient").setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("deleteUserConsentRolesByClient").setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("deleteUserConsentsByClient").setParameter("clientId", client.getId()).executeUpdate();
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        em.createNamedQuery("deleteUserConsentProtMappersByProtocolMapper")
                .setParameter("protocolMapperId", protocolMapper.getId())
                .executeUpdate();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        TypedQuery<UserEntity> query = em.createNamedQuery("groupMembership", UserEntity.class);
        query.setParameter("groupId", group.getId());
        List<UserEntity> results = query.getResultList();

        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity user : results) {
            users.add(new UserAdapter(session, realm, em, user));
        }
        return users;
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        em.createNamedQuery("deleteUserGroupMembershipsByGroup").setParameter("groupId", group.getId()).executeUpdate();

    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserById", UserEntity.class);
        query.setParameter("id", id);
        query.setParameter("realmId", realm.getId());
        List<UserEntity> entities = query.getResultList();
        if (entities.size() == 0) return null;
        return new UserAdapter(session, realm, em, entities.get(0));
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByUsername", UserEntity.class);
        query.setParameter("username", username.toLowerCase());
        query.setParameter("realmId", realm.getId());
        List<UserEntity> results = query.getResultList();
        if (results.size() == 0) return null;
        return new UserAdapter(session, realm, em, results.get(0));
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByEmail", UserEntity.class);
        query.setParameter("email", email.toLowerCase());
        query.setParameter("realmId", realm.getId());
        List<UserEntity> results = query.getResultList();
        
        if (results.isEmpty()) return null;
        
        ensureEmailConstraint(results, realm);
        
        return new UserAdapter(session, realm, em, results.get(0));
    }

     @Override
    public void close() {
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel identity, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("findUserByFederatedIdentityAndRealm", UserEntity.class);
        query.setParameter("realmId", realm.getId());
        query.setParameter("identityProvider", identity.getIdentityProvider());
        query.setParameter("userId", identity.getUserId());
        List<UserEntity> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("More results found for identityProvider=" + identity.getIdentityProvider() +
                    ", userId=" + identity.getUserId() + ", results=" + results);
        } else {
            UserEntity user = results.get(0);
            return new UserAdapter(session, realm, em, user);
        }
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByServiceAccount", UserEntity.class);
        query.setParameter("realmId", client.getRealm().getId());
        query.setParameter("clientInternalId", client.getId());
        List<UserEntity> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("More service account linked users found for client=" + client.getClientId() +
                    ", results=" + results);
        } else {
            UserEntity user = results.get(0);
            return new UserAdapter(session, client.getRealm(), em, user);
        }
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts) {
        return getUsers(realm, -1, -1, includeServiceAccounts);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        Object count = em.createNamedQuery("getRealmUserCount")
                .setParameter("realmId", realm.getId())
                .getSingleResult();
        return ((Number)count).intValue();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, false);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return getUsers(realm, firstResult, maxResults, false);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts) {
        String queryName = includeServiceAccounts ? "getAllUsersByRealm" : "getAllUsersByRealmExcludeServiceAccount" ;

        TypedQuery<UserEntity> query = em.createNamedQuery(queryName, UserEntity.class);
        query.setParameter("realmId", realm.getId());
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (UserEntity entity : results) users.add(new UserAdapter(session, realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        TypedQuery<UserEntity> query = em.createNamedQuery("groupMembership", UserEntity.class);
        query.setParameter("groupId", group.getId());
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();

        List<UserModel> users = new LinkedList<>();
        for (UserEntity user : results) {
            users.add(new UserAdapter(session, realm, em, user));
        }
        return users;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        TypedQuery<UserEntity> query = em.createNamedQuery("searchForUser", UserEntity.class);
        query.setParameter("realmId", realm.getId());
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (UserEntity entity : results) users.add(new UserAdapter(session, realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm) {
        return searchForUser(attributes, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        StringBuilder builder = new StringBuilder("select u from UserEntity u where u.realmId = :realmId");
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attribute = null;
            String parameterName = null;
            if (entry.getKey().equals(UserModel.USERNAME)) {
                attribute = "lower(u.username)";
                parameterName = JpaUserProvider.USERNAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                attribute = "lower(u.firstName)";
                parameterName = JpaUserProvider.FIRST_NAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                attribute = "lower(u.lastName)";
                parameterName = JpaUserProvider.LAST_NAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                attribute = "lower(u.email)";
                parameterName = JpaUserProvider.EMAIL;
            }
            if (attribute == null) continue;
            builder.append(" and ");
            builder.append(attribute).append(" like :").append(parameterName);
        }
        builder.append(" order by u.username");
        String q = builder.toString();
        TypedQuery<UserEntity> query = em.createQuery(q, UserEntity.class);
        query.setParameter("realmId", realm.getId());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String parameterName = null;
            if (entry.getKey().equals(UserModel.USERNAME)) {
                parameterName = JpaUserProvider.USERNAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                parameterName = JpaUserProvider.FIRST_NAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                parameterName = JpaUserProvider.LAST_NAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                parameterName = JpaUserProvider.EMAIL;
            }
            if (parameterName == null) continue;
            query.setParameter(parameterName, "%" + entry.getValue().toLowerCase() + "%");
        }
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(session, realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUsersByAttributeNameAndValue", UserEntity.class);
        query.setParameter("name", attrName);
        query.setParameter("value", attrValue);
        query.setParameter("realmId", realm.getId());
        List<UserEntity> results = query.getResultList();

        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity user : results) {
            users.add(new UserAdapter(session, realm, em, user));
        }
        return users;
    }

    private FederatedIdentityEntity findFederatedIdentity(UserModel user, String identityProvider) {
        TypedQuery<FederatedIdentityEntity> query = em.createNamedQuery("findFederatedIdentityByUserAndProvider", FederatedIdentityEntity.class);
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        query.setParameter("user", userEntity);
        query.setParameter("identityProvider", identityProvider);
        List<FederatedIdentityEntity> results = query.getResultList();
        return results.size() > 0 ? results.get(0) : null;
    }


    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        TypedQuery<FederatedIdentityEntity> query = em.createNamedQuery("findFederatedIdentityByUser", FederatedIdentityEntity.class);
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        query.setParameter("user", userEntity);
        List<FederatedIdentityEntity> results = query.getResultList();
        Set<FederatedIdentityModel> set = new HashSet<FederatedIdentityModel>();
        for (FederatedIdentityEntity entity : results) {
            set.add(new FederatedIdentityModel(entity.getIdentityProvider(), entity.getUserId(), entity.getUserName(), entity.getToken()));
        }
        return set;
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String identityProvider, RealmModel realm) {
        FederatedIdentityEntity entity = findFederatedIdentity(user, identityProvider);
        return (entity != null) ? new FederatedIdentityModel(entity.getIdentityProvider(), entity.getUserId(), entity.getUserName(), entity.getToken()) : null;
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {
        if (!component.getProviderType().equals(UserStorageProvider.class.getName())) return;
        removeImportedUsers(realm, component.getId());

    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        CredentialEntity entity = em.find(CredentialEntity.class, cred.getId());
        if (entity == null) return;
        entity.setAlgorithm(cred.getAlgorithm());
        entity.setCounter(cred.getCounter());
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setDevice(cred.getDevice());
        entity.setDigits(cred.getDigits());
        entity.setHashIterations(cred.getHashIterations());
        entity.setPeriod(cred.getPeriod());
        entity.setSalt(cred.getSalt());
        entity.setType(cred.getType());
        entity.setValue(cred.getValue());
        if (entity.getCredentialAttributes().isEmpty() && (cred.getConfig() == null || cred.getConfig().isEmpty())) {

        } else {
            MultivaluedHashMap<String, String> attrs = cred.getConfig();
            MultivaluedHashMap<String, String> config = cred.getConfig();
            if (config == null) config = new MultivaluedHashMap<>();

            Iterator<CredentialAttributeEntity> it = entity.getCredentialAttributes().iterator();
            while (it.hasNext()) {
                CredentialAttributeEntity attr = it.next();
                List<String> values = config.getList(attr.getName());
                if (values == null || !values.contains(attr.getValue())) {
                    em.remove(attr);
                    it.remove();
                } else {
                    attrs.add(attr.getName(), attr.getValue());
                }

            }
            for (String key : config.keySet()) {
                List<String> values = config.getList(key);
                List<String> attrValues = attrs.getList(key);
                for (String val : values) {
                    if (attrValues == null || !attrValues.contains(val)) {
                        CredentialAttributeEntity attr = new CredentialAttributeEntity();
                        attr.setId(KeycloakModelUtils.generateId());
                        attr.setValue(val);
                        attr.setName(key);
                        attr.setCredential(entity);
                        em.persist(attr);
                        entity.getCredentialAttributes().add(attr);
                    }
                }
            }

        }

    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        CredentialEntity entity = new CredentialEntity();
        String id = cred.getId() == null ? KeycloakModelUtils.generateId() : cred.getId();
        entity.setId(id);
        entity.setAlgorithm(cred.getAlgorithm());
        entity.setCounter(cred.getCounter());
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setDevice(cred.getDevice());
        entity.setDigits(cred.getDigits());
        entity.setHashIterations(cred.getHashIterations());
        entity.setPeriod(cred.getPeriod());
        entity.setSalt(cred.getSalt());
        entity.setType(cred.getType());
        entity.setValue(cred.getValue());
        UserEntity userRef = em.getReference(UserEntity.class, user.getId());
        entity.setUser(userRef);
        em.persist(entity);
        MultivaluedHashMap<String, String> config = cred.getConfig();
        if (config != null && !config.isEmpty()) {

            for (String key : config.keySet()) {
                List<String> values = config.getList(key);
                for (String val : values) {
                    CredentialAttributeEntity attr = new CredentialAttributeEntity();
                    attr.setId(KeycloakModelUtils.generateId());
                    attr.setValue(val);
                    attr.setName(key);
                    attr.setCredential(entity);
                    em.persist(attr);
                    entity.getCredentialAttributes().add(attr);
                }
            }

        }
        return toModel(entity);
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        CredentialEntity entity = em.find(CredentialEntity.class, id);
        if (entity == null) return false;
        em.remove(entity);
        return true;
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        CredentialEntity entity = em.find(CredentialEntity.class, id);
        if (entity == null) return null;
        CredentialModel model = toModel(entity);
        return model;
    }

    protected CredentialModel toModel(CredentialEntity entity) {
        CredentialModel model = new CredentialModel();
        model.setId(entity.getId());
        model.setType(entity.getType());
        model.setValue(entity.getValue());
        model.setAlgorithm(entity.getAlgorithm());
        model.setSalt(entity.getSalt());
        model.setPeriod(entity.getPeriod());
        model.setCounter(entity.getCounter());
        model.setCreatedDate(entity.getCreatedDate());
        model.setDevice(entity.getDevice());
        model.setDigits(entity.getDigits());
        model.setHashIterations(entity.getHashIterations());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        model.setConfig(config);
        for (CredentialAttributeEntity attr : entity.getCredentialAttributes()) {
            config.add(attr.getName(), attr.getValue());
        }
        return model;
    }

    @Override
    public List<CredentialModel> getStoredCredentials(RealmModel realm, UserModel user) {
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        TypedQuery<CredentialEntity> query = em.createNamedQuery("credentialByUser", CredentialEntity.class)
                .setParameter("user", userEntity);
        List<CredentialEntity> results = query.getResultList();
        List<CredentialModel> rtn = new LinkedList<>();
        for (CredentialEntity entity : results) {
            rtn.add(toModel(entity));
        }
        return rtn;
    }

    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel realm, UserModel user, String type) {
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        TypedQuery<CredentialEntity> query = em.createNamedQuery("credentialByUserAndType", CredentialEntity.class)
                .setParameter("type", type)
                .setParameter("user", userEntity);
        List<CredentialEntity> results = query.getResultList();
        List<CredentialModel> rtn = new LinkedList<>();
        for (CredentialEntity entity : results) {
            rtn.add(toModel(entity));
        }
        return rtn;
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        TypedQuery<CredentialEntity> query = em.createNamedQuery("credentialByNameAndType", CredentialEntity.class)
                .setParameter("type", type)
                .setParameter("device", name)
                .setParameter("user", userEntity);
        List<CredentialEntity> results = query.getResultList();
        if (results.isEmpty()) return null;
        return toModel(results.get(0));
    }

    // Could override this to provide a custom behavior.
    protected void ensureEmailConstraint(List<UserEntity> users, RealmModel realm) {
        UserEntity user = users.get(0);
        
        if (users.size() > 1) {
            // Realm settings have been changed from allowing duplicate emails to not allowing them
            // but duplicates haven't been removed.
            throw new ModelDuplicateException("Multiple users with email '" + user.getEmail() + "' exist in Keycloak.");
        }
        
        if (realm.isDuplicateEmailsAllowed()) {
            return;
        }
     
        if (user.getEmail() != null && !user.getEmail().equals(user.getEmailConstraint())) {
            // Realm settings have been changed from allowing duplicate emails to not allowing them.
            // We need to update the email constraint to reflect this change in the user entities.
            user.setEmailConstraint(user.getEmail());
            em.persist(user);
        }  
    }
}
