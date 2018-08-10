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

package org.keycloak.models;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentModel {

    private final ClientModel client;
    private Set<ProtocolMapperModel> protocolMappers = new HashSet<ProtocolMapperModel>();
    private Set<RoleModel> roles = new HashSet<RoleModel>();
    private Long createdDate;
    private Long lastUpdatedDate;

    public UserConsentModel(ClientModel client) {
        this.client = client;
    }

    public ClientModel getClient() {
        return client;
    }

    public void addGrantedRole(RoleModel role) {
        roles.add(role);
    }

    public Set<RoleModel> getGrantedRoles() {
        return roles;
    }

    public boolean isRoleGranted(RoleModel role) {
        for (RoleModel currentRole : roles) {
            if (currentRole.getId().equals(role.getId())) return true;
        }
        return false;
    }

    public void addGrantedProtocolMapper(ProtocolMapperModel protocolMapper) {
        protocolMappers.add(protocolMapper);
    }

    public Set<ProtocolMapperModel> getGrantedProtocolMappers() {
        return protocolMappers;
    }

    public boolean isProtocolMapperGranted(ProtocolMapperModel protocolMapper) {
        for (ProtocolMapperModel currentProtMapper : protocolMappers) {
            if (currentProtMapper.getId().equals(protocolMapper.getId())) return true;
        }
        return false;
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
}
