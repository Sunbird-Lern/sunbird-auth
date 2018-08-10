package org.keycloak.authorization.policy.provider.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmModel.ClientRemovedEvent;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.util.JsonSerialization;

public class ClientPolicyProviderFactory implements PolicyProviderFactory<ClientPolicyRepresentation> {

    private ClientPolicyProvider provider = new ClientPolicyProvider(policy -> toRepresentation(policy, new ClientPolicyRepresentation()));

    @Override
    public String getName() {
        return "Client";
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
    public ClientPolicyRepresentation toRepresentation(Policy policy, ClientPolicyRepresentation representation) {
        representation.setClients(new HashSet<>(Arrays.asList(getClients(policy))));
        return representation;
    }

    @Override
    public Class<ClientPolicyRepresentation> getRepresentationType() {
        return ClientPolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, ClientPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateClients(policy, representation.getClients(), authorization);
    }

    @Override
    public void onUpdate(Policy policy, ClientPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateClients(policy, representation.getClients(), authorization);
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        updateClients(policy, new HashSet<>(Arrays.asList(getClients(policy))), authorization);
    }

    @Override
    public void onExport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorizationProvider) {
        ClientPolicyRepresentation userRep = toRepresentation(policy, new ClientPolicyRepresentation());
        Map<String, String> config = new HashMap<>();

        try {
            RealmModel realm = authorizationProvider.getRealm();
            config.put("clients", JsonSerialization.writeValueAsString(userRep.getClients().stream().map(id -> realm.getClientById(id).getClientId()).collect(Collectors.toList())));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to export user policy [" + policy.getName() + "]", cause);
        }

        representation.setConfig(config);
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof ClientRemovedEvent) {
                KeycloakSession keycloakSession = ((ClientRemovedEvent) event).getKeycloakSession();
                AuthorizationProvider provider = keycloakSession.getProvider(AuthorizationProvider.class);
                StoreFactory storeFactory = provider.getStoreFactory();
                PolicyStore policyStore = storeFactory.getPolicyStore();
                ClientModel removedClient = ((ClientRemovedEvent) event).getClient();
                ResourceServerStore resourceServerStore = storeFactory.getResourceServerStore();
                ResourceServer resourceServer = resourceServerStore.findByClient(removedClient.getId());

                if (resourceServer != null) {
                    policyStore.findByType(getId(), resourceServer.getId()).forEach(policy -> {
                        List<String> clients = new ArrayList<>();

                        for (String clientId : getClients(policy)) {
                            if (!clientId.equals(removedClient.getId())) {
                                clients.add(clientId);
                            }
                        }

                        try {
                            if (clients.isEmpty()) {
                                policyStore.delete(policy.getId());
                            } else {
                                policy.putConfig("clients", JsonSerialization.writeValueAsString(clients));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Error while synchronizing clients with policy [" + policy.getName() + "].", e);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "client";
    }

    private void updateClients(Policy policy, Set<String> clients, AuthorizationProvider authorization) {
        RealmModel realm = authorization.getRealm();

        if (clients == null || clients.isEmpty()) {
            throw new RuntimeException("No client provided.");
        }

        Set<String> updatedClients = new HashSet<>();

        for (String id : clients) {
            ClientModel client = realm.getClientByClientId(id);

            if (client == null) {
                client = realm.getClientById(id);
            }

            if (client == null) {
                throw new RuntimeException("Error while updating policy [" + policy.getName()  + "]. Client [" + id + "] could not be found.");
            }

            updatedClients.add(client.getId());
        }

        try {
            policy.putConfig("clients", JsonSerialization.writeValueAsString(updatedClients));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to serialize clients", cause);
        }
    }

    private String[] getClients(Policy policy) {
        String clients = policy.getConfig().get("clients");

        if (clients != null) {
            try {
                return JsonSerialization.readValue(clients.getBytes(), String[].class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse clients [" + clients + "] from policy config [" + policy.getName() + "].", e);
            }
        }

        return new String[]{};
    }
}
