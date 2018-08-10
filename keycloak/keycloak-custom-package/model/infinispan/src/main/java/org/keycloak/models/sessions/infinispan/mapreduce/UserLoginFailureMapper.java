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
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserLoginFailureMapper implements Mapper<LoginFailureKey, LoginFailureEntity, LoginFailureKey, Object>, Serializable {

    public UserLoginFailureMapper(String realm) {
        this.realm = realm;
    }

    private enum EmitValue {
        KEY, ENTITY
    }

    private String realm;

    private EmitValue emit = EmitValue.ENTITY;

    public static UserLoginFailureMapper create(String realm) {
        return new UserLoginFailureMapper(realm);
    }

    public UserLoginFailureMapper emitKey() {
        emit = EmitValue.KEY;
        return this;
    }

    @Override
    public void map(LoginFailureKey key, LoginFailureEntity e, Collector collector) {
        if (!realm.equals(e.getRealm())) {
            return;
        }

        switch (emit) {
            case KEY:
                collector.emit(key, key);
                break;
            case ENTITY:
                collector.emit(key, e);
                break;
        }
    }

}
