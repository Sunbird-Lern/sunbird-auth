/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.sessions.infinispan;

import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.*;

import org.keycloak.models.cache.infinispan.events.RemoveActionTokensSpecificEvent;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenReducedKey;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.remoting.transport.Address;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class InfinispanActionTokenStoreProviderFactory implements ActionTokenStoreProviderFactory {

    private static final Logger LOG = Logger.getLogger(InfinispanActionTokenStoreProviderFactory.class);

    private volatile Cache<ActionTokenReducedKey, ActionTokenValueEntity> actionTokenCache;

    public static final String ACTION_TOKEN_EVENTS = "ACTION_TOKEN_EVENTS";

    /**
     * If expiration is set to this value, no expiration is set on the corresponding cache entry (hence cache default is honored)
     */
    private static final int DEFAULT_CACHE_EXPIRATION = 0;

    private Config.Scope config;

    @Override
    public ActionTokenStoreProvider create(KeycloakSession session) {
        return new InfinispanActionTokenStoreProvider(session, this.actionTokenCache);
    }

    @Override
    public void init(Scope config) {
        this.config = config;
    }

    private static Cache<ActionTokenReducedKey, ActionTokenValueEntity> initActionTokenCache(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        Cache<ActionTokenReducedKey, ActionTokenValueEntity> cache = connections.getCache(InfinispanConnectionProvider.ACTION_TOKEN_CACHE);
        final Address cacheAddress = cache.getCacheManager().getAddress();

        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        cluster.registerListener(ClusterProvider.ALL, event -> {
            if (event instanceof RemoveActionTokensSpecificEvent) {
                RemoveActionTokensSpecificEvent e = (RemoveActionTokensSpecificEvent) event;

                LOG.debugf("[%s] Removing token invalidation for user+action: userId=%s, actionId=%s", cacheAddress, e.getUserId(), e.getActionId());

                AdvancedCache<ActionTokenReducedKey, ActionTokenValueEntity> localCache = cache
                  .getAdvancedCache()
                  .withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD);

                List<ActionTokenReducedKey> toRemove = localCache
                  .keySet()
                  .stream()
                  .filter(k -> Objects.equals(k.getUserId(), e.getUserId()) && Objects.equals(k.getActionId(), e.getActionId()))
                  .collect(Collectors.toList());

                toRemove.forEach(localCache::remove);
            }
        });

        LOG.debugf("[%s] Registered cluster listeners", cacheAddress);

        return cache;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        Cache<ActionTokenReducedKey, ActionTokenValueEntity> cache = this.actionTokenCache;

        // It is necessary to put the cache initialization here, otherwise the cache would be initialized lazily, that
        // means also listeners will start only after first cache initialization - that would be too late
        if (cache == null) {
            synchronized (this) {
                cache = this.actionTokenCache;
                if (cache == null) {
                    this.actionTokenCache = initActionTokenCache(factory.create());
                }
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "infinispan";
    }

}
