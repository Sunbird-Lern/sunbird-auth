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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Entity
@Table(name="USER_CONSENT", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "CLIENT_ID"})
})
@NamedQueries({
        @NamedQuery(name="userConsentByUserAndClient", query="select consent from UserConsentEntity consent where consent.user.id = :userId and consent.clientId = :clientId"),
        @NamedQuery(name="userConsentsByUser", query="select consent from UserConsentEntity consent where consent.user.id = :userId"),
        @NamedQuery(name="deleteUserConsentsByRealm", query="delete from UserConsentEntity consent where consent.user IN (select user from UserEntity user where user.realmId = :realmId)"),
        @NamedQuery(name="deleteUserConsentsByRealmAndLink", query="delete from UserConsentEntity consent where consent.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name="deleteUserConsentsByUser", query="delete from UserConsentEntity consent where consent.user = :user"),
        @NamedQuery(name="deleteUserConsentsByClient", query="delete from UserConsentEntity consent where consent.clientId = :clientId"),
})
public class UserConsentEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    @Column(name="CLIENT_ID")
    protected String clientId;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "userConsent")
    Collection<UserConsentRoleEntity> grantedRoles = new ArrayList<UserConsentRoleEntity>();

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "userConsent")
    Collection<UserConsentProtocolMapperEntity> grantedProtocolMappers = new ArrayList<UserConsentProtocolMapperEntity>();

    @Column(name = "CREATED_DATE")
    private Long createdDate;

    @Column(name = "LAST_UPDATED_DATE")
    private Long lastUpdatedDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Collection<UserConsentRoleEntity> getGrantedRoles() {
        return grantedRoles;
    }

    public void setGrantedRoles(Collection<UserConsentRoleEntity> grantedRoles) {
        this.grantedRoles = grantedRoles;
    }

    public Collection<UserConsentProtocolMapperEntity> getGrantedProtocolMappers() {
        return grantedProtocolMappers;
    }

    public void setGrantedProtocolMappers(Collection<UserConsentProtocolMapperEntity> grantedProtocolMappers) {
        this.grantedProtocolMappers = grantedProtocolMappers;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof UserConsentEntity)) return false;

        UserConsentEntity that = (UserConsentEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
