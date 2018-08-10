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
package org.keycloak.testsuite.util;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class TestEventsLogger extends RunListener {

    
    private static WebDriver driver;
    
    public static void setDriver(WebDriver driver) {
        TestEventsLogger.driver = driver;
    }
    
    private Logger log(Description d) {
        return Logger.getLogger(d.getClassName());
    }

    private String getMessage(Description d, String status) {
        return String.format("[%s] %s() %s", d.getTestClass().getSimpleName(), d.getMethodName(), status);
    }

    @Override
    public void testStarted(Description d) throws Exception {
        log(d).info(getMessage(d, "STARTED"));
    }

    @Override
    public void testFailure(Failure f) throws Exception {
        Description d = f.getDescription();
        createPageSrcFile(d);
        log(d).error(getMessage(d, "FAILED"));
    }

    @Override
    public void testIgnored(Description d) throws Exception {
        log(d).warn(getMessage(d, "IGNORED\n\n"));
    }

    @Override
    public void testFinished(Description d) throws Exception {
        log(d).info(getMessage(d, "FINISHED\n\n"));
    }

    private void createPageSrcFile(Description d) throws IOException {
        try {
            if (driver != null && driver.getPageSource() != null) {
                String pageSourceLocation = System.getProperty("page.source.location", "target/failed-tests/page-source/");
                FileUtils.writeStringToFile(new File(pageSourceLocation + d.getTestClass().getSimpleName() + "/" + d.getMethodName() + ".html"), 
                        driver.getPageSource());
            }
        } catch (IllegalStateException ex) {
            Logger.getLogger(TestEventsLogger.class).warn(ex.getMessage());
        }
    }
}
