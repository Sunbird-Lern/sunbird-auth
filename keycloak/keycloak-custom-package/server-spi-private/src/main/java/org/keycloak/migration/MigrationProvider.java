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

package org.keycloak.migration;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.util.List;

/**
 * Various common utils needed for migration from older version to newer
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface MigrationProvider extends Provider {

    /**
     * @param claimMask mask used on ClientModel in 1.1.0
     * @return set of 1.2.0.Beta1 protocol mappers corresponding to given claimMask
     */
    List<ProtocolMapperRepresentation> getMappersForClaimMask(Long claimMask);

    List<ProtocolMapperModel> getBuiltinMappers(String protocol);

    void setupAdminCli(RealmModel realm);

}
