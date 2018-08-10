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

package org.keycloak.models.cache.infinispan;

import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.cache.infinispan.entities.CachedFederatedIdentityLinks;
import org.keycloak.models.cache.infinispan.entities.CachedUser;
import org.keycloak.models.cache.infinispan.entities.CachedUserConsent;
import org.keycloak.models.cache.infinispan.entities.CachedUserConsents;
import org.keycloak.models.cache.infinispan.entities.UserListQuery;
import org.keycloak.models.cache.infinispan.events.UserCacheRealmInvalidationEvent;
import org.keycloak.models.cache.infinispan.events.UserConsentsUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.UserFederationLinkRemovedEvent;
import org.keycloak.models.cache.infinispan.events.UserFederationLinkUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.UserFullInvalidationEvent;
import org.keycloak.models.cache.infinispan.events.UserUpdatedEvent;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserCacheSession implements UserCache {
    protected static final Logger logger = Logger.getLogger(UserCacheSession.class);
    protected UserCacheManager cache;
    protected KeycloakSession session;
    protected UserProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;
    protected final long startupRevision;


    protected Set<String> invalidations = new HashSet<>();
    protected Set<String> realmInvalidations = new HashSet<>();
    protected Set<InvalidationEvent> invalidationEvents = new HashSet<>(); // Events to be sent across cluster
    protected Map<String, UserModel> managedUsers = new HashMap<>();

    public UserCacheSession(UserCacheManager cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;
        this.startupRevision = cache.getCurrentCounter();
        session.getTransactionManager().enlistAfterCompletion(getTransaction());
    }

    @Override
    public void clear() {
        cache.clear();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        cluster.notify(InfinispanUserCacheProviderFactory.USER_CLEAR_CACHE_EVENTS, new ClearCacheEvent(), true);
    }

    public UserProvider getDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (delegate != null) return delegate;
        delegate = session.userStorageManager();

        return delegate;
    }

    public void registerUserInvalidation(RealmModel realm,CachedUser user) {
        cache.userUpdatedInvalidations(user.getId(), user.getUsername(), user.getEmail(), user.getRealm(), invalidations);
        invalidationEvents.add(UserUpdatedEvent.create(user.getId(), user.getUsername(), user.getEmail(), user.getRealm()));
    }

    @Override
    public void evict(RealmModel realm, UserModel user) {
        if (user instanceof CachedUserModel) {
            ((CachedUserModel)user).invalidate();
        } else {
            cache.userUpdatedInvalidations(user.getId(), user.getUsername(), user.getEmail(), realm.getId(), invalidations);
            invalidationEvents.add(UserUpdatedEvent.create(user.getId(), user.getUsername(), user.getEmail(), realm.getId()));
        }
    }

    @Override
    public void evict(RealmModel realm) {
        addRealmInvalidation(realm.getId());
    }

    protected void runInvalidations() {
        for (String realmId : realmInvalidations) {
            cache.invalidateRealmUsers(realmId, invalidations);
        }
        for (String invalidation : invalidations) {
            cache.invalidateObject(invalidation);
        }

        cache.sendInvalidationEvents(session, invalidationEvents);
    }

    private KeycloakTransaction getTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                if (delegate == null) return;
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void rollback() {
                setRollbackOnly = true;
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void setRollbackOnly() {
                setRollbackOnly = true;
            }

            @Override
            public boolean getRollbackOnly() {
                return setRollbackOnly;
            }

            @Override
            public boolean isActive() {
                return transactionActive;
            }
        };
    }

    private boolean isRegisteredForInvalidation(RealmModel realm, String userId) {
        return realmInvalidations.contains(realm.getId()) || invalidations.contains(userId);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        logger.tracev("getuserById {0}", id);
        if (isRegisteredForInvalidation(realm, id)) {
            logger.trace("registered for invalidation return delegate");
            return getDelegate().getUserById(id, realm);
        }
        if (managedUsers.containsKey(id)) {
            logger.trace("return managedusers");
            return managedUsers.get(id);
        }

        CachedUser cached = cache.get(id, CachedUser.class);
        UserModel adapter = null;
        if (cached == null) {
            logger.trace("not cached");
            Long loaded = cache.getCurrentRevision(id);
            UserModel delegate = getDelegate().getUserById(id, realm);
            if (delegate == null) {
                logger.trace("delegate returning null");
                return null;
            }
            adapter = cacheUser(realm, delegate, loaded);
        } else {
            adapter = validateCache(realm, cached);
        }
        managedUsers.put(id, adapter);
        return adapter;
    }

    static String getUserByUsernameCacheKey(String realmId, String username) {
        return realmId + ".username." + username;
    }

    static String getUserByEmailCacheKey(String realmId, String email) {
        return realmId + ".email." + email;
    }

    private static String getUserByFederatedIdentityCacheKey(String realmId, FederatedIdentityModel socialLink) {
        return getUserByFederatedIdentityCacheKey(realmId, socialLink.getIdentityProvider(), socialLink.getUserId());
    }

    static String getUserByFederatedIdentityCacheKey(String realmId, String identityProvider, String socialUserId) {
        return realmId + ".idp." + identityProvider + "." + socialUserId;
    }

    static String getFederatedIdentityLinksCacheKey(String userId) {
        return userId + ".idplinks";
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        logger.tracev("getUserByUsername: {0}", username);
        username = username.toLowerCase();
        if (realmInvalidations.contains(realm.getId())) {
            logger.tracev("realmInvalidations");
            return getDelegate().getUserByUsername(username, realm);
        }
        String cacheKey = getUserByUsernameCacheKey(realm.getId(), username);
        if (invalidations.contains(cacheKey)) {
            logger.tracev("invalidations");
            return getDelegate().getUserByUsername(username, realm);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        String userId = null;
        if (query == null) {
            logger.tracev("query null");
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getUserByUsername(username, realm);
            if (model == null) {
                logger.tracev("model from delegate null");
                return null;
            }
            userId = model.getId();
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) {
                logger.tracev("return managed user");
                return managedUsers.get(userId);
            }

            UserModel adapter = getUserAdapter(realm, userId, loaded, model);
            if (adapter instanceof UserAdapter) { // this was cached, so we can cache query too
                query = new UserListQuery(loaded, cacheKey, realm, model.getId());
                cache.addRevisioned(query, startupRevision);
            }
            managedUsers.put(userId, adapter);
            return adapter;
        } else {
            userId = query.getUsers().iterator().next();
            if (invalidations.contains(userId)) {
                logger.tracev("invalidated cache return delegate");
                return getDelegate().getUserByUsername(username, realm);

            }
            logger.trace("return getUserById");
            return getUserById(userId, realm);
        }
    }

    protected UserModel getUserAdapter(RealmModel realm, String userId, Long loaded, UserModel delegate) {
        CachedUser cached = cache.get(userId, CachedUser.class);
        if (cached == null) {
            return cacheUser(realm, delegate, loaded);
        } else {
            return validateCache(realm, cached);
        }
    }

    protected UserModel validateCache(RealmModel realm, CachedUser cached) {
        if (!realm.getId().equals(cached.getRealm())) {
            return null;
        }

        StorageId storageId = new StorageId(cached.getId());
        if (!storageId.isLocal()) {
            ComponentModel component = realm.getComponent(storageId.getProviderId());
            UserStorageProviderModel model = new UserStorageProviderModel(component);
            UserStorageProviderModel.CachePolicy policy = model.getCachePolicy();
            // although we do set a timeout, Infinispan has no guarantees when the user will be evicted
            // its also hard to test stuff
            boolean invalidate = false;
            if (policy != null) {
                //String currentTime = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date(Time.currentTimeMillis()));
                if (policy == UserStorageProviderModel.CachePolicy.NO_CACHE) {
                    invalidate = true;
                } else if (cached.getCacheTimestamp() < model.getCacheInvalidBefore()) {
                    invalidate = true;
                } else if (policy == UserStorageProviderModel.CachePolicy.MAX_LIFESPAN) {
                    if (cached.getCacheTimestamp() + model.getMaxLifespan() < Time.currentTimeMillis()) {
                        invalidate = true;
                    }
                } else if (policy == UserStorageProviderModel.CachePolicy.EVICT_DAILY) {
                    long dailyTimeout = dailyTimeout(model.getEvictionHour(), model.getEvictionMinute());
                    dailyTimeout = dailyTimeout - (24 * 60 * 60 * 1000);
                    //String timeout = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date(dailyTimeout));
                    //String stamp = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date(cached.getCacheTimestamp()));
                    if (cached.getCacheTimestamp() <= dailyTimeout) {
                        invalidate = true;
                    }
                } else if (policy == UserStorageProviderModel.CachePolicy.EVICT_WEEKLY) {
                    int oneWeek = 7 * 24 * 60 * 60 * 1000;
                    long weeklyTimeout = weeklyTimeout(model.getEvictionDay(), model.getEvictionHour(), model.getEvictionMinute());
                    long lastTimeout = weeklyTimeout - oneWeek;
                    //String timeout = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date(weeklyTimeout));
                    //String stamp = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date(cached.getCacheTimestamp()));
                    if (cached.getCacheTimestamp() <= lastTimeout) {
                        invalidate = true;
                    }
                }
            }
            if (invalidate) {
                registerUserInvalidation(realm, cached);
                return getDelegate().getUserById(cached.getId(), realm);
            }
        }
        return new UserAdapter(cached, this, session, realm);
    }

    protected UserModel cacheUser(RealmModel realm, UserModel delegate, Long revision) {
        StorageId storageId = new StorageId(delegate.getId());
        CachedUser cached = null;
        if (!storageId.isLocal()) {
            ComponentModel component = realm.getComponent(storageId.getProviderId());
            UserStorageProviderModel model = new UserStorageProviderModel(component);
            UserStorageProviderModel.CachePolicy policy = model.getCachePolicy();
            if (policy != null && policy == UserStorageProviderModel.CachePolicy.NO_CACHE) {
                return delegate;
            }
            cached = new CachedUser(revision, realm, delegate);
            if (policy == null || policy == UserStorageProviderModel.CachePolicy.DEFAULT) {
                cache.addRevisioned(cached, startupRevision);
            } else {
                long lifespan = -1;
                if (policy == UserStorageProviderModel.CachePolicy.EVICT_DAILY) {
                    if (model.getEvictionHour() > -1 && model.getEvictionMinute() > -1) {
                        lifespan = dailyTimeout(model.getEvictionHour(), model.getEvictionMinute()) - Time.currentTimeMillis();
                    }
                } else if (policy == UserStorageProviderModel.CachePolicy.EVICT_WEEKLY) {
                    if (model.getEvictionDay() > 0 && model.getEvictionHour() > -1 && model.getEvictionMinute() > -1) {
                        lifespan = weeklyTimeout(model.getEvictionDay(), model.getEvictionHour(), model.getEvictionMinute()) - Time.currentTimeMillis();
                    }
                } else if (policy == UserStorageProviderModel.CachePolicy.MAX_LIFESPAN) {
                    lifespan = model.getMaxLifespan();
                }
                if (lifespan > 0) {
                    cache.addRevisioned(cached, startupRevision, lifespan);
                } else {
                    cache.addRevisioned(cached, startupRevision);
                }
            }
        } else {
            cached = new CachedUser(revision, realm, delegate);
            cache.addRevisioned(cached, startupRevision);
        }
        UserAdapter adapter = new UserAdapter(cached, this, session, realm);
        onCache(realm, adapter, delegate);
        return adapter;

    }


    public static long dailyTimeout(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal.setTimeInMillis(Time.currentTimeMillis());
        cal2.setTimeInMillis(Time.currentTimeMillis());
        cal2.set(Calendar.HOUR_OF_DAY, hour);
        cal2.set(Calendar.MINUTE, minute);
        if (cal2.getTimeInMillis() < cal.getTimeInMillis()) {
            int add = (24 * 60 * 60 * 1000);
            cal.add(Calendar.MILLISECOND, add);
        } else {
            cal.add(Calendar.MILLISECOND, (int)(cal2.getTimeInMillis() - cal.getTimeInMillis()));
        }
        return cal.getTimeInMillis();
    }

    public static long weeklyTimeout(int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal.setTimeInMillis(Time.currentTimeMillis());
        cal2.setTimeInMillis(Time.currentTimeMillis());
        cal2.set(Calendar.HOUR_OF_DAY, hour);
        cal2.set(Calendar.MINUTE, minute);
        cal2.set(Calendar.DAY_OF_WEEK, day);
        if (cal2.getTimeInMillis() < cal.getTimeInMillis()) {
            int add = (7 * 24 * 60 * 60 * 1000);
            cal2.add(Calendar.MILLISECOND, add);
        }

        return cal2.getTimeInMillis();
    }

    private void onCache(RealmModel realm, UserAdapter adapter, UserModel delegate) {
        ((OnUserCache)getDelegate()).onCache(realm, adapter, delegate);
        ((OnUserCache)session.userCredentialManager()).onCache(realm, adapter, delegate);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        if (email == null) return null;
        email = email.toLowerCase();
        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserByEmail(email, realm);
        }
        String cacheKey = getUserByEmailCacheKey(realm.getId(), email);
        if (invalidations.contains(cacheKey)) {
            return getDelegate().getUserByEmail(email, realm);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        String userId = null;
        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getUserByEmail(email, realm);
            if (model == null) return null;
            userId = model.getId();
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) return managedUsers.get(userId);

            UserModel adapter = getUserAdapter(realm, userId, loaded, model);
            if (adapter instanceof UserAdapter) {
                query = new UserListQuery(loaded, cacheKey, realm, model.getId());
                cache.addRevisioned(query, startupRevision);
            }
            managedUsers.put(userId, adapter);
            return adapter;
        } else {
            userId = query.getUsers().iterator().next();
            if (invalidations.contains(userId)) {
                return getDelegate().getUserByEmail(email, realm);

            }
            return getUserById(userId, realm);
        }
    }

    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        if (socialLink == null) return null;
        if (!realm.isIdentityFederationEnabled()) return null;

        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserByFederatedIdentity(socialLink, realm);
        }
        String cacheKey = getUserByFederatedIdentityCacheKey(realm.getId(), socialLink);
        if (invalidations.contains(cacheKey)) {
            return getDelegate().getUserByFederatedIdentity(socialLink, realm);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        String userId = null;
        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getUserByFederatedIdentity(socialLink, realm);
            if (model == null) return null;
            userId = model.getId();
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) return managedUsers.get(userId);

            UserModel adapter = getUserAdapter(realm, userId, loaded, model);
            if (adapter instanceof UserAdapter) {
                query = new UserListQuery(loaded, cacheKey, realm, model.getId());
                cache.addRevisioned(query, startupRevision);
            }

            managedUsers.put(userId, adapter);
            return adapter;
        } else {
            userId = query.getUsers().iterator().next();
            if (invalidations.contains(userId)) {
                invalidations.add(cacheKey);
                return getDelegate().getUserByFederatedIdentity(socialLink, realm);

            }
            return getUserById(userId, realm);
        }
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return getDelegate().getGroupMembers(realm, group, firstResult, maxResults);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getDelegate().getGroupMembers(realm, group);
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        // Just an attempt to find the user from cache by default serviceAccount username
        UserModel user = findServiceAccount(client);
        if (user != null && user.getServiceAccountClientLink() != null && user.getServiceAccountClientLink().equals(client.getId())) {
            return user;
        }

        return getDelegate().getServiceAccount(client);
    }

    public UserModel findServiceAccount(ClientModel client) {
        String username = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId();
        logger.tracev("getServiceAccount: {0}", username);
        username = username.toLowerCase();
        RealmModel realm = client.getRealm();
        if (realmInvalidations.contains(realm.getId())) {
            logger.tracev("realmInvalidations");
            return getDelegate().getServiceAccount(client);
        }
        String cacheKey = getUserByUsernameCacheKey(realm.getId(), username);
        if (invalidations.contains(cacheKey)) {
            logger.tracev("invalidations");
            return getDelegate().getServiceAccount(client);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        String userId = null;
        if (query == null) {
            logger.tracev("query null");
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getServiceAccount(client);
            if (model == null) {
                logger.tracev("model from delegate null");
                return null;
            }
            userId = model.getId();
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) {
                logger.tracev("return managed user");
                return managedUsers.get(userId);
            }

            UserModel adapter = getUserAdapter(realm, userId, loaded, model);
            if (adapter instanceof UserAdapter) { // this was cached, so we can cache query too
                query = new UserListQuery(loaded, cacheKey, realm, model.getId());
                cache.addRevisioned(query, startupRevision);
            }
            managedUsers.put(userId, adapter);
            return adapter;
        } else {
            userId = query.getUsers().iterator().next();
            if (invalidations.contains(userId)) {
                logger.tracev("invalidated cache return delegate");
                return getDelegate().getUserByUsername(username, realm);

            }
            logger.trace("return getUserById");
            return getUserById(userId, realm);
        }
    }




    @Override
    public List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts) {
        return getDelegate().getUsers(realm, includeServiceAccounts);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return getDelegate().getUsersCount(realm);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts) {
        return getDelegate().getUsers(realm, firstResult, maxResults, includeServiceAccounts);
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
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return getDelegate().searchForUser(search, realm);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return getDelegate().searchForUser(search, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm) {
        return getDelegate().searchForUser(attributes, realm);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        return getDelegate().searchForUser(attributes, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return getDelegate().searchForUserByUserAttribute(attrName, attrValue, realm);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        logger.tracev("getFederatedIdentities: {0}", user.getUsername());

        String cacheKey = getFederatedIdentityLinksCacheKey(user.getId());
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(user.getId()) || invalidations.contains(cacheKey)) {
            return getDelegate().getFederatedIdentities(user, realm);
        }

        CachedFederatedIdentityLinks cachedLinks = cache.get(cacheKey, CachedFederatedIdentityLinks.class);

        if (cachedLinks == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            Set<FederatedIdentityModel> federatedIdentities = getDelegate().getFederatedIdentities(user, realm);
            cachedLinks = new CachedFederatedIdentityLinks(loaded, cacheKey, realm, federatedIdentities);
            cache.addRevisioned(cachedLinks, startupRevision);
            return federatedIdentities;
        } else {
            return new HashSet<>(cachedLinks.getFederatedIdentities());
        }
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        logger.tracev("getFederatedIdentity: {0} {1}", user.getUsername(), socialProvider);

        String cacheKey = getFederatedIdentityLinksCacheKey(user.getId());
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(user.getId()) || invalidations.contains(cacheKey)) {
            return getDelegate().getFederatedIdentity(user, socialProvider, realm);
        }

        Set<FederatedIdentityModel> federatedIdentities = getFederatedIdentities(user, realm);
        for (FederatedIdentityModel socialLink : federatedIdentities) {
            if (socialLink.getIdentityProvider().equals(socialProvider)) {
                return socialLink;
            }
        }
        return null;
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        invalidateConsent(userId);
        getDelegate().updateConsent(realm, userId, consent);
    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId) {
        invalidateConsent(userId);
        return getDelegate().revokeConsentForClient(realm, userId, clientInternalId);
    }

    static String getConsentCacheKey(String userId) {
        return userId + ".consents";
    }


    @Override
    public void addConsent(RealmModel realm, String userId, UserConsentModel consent) {
        invalidateConsent(userId);
        getDelegate().addConsent(realm, userId, consent);
    }

    private void invalidateConsent(String userId) {
        cache.consentInvalidation(userId, invalidations);
        invalidationEvents.add(UserConsentsUpdatedEvent.create(userId));
    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientId) {
        logger.tracev("getConsentByClient: {0}", userId);

        String cacheKey = getConsentCacheKey(userId);
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(userId) || invalidations.contains(cacheKey)) {
            return getDelegate().getConsentByClient(realm, userId, clientId);
        }

        CachedUserConsents cached = cache.get(cacheKey, CachedUserConsents.class);

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<UserConsentModel> consents = getDelegate().getConsents(realm, userId);
            cached = new CachedUserConsents(loaded, cacheKey, realm, consents);
            cache.addRevisioned(cached, startupRevision);
        }
        CachedUserConsent cachedConsent = cached.getConsents().get(clientId);
        if (cachedConsent == null) return null;
        return toConsentModel(realm, cachedConsent);
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, String userId) {
        logger.tracev("getConsents: {0}", userId);

        String cacheKey = getConsentCacheKey(userId);
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(userId) || invalidations.contains(cacheKey)) {
            return getDelegate().getConsents(realm, userId);
        }

        CachedUserConsents cached = cache.get(cacheKey, CachedUserConsents.class);

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<UserConsentModel> consents = getDelegate().getConsents(realm, userId);
            cached = new CachedUserConsents(loaded, cacheKey, realm, consents);
            cache.addRevisioned(cached, startupRevision);
            return consents;
        } else {
            List<UserConsentModel> result = new LinkedList<>();
            for (CachedUserConsent cachedConsent : cached.getConsents().values()) {
                UserConsentModel consent = toConsentModel(realm, cachedConsent);
                if (consent != null) {
                    result.add(consent);
                }
            }
            return result;
        }
    }

    private UserConsentModel toConsentModel(RealmModel realm, CachedUserConsent cachedConsent) {
        ClientModel client = session.realms().getClientById(cachedConsent.getClientDbId(), realm);
        if (client == null) {
            return null;
        }

        UserConsentModel consentModel = new UserConsentModel(client);
        consentModel.setCreatedDate(cachedConsent.getCreatedDate());
        consentModel.setLastUpdatedDate(cachedConsent.getLastUpdatedDate());

        for (String roleId : cachedConsent.getRoleIds()) {
            RoleModel role = session.realms().getRoleById(roleId, realm);
            if (role != null) {
                consentModel.addGrantedRole(role);
            }
        }
        for (ProtocolMapperModel protocolMapper : cachedConsent.getProtocolMappers()) {
            consentModel.addGrantedProtocolMapper(protocolMapper);
        }
        return consentModel;
    }


    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        UserModel user = getDelegate().addUser(realm, id, username, addDefaultRoles, addDefaultRoles);
        // just in case the transaction is rolled back you need to invalidate the user and all cache queries for that user
        fullyInvalidateUser(realm, user);
        managedUsers.put(user.getId(), user);
        return user;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserModel user = getDelegate().addUser(realm, username);
        // just in case the transaction is rolled back you need to invalidate the user and all cache queries for that user
        fullyInvalidateUser(realm, user);
        managedUsers.put(user.getId(), user);
        return user;
    }

    // just in case the transaction is rolled back you need to invalidate the user and all cache queries for that user
    protected void fullyInvalidateUser(RealmModel realm, UserModel user) {
        Set<FederatedIdentityModel> federatedIdentities = realm.isIdentityFederationEnabled() ? getFederatedIdentities(user, realm) : null;

        UserFullInvalidationEvent event = UserFullInvalidationEvent.create(user.getId(), user.getUsername(), user.getEmail(), realm.getId(), realm.isIdentityFederationEnabled(), federatedIdentities);

        cache.fullUserInvalidation(user.getId(), user.getUsername(), user.getEmail(), realm.getId(), realm.isIdentityFederationEnabled(), event.getFederatedIdentities(), invalidations);
        invalidationEvents.add(event);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        fullyInvalidateUser(realm, user);
        return getDelegate().removeUser(realm, user);
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        invalidateFederationLink(user.getId());
        getDelegate().addFederatedIdentity(realm, user, socialLink);
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        invalidateFederationLink(federatedUser.getId());
        getDelegate().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);
    }

    private void invalidateFederationLink(String userId) {
        cache.federatedIdentityLinkUpdatedInvalidation(userId, invalidations);
        invalidationEvents.add(UserFederationLinkUpdatedEvent.create(userId));
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        // Needs to invalidate both directions
        FederatedIdentityModel socialLink = getFederatedIdentity(user, socialProvider, realm);

        UserFederationLinkRemovedEvent event = UserFederationLinkRemovedEvent.create(user.getId(), realm.getId(), socialLink);
        cache.federatedIdentityLinkRemovedInvalidation(user.getId(), realm.getId(), event.getIdentityProviderId(), event.getSocialUserId(), invalidations);
        invalidationEvents.add(event);

        return getDelegate().removeFederatedIdentity(realm, user, socialProvider);
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
        getDelegate().grantToAllUsers(realm, role);
    }

    @Override
    public void preRemove(RealmModel realm) {
        addRealmInvalidation(realm.getId());
        getDelegate().preRemove(realm);
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, role);
    }
    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, group);
    }


    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, client);
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        getDelegate().preRemove(protocolMapper);
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {
        if (!component.getProviderType().equals(UserStorageProvider.class.getName())) return;
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, component);

    }

    @Override
    public void removeImportedUsers(RealmModel realm, String storageProviderId) {
        getDelegate().removeImportedUsers(realm, storageProviderId);
        clear();
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
    }

    @Override
    public void unlinkUsers(RealmModel realm, String storageProviderId) {
        getDelegate().unlinkUsers(realm, storageProviderId);
        clear();
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm

    }

    private void addRealmInvalidation(String realmId) {
        realmInvalidations.add(realmId);
        invalidationEvents.add(UserCacheRealmInvalidationEvent.create(realmId));
    }

}
