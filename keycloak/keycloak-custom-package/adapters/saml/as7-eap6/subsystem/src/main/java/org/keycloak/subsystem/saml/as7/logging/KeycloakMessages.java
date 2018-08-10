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
package org.keycloak.subsystem.saml.as7.logging;

import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This interface to be fleshed out later when error messages are fully externalized.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
@MessageBundle(projectCode = "TLIP")
public interface KeycloakMessages {

    /**
     * The messages
     */
    KeycloakMessages MESSAGES = Messages.getBundle(KeycloakMessages.class);
}
