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

package org.keycloak.testsuite.arquillian;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.container.test.impl.client.deployment.AnnotationDeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;

import java.util.List;

import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.getAppServerQualifier;

/**
 * Changes target container for all Arquillian deployments based on value of
 * @AppServerContainer.
 *
 * @author tkyjovsk
 */
public class DeploymentTargetModifier extends AnnotationDeploymentScenarioGenerator {

    // Will be replaced in runtime by real auth-server-container
    public static final String AUTH_SERVER_CURRENT = "auth-server-current";

    protected final Logger log = Logger.getLogger(this.getClass());

    @Override
    public List<DeploymentDescription> generate(TestClass testClass) {
        List<DeploymentDescription> deployments = super.generate(testClass);

        checkAuthServerTestDeployment(deployments, testClass);

        String appServerQualifier = getAppServerQualifier(
                testClass.getJavaClass());

        if (appServerQualifier != null && !appServerQualifier.isEmpty()) {
            for (DeploymentDescription deployment : deployments) {
                if (deployment.getTarget() == null || !deployment.getTarget().getName().startsWith(appServerQualifier)) {
                    log.debug("Setting target container for " + deployment.getName() + ": " + appServerQualifier);
                    deployment.setTarget(new TargetDescription(appServerQualifier));
                }
            }
        }

        return deployments;
    }

    private void checkAuthServerTestDeployment(List<DeploymentDescription> descriptions, TestClass testClass) {
        for (DeploymentDescription deployment : descriptions) {
            if (deployment.getTarget() != null) {
                String containerQualifier = deployment.getTarget().getName();
                if (AUTH_SERVER_CURRENT.equals(containerQualifier)) {
                    String authServerQualifier = AuthServerTestEnricher.AUTH_SERVER_CONTAINER;
                    log.infof("Setting target container for deployment %s.%s: %s", testClass.getName(), deployment.getName(), authServerQualifier);
                    deployment.setTarget(new TargetDescription(authServerQualifier));
                }
            }
        }
    }

}
