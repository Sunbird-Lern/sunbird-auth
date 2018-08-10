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

package org.keycloak.models.sessions.infinispan;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.sessions.infinispan.entities.AuthenticationSessionEntity;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * NOTE: Calling setter doesn't automatically enlist for update
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionAdapter implements AuthenticationSessionModel {

    private KeycloakSession session;
    private InfinispanAuthenticationSessionProvider provider;
    private Cache<String, AuthenticationSessionEntity> cache;
    private RealmModel realm;
    private AuthenticationSessionEntity entity;

    public AuthenticationSessionAdapter(KeycloakSession session, InfinispanAuthenticationSessionProvider provider, Cache<String, AuthenticationSessionEntity> cache, RealmModel realm,
                                        AuthenticationSessionEntity entity) {
        this.session = session;
        this.provider = provider;
        this.cache = cache;
        this.realm = realm;
        this.entity = entity;
    }

    void update() {
        provider.tx.replace(cache, entity.getId(), entity);
    }


    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public ClientModel getClient() {
        return realm.getClientById(entity.getClientUuid());
    }

    @Override
    public String getRedirectUri() {
        return entity.getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
        entity.setRedirectUri(uri);
        update();
    }

    @Override
    public int getTimestamp() {
        return entity.getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        entity.setTimestamp(timestamp);
        update();
    }

    @Override
    public String getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(String action) {
        entity.setAction(action);
        update();
    }

    @Override
    public Set<String> getRoles() {
        if (entity.getRoles() == null || entity.getRoles().isEmpty()) return Collections.emptySet();
        return new HashSet<>(entity.getRoles());
    }

    @Override
    public void setRoles(Set<String> roles) {
        entity.setRoles(roles);
        update();
    }

    @Override
    public Set<String> getProtocolMappers() {
        if (entity.getProtocolMappers() == null || entity.getProtocolMappers().isEmpty()) return Collections.emptySet();
        return new HashSet<>(entity.getProtocolMappers());
    }

    @Override
    public void setProtocolMappers(Set<String> protocolMappers) {
        entity.setProtocolMappers(protocolMappers);
        update();
    }

    @Override
    public String getProtocol() {
        return entity.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        entity.setProtocol(protocol);
        update();
    }

    @Override
    public String getClientNote(String name) {
        return (entity.getClientNotes() != null && name != null) ? entity.getClientNotes().get(name) : null;
    }

    @Override
    public void setClientNote(String name, String value) {
        if (entity.getClientNotes() == null) {
            entity.setClientNotes(new ConcurrentHashMap<>());
        }
        if (name != null) {
            if (value == null) {
                entity.getClientNotes().remove(name);
            } else {
                entity.getClientNotes().put(name, value);
            }
        }
        update();
    }

    @Override
    public void removeClientNote(String name) {
        if (entity.getClientNotes() != null && name != null) {
            entity.getClientNotes().remove(name);
        }
        update();
    }

    @Override
    public Map<String, String> getClientNotes() {
        if (entity.getClientNotes() == null || entity.getClientNotes().isEmpty()) return Collections.emptyMap();
        Map<String, String> copy = new ConcurrentHashMap<>();
        copy.putAll(entity.getClientNotes());
        return copy;
    }

    @Override
    public void clearClientNotes() {
        entity.setClientNotes(new ConcurrentHashMap<>());
        update();
    }

    @Override
    public String getAuthNote(String name) {
        return (entity.getAuthNotes() != null && name != null) ? entity.getAuthNotes().get(name) : null;
    }

    @Override
    public void setAuthNote(String name, String value) {
        if (entity.getAuthNotes() == null) {
            entity.setAuthNotes(new ConcurrentHashMap<>());
        }
        if (name != null) {
            if (value == null) {
                entity.getAuthNotes().remove(name);
            } else {
                entity.getAuthNotes().put(name, value);
            }
        }
        update();
    }

    @Override
    public void removeAuthNote(String name) {
        if (entity.getAuthNotes() != null && name != null) {
            entity.getAuthNotes().remove(name);
        }
        update();
    }

    @Override
    public void clearAuthNotes() {
        entity.setAuthNotes(new ConcurrentHashMap<>());
        update();
    }

    @Override
    public void setUserSessionNote(String name, String value) {
        if (entity.getUserSessionNotes() == null) {
            entity.setUserSessionNotes(new ConcurrentHashMap<>());
        }
        if (name != null) {
            if (value == null) {
                entity.getUserSessionNotes().remove(name);
            } else {
                entity.getUserSessionNotes().put(name, value);
            }
        }
        update();

    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        if (entity.getUserSessionNotes() == null) {
            return Collections.EMPTY_MAP;
        }
        ConcurrentHashMap<String, String> copy = new ConcurrentHashMap<>();
        copy.putAll(entity.getUserSessionNotes());
        return copy;
    }

    @Override
    public void clearUserSessionNotes() {
        entity.setUserSessionNotes(new ConcurrentHashMap<>());
        update();

    }

    @Override
    public Set<String> getRequiredActions() {
        Set<String> copy = new HashSet<>();
        copy.addAll(entity.getRequiredActions());
        return copy;
    }

    @Override
    public void addRequiredAction(String action) {
        entity.getRequiredActions().add(action);
        update();

    }

    @Override
    public void removeRequiredAction(String action) {
        entity.getRequiredActions().remove(action);
        update();

    }

    @Override
    public void addRequiredAction(UserModel.RequiredAction action) {
        addRequiredAction(action.name());
    }

    @Override
    public void removeRequiredAction(UserModel.RequiredAction action) {
        removeRequiredAction(action.name());
    }

    @Override
    public Map<String, AuthenticationSessionModel.ExecutionStatus> getExecutionStatus() {

        return entity.getExecutionStatus();
    }

    @Override
    public void setExecutionStatus(String authenticator, AuthenticationSessionModel.ExecutionStatus status) {
        entity.getExecutionStatus().put(authenticator, status);
        update();

    }

    @Override
    public void clearExecutionStatus() {
        entity.getExecutionStatus().clear();
        update();
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return entity.getAuthUserId() == null ? null : session.users().getUserById(entity.getAuthUserId(), realm);    }

    @Override
    public void setAuthenticatedUser(UserModel user) {
        if (user == null) entity.setAuthUserId(null);
        else entity.setAuthUserId(user.getId());
        update();
    }

    @Override
    public void updateClient(ClientModel client) {
        entity.setClientUuid(client.getId());
        update();
    }

    @Override
    public void restartSession(RealmModel realm, ClientModel client) {
        String id = entity.getId();
        entity = new AuthenticationSessionEntity();
        entity.setId(id);
        entity.setRealm(realm.getId());
        entity.setClientUuid(client.getId());
        entity.setTimestamp(Time.currentTime());
        update();
    }
}
