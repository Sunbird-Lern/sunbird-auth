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

package org.keycloak.sessions;

import java.util.Map;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;

/**
 * Predecesor of AuthenticationSessionModel, ClientLoginSessionModel and ClientSessionModel (then action tickets). Maybe we will remove it later...
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface CommonClientSessionModel {

    public String getRedirectUri();
    public void setRedirectUri(String uri);

    public String getId();
    public RealmModel getRealm();
    public ClientModel getClient();

    public int getTimestamp();
    public void setTimestamp(int timestamp);

    public String getAction();
    public void setAction(String action);

    public String getProtocol();
    public void setProtocol(String method);

    // TODO: Not needed here...?
    public Set<String> getRoles();
    public void setRoles(Set<String> roles);

    // TODO: Not needed here...?
    public Set<String> getProtocolMappers();
    public void setProtocolMappers(Set<String> protocolMappers);
    
    public static enum Action {
        OAUTH_GRANT,
        CODE_TO_TOKEN,
        AUTHENTICATE,
        LOGGED_OUT,
        REQUIRED_ACTIONS
    }

    public enum ExecutionStatus {
        FAILED,
        SUCCESS,
        SETUP_REQUIRED,
        ATTEMPTED,
        SKIPPED,
        CHALLENGED
    }
}
