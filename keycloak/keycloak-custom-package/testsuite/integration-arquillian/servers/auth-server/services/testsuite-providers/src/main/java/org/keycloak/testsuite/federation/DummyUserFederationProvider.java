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

package org.keycloak.testsuite.federation;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DummyUserFederationProvider implements UserStorageProvider,
        UserLookupProvider,
        UserRegistrationProvider,
        CredentialInputValidator {

    private final Map<String, UserModel> users;
    private KeycloakSession session;
    private ComponentModel component;



    public DummyUserFederationProvider(KeycloakSession session, ComponentModel component, Map<String, UserModel> users) {
        this.users = users;
        this.session = session;
        this.component = component;
    }



    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserModel local = session.userLocalStorage().addUser(realm, username);
        local.setFederationLink(component.getId());

        users.put(username, local);
        return local;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return users.remove(user.getUsername()) != null;
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return null;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        return users.get(username);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    public Set<String> getSupportedCredentialTypes() {
        return Collections.singleton(UserCredentialModel.PASSWORD);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getSupportedCredentialTypes().contains(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!CredentialModel.PASSWORD.equals(credentialType)) return false;

        if (user.getUsername().equals("test-user")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (user.getUsername().equals("test-user")) {
            UserCredentialModel password = (UserCredentialModel)input;
            if (password.getType().equals(UserCredentialModel.PASSWORD)) {
                return "secret".equals(password.getValue());
            }
        }
        return false;    }

     @Override
    public void close() {

    }
}

