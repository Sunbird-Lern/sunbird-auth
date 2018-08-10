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

package org.keycloak.testsuite.rule;

import org.jboss.logging.Logger;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.keycloak.testsuite.federation.ldap.LDAPTestConfiguration;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import java.util.Map;
import java.util.Properties;

/**
 * This rule handles:
 * - Reading of LDAP configuration from properties file
 * - Eventually start+stop of LDAP embedded server.
 * - Eventually allows to ignore the test if particular condition is not met. This allows to run specific tests just for some LDAP vendors
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPRule implements TestRule {

    private static final Logger logger = Logger.getLogger(LDAPRule.class);

    public static final String LDAP_CONNECTION_PROPERTIES_LOCATION = "ldap/ldap-connection.properties";

    protected LDAPTestConfiguration ldapTestConfiguration;
    protected LDAPEmbeddedServer ldapEmbeddedServer;

    private final LDAPRuleCondition condition;


    public LDAPRule() {
        this(null);
    }

    public LDAPRule(LDAPRuleCondition condition) {
        this.condition = condition;
    }


    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                boolean skipTest = before();

                if (skipTest) {
                    logger.infof("Skip %s due to LDAPRuleCondition not met", description.getDisplayName());
                    return;
                }

                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }


    // Return true if test should be skipped
    protected boolean before() throws Throwable {
        String connectionPropsLocation = getConnectionPropertiesLocation();
        ldapTestConfiguration = LDAPTestConfiguration.readConfiguration(connectionPropsLocation);

        if (condition != null && condition.skipTest(ldapTestConfiguration.getLDAPConfig())) {
            return true;
        }

        if (ldapTestConfiguration.isStartEmbeddedLdapServer()) {
            ldapEmbeddedServer = createServer();
            ldapEmbeddedServer.init();
            ldapEmbeddedServer.start();
        }

        return false;
    }


    protected void after() {
        try {
            if (ldapEmbeddedServer != null) {
                ldapEmbeddedServer.stop();
                ldapEmbeddedServer = null;
                ldapTestConfiguration = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error tearDown Embedded LDAP server.", e);
        }
    }

    protected String getConnectionPropertiesLocation() {
        return LDAP_CONNECTION_PROPERTIES_LOCATION;
    }

    protected LDAPEmbeddedServer createServer() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_LDIF_FILE, "classpath:ldap/users.ldif");

        return new LDAPEmbeddedServer(defaultProperties);
    }

    public Map<String, String> getConfig() {
        return ldapTestConfiguration.getLDAPConfig();
    }

    public int getSleepTime() {
        return ldapTestConfiguration.getSleepTime();
    }


    // Allows to skip particular LDAP test just under specific conditions (eg. some test running just on Active Directory)
    public interface LDAPRuleCondition {

        boolean skipTest(Map<String, String> ldapConfig);

    }
}
