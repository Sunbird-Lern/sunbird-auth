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

package org.keycloak.testsuite.page;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;

/**
 *
 * @author tkyjovsk
 */
public class Form {

    protected final Logger log = Logger.getLogger(this.getClass());
    
    @Drone
    protected WebDriver driver;

    public static final String ACTIVE_DIV_XPATH = ".//div[not(contains(@class,'ng-hide'))]";

    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[@kc-save or @data-kc-save]")
    private WebElement save;
    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[@kc-reset or @data-kc-reset]")
    private WebElement cancel;

    public void save() {
//        guardAjax(save).click();
        save.click();
        WaitUtils.waitForPageToLoad(driver);
    }

    public void cancel() {
        guardAjax(cancel).click();
    }

    public static String getInputValue(WebElement input) {
        return input.getAttribute(VALUE);
    }

    public static final String VALUE = "value";

    public static void setInputValue(WebElement input, String value) {
        if (input.isEnabled()) {
            input.clear();
            if (value != null) {
                input.sendKeys(value);
            }
        } else {
            // TODO log warning
        }
    }

}
