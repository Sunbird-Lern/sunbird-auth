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

package org.keycloak.models.jpa.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Table(name="CLIENT", uniqueConstraints = {@UniqueConstraint(columnNames = {"REALM_ID", "CLIENT_ID"})})
@NamedQueries({
        @NamedQuery(name="getClientsByRealm", query="select client from ClientEntity client where client.realm = :realm"),
        @NamedQuery(name="getClientById", query="select client from ClientEntity client where client.id = :id and client.realm.id = :realm"),
        @NamedQuery(name="getClientIdsByRealm", query="select client.id from ClientEntity client where client.realm.id = :realm"),
        @NamedQuery(name="findClientIdByClientId", query="select client.id from ClientEntity client where client.clientId = :clientId and client.realm.id = :realm"),
        @NamedQuery(name="findClientByClientId", query="select client from ClientEntity client where client.clientId = :clientId and client.realm.id = :realm"),
})
public class ClientEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "DESCRIPTION")
    private String description;
    @Column(name = "CLIENT_ID")
    private String clientId;
    @Column(name="ENABLED")
    private boolean enabled;
    @Column(name="SECRET")
    private String secret;
    @Column(name="REGISTRATION_TOKEN")
    private String registrationToken;
    @Column(name="CLIENT_AUTHENTICATOR_TYPE")
    private String clientAuthenticatorType;
    @Column(name="NOT_BEFORE")
    private int notBefore;
    @Column(name="PUBLIC_CLIENT")
    private boolean publicClient;
    @Column(name="PROTOCOL")
    private String protocol;
    @Column(name="FRONTCHANNEL_LOGOUT")
    private boolean frontchannelLogout;
    @Column(name="FULL_SCOPE_ALLOWED")
    private boolean fullScopeAllowed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_TEMPLATE_ID")
    protected ClientTemplateEntity clientTemplate;

    @Column(name="USE_TEMPLATE_CONFIG")
    private boolean useTemplateConfig;

    @Column(name="USE_TEMPLATE_SCOPE")
    private boolean useTemplateScope;

    @Column(name="USE_TEMPLATE_MAPPERS")
    private boolean useTemplateMappers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @ElementCollection
    @Column(name="VALUE")
    @CollectionTable(name = "WEB_ORIGINS", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    protected Set<String> webOrigins = new HashSet<String>();

    @ElementCollection
    @Column(name="VALUE")
    @CollectionTable(name = "REDIRECT_URIS", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    protected Set<String> redirectUris = new HashSet<String>();

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", length = 2048)
    @CollectionTable(name="CLIENT_ATTRIBUTES", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    protected Map<String, String> attributes = new HashMap<String, String>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "client", cascade = CascadeType.REMOVE)
    Collection<ClientIdentityProviderMappingEntity> identityProviders = new ArrayList<ClientIdentityProviderMappingEntity>();

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "client")
    Collection<ProtocolMapperEntity> protocolMappers = new ArrayList<ProtocolMapperEntity>();

    @Column(name="SURROGATE_AUTH_REQUIRED")
    private boolean surrogateAuthRequired;

    @Column(name="ROOT_URL")
    private String rootUrl;

    @Column(name="BASE_URL")
    private String baseUrl;

    @Column(name="MANAGEMENT_URL")
    private String managementUrl;

    @Column(name="BEARER_ONLY")
    private boolean bearerOnly;

    @Column(name="CONSENT_REQUIRED")
    private boolean consentRequired;

    @Column(name="STANDARD_FLOW_ENABLED")
    private boolean standardFlowEnabled;

    @Column(name="IMPLICIT_FLOW_ENABLED")
    private boolean implicitFlowEnabled;

    @Column(name="DIRECT_ACCESS_GRANTS_ENABLED")
    private boolean directAccessGrantsEnabled;

    @Column(name="SERVICE_ACCOUNTS_ENABLED")
    private boolean serviceAccountsEnabled;

    @Column(name="NODE_REREG_TIMEOUT")
    private int nodeReRegistrationTimeout;

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="CLIENT_DEFAULT_ROLES", joinColumns = { @JoinColumn(name="CLIENT_ID")}, inverseJoinColumns = { @JoinColumn(name="ROLE_ID")})
    Collection<RoleEntity> defaultRoles = new ArrayList<RoleEntity>();

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name="SCOPE_MAPPING", joinColumns = { @JoinColumn(name="CLIENT_ID")}, inverseJoinColumns = { @JoinColumn(name="ROLE_ID")})
    protected Set<RoleEntity> scopeMapping = new HashSet<>();

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE")
    @CollectionTable(name="CLIENT_NODE_REGISTRATIONS", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    Map<String, Integer> registeredNodes = new HashMap<String, Integer>();

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(Set<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getClientAuthenticatorType() {
        return clientAuthenticatorType;
    }

    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        this.clientAuthenticatorType = clientAuthenticatorType;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public void setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(boolean fullScopeAllowed) {
        this.fullScopeAllowed = fullScopeAllowed;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public void setFrontchannelLogout(boolean frontchannelLogout) {
        this.frontchannelLogout = frontchannelLogout;
    }

    public Collection<ClientIdentityProviderMappingEntity> getIdentityProviders() {
        return this.identityProviders;
    }

    public void setIdentityProviders(Collection<ClientIdentityProviderMappingEntity> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public Collection<ProtocolMapperEntity> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(Collection<ProtocolMapperEntity> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl = managementUrl;
    }

    public Collection<RoleEntity> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(Collection<RoleEntity> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public boolean isStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        this.standardFlowEnabled = standardFlowEnabled;
    }

    public boolean isImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        this.implicitFlowEnabled = implicitFlowEnabled;
    }

    public boolean isDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public void setNodeReRegistrationTimeout(int nodeReRegistrationTimeout) {
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
    }

    public Map<String, Integer> getRegisteredNodes() {
        return registeredNodes;
    }

    public void setRegisteredNodes(Map<String, Integer> registeredNodes) {
        this.registeredNodes = registeredNodes;
    }

    public ClientTemplateEntity getClientTemplate() {
        return clientTemplate;
    }

    public void setClientTemplate(ClientTemplateEntity clientTemplate) {
        this.clientTemplate = clientTemplate;
    }

    public boolean isUseTemplateConfig() {
        return useTemplateConfig;
    }

    public void setUseTemplateConfig(boolean useTemplateConfig) {
        this.useTemplateConfig = useTemplateConfig;
    }

    public boolean isUseTemplateScope() {
        return useTemplateScope;
    }

    public void setUseTemplateScope(boolean useTemplateScope) {
        this.useTemplateScope = useTemplateScope;
    }

    public boolean isUseTemplateMappers() {
        return useTemplateMappers;
    }

    public void setUseTemplateMappers(boolean useTemplateMappers) {
        this.useTemplateMappers = useTemplateMappers;
    }

    public Set<RoleEntity> getScopeMapping() {
        return scopeMapping;
    }

    public void setScopeMapping(Set<RoleEntity> scopeMapping) {
        this.scopeMapping = scopeMapping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof ClientEntity)) return false;

        ClientEntity that = (ClientEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
