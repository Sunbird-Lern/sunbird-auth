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

package org.keycloak.testsuite.arquillian.migration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.testsuite.util.OAuthClient;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrationContext {

    public static final Logger logger = Logger.getLogger(MigrationContext.class);


    public String loadOfflineToken() throws Exception {
        String file = getOfflineTokenLocation();
        logger.infof("Reading previously saved offline token from the file: %s", file);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            String offlineToken = StreamUtil.readString(fis);

            File f = new File(file);
            f.delete();
            logger.infof("Deleted file with offline token: %s", file);

            return offlineToken;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }


    // Do some actions on the old container
    public void runPreMigrationTask() throws Exception {
        String offlineToken = requestOfflineToken();
        saveOfflineToken(offlineToken);
    }

    private String requestOfflineToken() {
        logger.info("Requesting offline token on the old container");
        try {
            OAuthClient oauth = new OAuthClient();
            oauth.init(null, null);
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            oauth.realm("Migration");
            oauth.clientId("migration-test-client");
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("b2c07929-69e3-44c6-8d7f-76939000b3e4", "migration-test-user", "admin");
            return tokenResponse.getRefreshToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void saveOfflineToken(String offlineToken) throws Exception {
        String file = getOfflineTokenLocation();
        logger.infof("Saving offline token to file: %s", file);

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            writer.print(offlineToken);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }


    // Needs to save offline token inside "basedir". There are issues with saving into directory "target" as it's cleared among restarts and
    // using "mvn install" instead of "mvn clean install" doesn't work ATM. Improve if needed...
    private String getOfflineTokenLocation() {
        return System.getProperty("basedir") + "/offline-token.txt";
    }

}
