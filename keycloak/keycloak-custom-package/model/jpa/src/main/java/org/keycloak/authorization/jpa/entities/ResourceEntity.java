/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.jpa.entities;

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Entity
@Table(name = "RESOURCE_SERVER_RESOURCE", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID", "OWNER"})
})
@NamedQueries(
        {
                @NamedQuery(name="findResourceIdByOwner", query="select r.id from ResourceEntity r where r.resourceServer.id = :serverId and r.owner = :owner"),
                @NamedQuery(name="findResourceIdByUri", query="select r.id from ResourceEntity r where  r.resourceServer.id = :serverId  and r.uri = :uri"),
                @NamedQuery(name="findResourceIdByName", query="select r.id from ResourceEntity r where  r.resourceServer.id = :serverId  and r.name = :name"),
                @NamedQuery(name="findResourceIdByType", query="select r.id from ResourceEntity r where  r.resourceServer.id = :serverId  and r.type = :type"),
                @NamedQuery(name="findResourceIdByServerId", query="select r.id from ResourceEntity r where  r.resourceServer.id = :serverId "),
                @NamedQuery(name="findResourceIdByScope", query="select r.id from ResourceEntity r inner join r.scopes s where r.resourceServer.id = :serverId and (s.resourceServer.id = :serverId and s.id in (:scopeIds))"),
                @NamedQuery(name="deleteResourceByResourceServer", query="delete from ResourceEntity r where r.resourceServer.id = :serverId")
        }
)
public class ResourceEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "URI")
    private String uri;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "ICON_URI")
    private String iconUri;

    @Column(name = "OWNER")
    private String owner;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "RESOURCE_SERVER_ID")
    private ResourceServerEntity resourceServer;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "RESOURCE_SCOPE", joinColumns = @JoinColumn(name = "RESOURCE_ID"), inverseJoinColumns = @JoinColumn(name = "SCOPE_ID"))
    private List<ScopeEntity> scopes = new LinkedList<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "RESOURCE_POLICY", joinColumns = @JoinColumn(name = "RESOURCE_ID"), inverseJoinColumns = @JoinColumn(name = "POLICY_ID"))
    private List<PolicyEntity> policies = new LinkedList<>();

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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ScopeEntity> getScopes() {
        return this.scopes;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public ResourceServerEntity getResourceServer() {
        return resourceServer;
    }

    public void setResourceServer(ResourceServerEntity resourceServer) {
        this.resourceServer = resourceServer;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<PolicyEntity> getPolicies() {
        return this.policies;
    }


    public void setPolicies(List<PolicyEntity> policies) {
        this.policies = policies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceEntity that = (ResourceEntity) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
