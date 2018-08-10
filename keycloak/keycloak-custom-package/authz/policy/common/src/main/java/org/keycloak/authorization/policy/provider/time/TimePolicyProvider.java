/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.policy.provider.time;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TimePolicyProvider implements PolicyProvider {

    static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd hh:mm:ss";

    private final SimpleDateFormat dateFormat;

    public TimePolicyProvider() {
        this.dateFormat = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();
        Date actualDate = new Date();

        try {
            String notBefore = policy.getConfig().get("nbf");
            if (notBefore != null && !"".equals(notBefore)) {
                if (actualDate.before(this.dateFormat.parse(format(notBefore)))) {
                    evaluation.deny();
                    return;
                }
            }

            String notOnOrAfter = policy.getConfig().get("noa");
            if (notOnOrAfter != null && !"".equals(notOnOrAfter)) {
                if (actualDate.after(this.dateFormat.parse(format(notOnOrAfter)))) {
                    evaluation.deny();
                    return;
                }
            }

            if (isInvalid(actualDate, Calendar.DAY_OF_MONTH, "dayMonth", policy)
                    || isInvalid(actualDate, Calendar.MONTH, "month", policy)
                    || isInvalid(actualDate, Calendar.YEAR, "year", policy)
                    || isInvalid(actualDate, Calendar.HOUR_OF_DAY, "hour", policy)
                    || isInvalid(actualDate, Calendar.MINUTE, "minute", policy)) {
                evaluation.deny();
                return;
            }

            evaluation.grant();
        } catch (Exception e) {
            throw new RuntimeException("Could not evaluate time-based policy [" + policy.getName() + "].", e);
        }
    }

    private boolean isInvalid(Date actualDate, int timeConstant, String configName, Policy policy) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(actualDate);

        int dateField = calendar.get(timeConstant);

        if (Calendar.MONTH == timeConstant) {
            dateField++;
        }

        String start = policy.getConfig().get(configName);
        if (start != null) {
            String end = policy.getConfig().get(configName + "End");
            if (end != null) {
                if (dateField < Integer.parseInt(start)  || dateField > Integer.parseInt(end)) {
                    return true;
                }
            } else {
                if (dateField != Integer.parseInt(start)) {
                    return true;
                }
            }
        }
        return false;
    }

    static String format(String notBefore) {
        String trimmed = notBefore.trim();

        if (trimmed.length() == 10) {
            notBefore = trimmed + " 00:00:00";
        }

        return notBefore;
    }

    @Override
    public void close() {

    }
}
