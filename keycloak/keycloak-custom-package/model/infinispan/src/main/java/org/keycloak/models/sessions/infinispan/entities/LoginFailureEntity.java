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

package org.keycloak.models.sessions.infinispan.entities;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginFailureEntity implements Serializable {

    private String userId;
    private String realm;
    private int failedLoginNotBefore;
    private int numFailures;
    private long lastFailure;
    private String lastIPFailure;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public int getFailedLoginNotBefore() {
        return failedLoginNotBefore;
    }

    public void setFailedLoginNotBefore(int failedLoginNotBefore) {
        this.failedLoginNotBefore = failedLoginNotBefore;
    }

    public int getNumFailures() {
        return numFailures;
    }

    public void setNumFailures(int numFailures) {
        this.numFailures = numFailures;
    }

    public long getLastFailure() {
        return lastFailure;
    }

    public void setLastFailure(long lastFailure) {
        this.lastFailure = lastFailure;
    }

    public String getLastIPFailure() {
        return lastIPFailure;
    }

    public void setLastIPFailure(String lastIPFailure) {
        this.lastIPFailure = lastIPFailure;
    }

    public void clearFailures() {
        this.failedLoginNotBefore = 0;
        this.numFailures = 0;
        this.lastFailure = 0;
        this.lastIPFailure = null;
    }
}
