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

package org.keycloak.storage.ldap.mappers;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.user.SynchronizationResult;

import javax.naming.AuthenticationException;
import java.util.Collections;
import java.util.List;

/**
 * Stateful per-request object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractLDAPStorageMapper implements LDAPStorageMapper {

    protected final KeycloakSession session;
    protected final ComponentModel mapperModel;
    protected final LDAPStorageProvider ldapProvider;

    public AbstractLDAPStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        this.mapperModel = mapperModel;
        this.ldapProvider = ldapProvider;
        this.session = ldapProvider.getSession();
    }


    public SynchronizationResult syncDataFromFederationProviderToKeycloak(RealmModel realm) {
        return new SynchronizationResult();
    }


    public SynchronizationResult syncDataFromKeycloakToFederationProvider(RealmModel realm) {
        return new SynchronizationResult();
    }


    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }


    public boolean onAuthenticationFailure(LDAPObject ldapUser, UserModel user, AuthenticationException ldapException, RealmModel realm) {
        return false;
    }


    public static boolean parseBooleanParameter(ComponentModel mapperModel, String paramName) {
        String paramm = mapperModel.getConfig().getFirst(paramName);
        return Boolean.parseBoolean(paramm);
    }


    public LDAPStorageProvider getLdapProvider() {
        return ldapProvider;
    }


    @Override
    public void close() {

    }

}
