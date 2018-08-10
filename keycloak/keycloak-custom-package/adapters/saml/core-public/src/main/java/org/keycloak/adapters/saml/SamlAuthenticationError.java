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

package org.keycloak.adapters.saml;

import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;

/**
 * Object that describes the SAML error that happened.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlAuthenticationError implements AuthenticationError {
    public static enum Reason {
        EXTRACTION_FAILURE,
        INVALID_SIGNATURE,
        ERROR_STATUS
    }

    private Reason reason;

    private StatusResponseType status;

    public SamlAuthenticationError(Reason reason) {
        this.reason = reason;
    }

    public SamlAuthenticationError(Reason reason, StatusResponseType status) {
        this.reason = reason;
        this.status = status;
    }

    public SamlAuthenticationError(StatusResponseType statusType) {
        this.status = statusType;
    }

    public Reason getReason() {
        return reason;
    }
    public StatusResponseType getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "SamlAuthenticationError [reason=" + reason + ", status=" + status + "]";
    }
    
}
