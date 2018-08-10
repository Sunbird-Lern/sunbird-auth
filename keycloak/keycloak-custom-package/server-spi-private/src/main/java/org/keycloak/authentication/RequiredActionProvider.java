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

package org.keycloak.authentication;

import org.keycloak.provider.Provider;

/**
 * RequiredAction provider.  Required actions are one-time actions that a user must perform before they are logged in.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RequiredActionProvider extends Provider {
    /**
     * Called every time a uesr authenticates.  This checks to see if this required action should be triggered.
     * The implementation of this method is responsible for setting the required action on the UserModel.
     *
     * For example, the UpdatePassword required actions checks the password policies to see if the password has expired.
     *
     * @param context
     */
    void evaluateTriggers(RequiredActionContext context);

    /**
     * If the user has a required action set, this method will be the initial call to obtain what to display to the
     * user's browser.  Return null if no action should be done.
     *
     * @param context
     * @return
     */
    void requiredActionChallenge(RequiredActionContext context);

    /**
     * Called when a required action has form input you want to process.
     *
     * @param context
     */
    void processAction(RequiredActionContext context);
}
