/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authorization.policy.provider.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserPolicyProviderFactory implements PolicyProviderFactory<UserPolicyRepresentation> {

    private UserPolicyProvider provider = new UserPolicyProvider((Function<Policy, UserPolicyRepresentation>) policy -> toRepresentation(policy, new UserPolicyRepresentation()));

    @Override
    public String getName() {
        return "User";
    }

    @Override
    public String getGroup() {
        return "Identity Based";
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return provider;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public UserPolicyRepresentation toRepresentation(Policy policy, UserPolicyRepresentation representation) {
        try {
            representation.setUsers(JsonSerialization.readValue(policy.getConfig().get("users"), Set.class));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize roles", cause);
        }
        return representation;
    }

    @Override
    public Class<UserPolicyRepresentation> getRepresentationType() {
        return UserPolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, UserPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateUsers(policy, representation, authorization);
    }

    @Override
    public void onUpdate(Policy policy, UserPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateUsers(policy, representation, authorization);
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        try {
            updateUsers(policy, authorization, JsonSerialization.readValue(representation.getConfig().get("users"), Set.class));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize users during import", cause);
        }
    }

    @Override
    public void onExport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorizationProvider) {
        UserPolicyRepresentation userRep = toRepresentation(policy, new UserPolicyRepresentation());
        Map<String, String> config = new HashMap<>();

        try {
            UserProvider userProvider = authorizationProvider.getKeycloakSession().users();
            RealmModel realm = authorizationProvider.getRealm();

            config.put("users", JsonSerialization.writeValueAsString(userRep.getUsers().stream().map(id -> userProvider.getUserById(id, realm).getUsername()).collect(Collectors.toList())));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to export user policy [" + policy.getName() + "]", cause);
        }

        representation.setConfig(config);
    }

    private void updateUsers(Policy policy, UserPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateUsers(policy, authorization, representation.getUsers());
    }

    private void updateUsers(Policy policy, AuthorizationProvider authorization, Set<String> users) {
        KeycloakSession session = authorization.getKeycloakSession();
        RealmModel realm = authorization.getRealm();
        UserProvider userProvider = session.users();
        Set<String> updatedUsers = new HashSet<>();

        if (users != null) {
            for (String userId : users) {
                UserModel user = null;

                try {
                    user = userProvider.getUserByUsername(userId, realm);
                } catch (Exception ignore) {
                }

                if (user == null) {
                    user = userProvider.getUserById(userId, realm);
                }

                if (user == null) {
                    throw new RuntimeException("Error while updating policy [" + policy.getName()  + "]. User [" + userId + "] could not be found.");
                }

                updatedUsers.add(user.getId());
            }
        }

        try {

            policy.putConfig("users", JsonSerialization.writeValueAsString(updatedUsers));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to serialize users", cause);
        }
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof UserRemovedEvent) {
                KeycloakSession keycloakSession = ((UserRemovedEvent) event).getKeycloakSession();
                AuthorizationProvider provider = keycloakSession.getProvider(AuthorizationProvider.class);
                StoreFactory storeFactory = provider.getStoreFactory();
                PolicyStore policyStore = storeFactory.getPolicyStore();
                UserModel removedUser = ((UserRemovedEvent) event).getUser();
                RealmModel realm = ((UserRemovedEvent) event).getRealm();
                ResourceServerStore resourceServerStore = storeFactory.getResourceServerStore();
                realm.getClients().forEach(clientModel -> {
                    ResourceServer resourceServer = resourceServerStore.findByClient(clientModel.getId());

                    if (resourceServer != null) {
                        policyStore.findByType(getId(), resourceServer.getId()).forEach(policy -> {
                            List<String> users = new ArrayList<>();

                            for (String userId : getUsers(policy)) {
                                if (!userId.equals(removedUser.getId())) {
                                    users.add(userId);
                                }
                            }

                            try {
                                if (users.isEmpty()) {
                                    policyStore.delete(policy.getId());
                                } else {
                                    policy.putConfig("users", JsonSerialization.writeValueAsString(users));
                                }
                            } catch (IOException e) {
                                throw new RuntimeException("Error while synchronizing users with policy [" + policy.getName() + "].", e);
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "user";
    }

    static String[] getUsers(Policy policy) {
        String users = policy.getConfig().get("users");

        if (users != null) {
            try {
                return JsonSerialization.readValue(users.getBytes(), String[].class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse users [" + users + "] from policy config [" + policy.getName() + ".", e);
            }
        }

        return new String[0];
    }
}
