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

package org.keycloak.models.sessions.infinispan.mapreduce;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionMapper implements Mapper<String, SessionEntity, String, Object>, Serializable {

    public UserSessionMapper(String realm) {
        this.realm = realm;
    }

    private enum EmitValue {
        KEY, ENTITY
    }

    private String realm;

    private EmitValue emit = EmitValue.ENTITY;

    private String user;

    private Integer expired;

    private Integer expiredRefresh;

    private String brokerSessionId;
    private String brokerUserId;

    public static UserSessionMapper create(String realm) {
        return new UserSessionMapper(realm);
    }

    public UserSessionMapper emitKey() {
        emit = EmitValue.KEY;
        return this;
    }

    public UserSessionMapper user(String user) {
        this.user = user;
        return this;
    }

    public UserSessionMapper expired(Integer expired, Integer expiredRefresh) {
        this.expired = expired;
        this.expiredRefresh = expiredRefresh;
        return this;
    }

    public UserSessionMapper brokerSessionId(String id) {
        this.brokerSessionId = id;
        return this;
    }

    public UserSessionMapper brokerUserId(String id) {
        this.brokerUserId = id;
        return this;
    }

    @Override
    public void map(String key, SessionEntity e, Collector collector) {
        if (!(e instanceof UserSessionEntity)) {
            return;
        }

        UserSessionEntity entity = (UserSessionEntity) e;

        if (!realm.equals(entity.getRealm())) {
            return;
        }

        if (user != null && !entity.getUser().equals(user)) {
            return;
        }

        if (brokerSessionId != null && !brokerSessionId.equals(entity.getBrokerSessionId())) return;
        if (brokerUserId != null && !brokerUserId.equals(entity.getBrokerUserId())) return;

        if (expired != null && expiredRefresh != null && entity.getStarted() > expired && entity.getLastSessionRefresh() > expiredRefresh) {
            return;
        }

        if (expired == null && expiredRefresh != null && entity.getLastSessionRefresh() > expiredRefresh) {
            return;
        }

        switch (emit) {
            case KEY:
                collector.emit(key, key);
                break;
            case ENTITY:
                collector.emit(key, entity);
                break;
        }
    }

}
