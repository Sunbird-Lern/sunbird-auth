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

package org.keycloak.testsuite.console.page.events;

import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class LoginEvents extends Events {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/events";
    }

    @FindBy(tagName = "table")
    private LoginEventsTable table;

    public LoginEventsTable table() {
        return table;
    }

    public class LoginEventsTable extends DataTable {

        @FindBy(xpath = "//button[text()[contains(.,'Filter')]]")
        private WebElement filterButton;

        @FindBy(tagName = "form")
        private LoginEventsTableFilterForm filterForm;

        public void update() {
            clickHeaderButton("Update");
        }

        public void reset() {
            clickHeaderButton("Reset");
        }

        public void filter() {
            filterButton.click();
        }

        public LoginEventsTableFilterForm filterForm() {
            return filterForm;
        }

        public class LoginEventsTableFilterForm extends Form {

            @FindBy(id = "client")
            private WebElement clientInput;

            @FindBy(id = "user")
            private WebElement userInput;

            @FindBy(xpath = "//div[@id='s2id_eventTypes']/ul")
            private WebElement eventTypeInput;

            @FindBy(xpath = "//div[@id='select2-drop']")
            private WebElement eventTypeValues;

            public void addEventType(String type) {
                eventTypeInput.click();
                eventTypeValues.findElement(By.xpath("//div[text()='" + type + "']")).click();
            }

            public void removeOperationType(String type) {
                eventTypeInput.findElement(By.xpath("//div[text()='" + type + "']/../a]")).click();
            }

            public void setClientInput(String value) {
                setInputValue(clientInput, value);
            }

            public void setUserInput(String value) {
                setInputValue(userInput, value);
            }
        }
    }
}
