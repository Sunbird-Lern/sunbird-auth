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

package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.services.ServicesLogger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCAttributeMapperHelper {

    public static final String TOKEN_CLAIM_NAME = "claim.name";
    public static final String TOKEN_CLAIM_NAME_LABEL = "tokenClaimName.label";
    public static final String TOKEN_CLAIM_NAME_TOOLTIP = "tokenClaimName.tooltip";
    public static final String JSON_TYPE = "jsonType.label";
    public static final String JSON_TYPE_TOOLTIP = "jsonType.tooltip";
    public static final String INCLUDE_IN_ACCESS_TOKEN = "access.token.claim";
    public static final String INCLUDE_IN_ACCESS_TOKEN_LABEL = "includeInAccessToken.label";
    public static final String INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT = "includeInAccessToken.tooltip";
    public static final String INCLUDE_IN_ID_TOKEN = "id.token.claim";
    public static final String INCLUDE_IN_ID_TOKEN_LABEL = "includeInIdToken.label";
    public static final String INCLUDE_IN_ID_TOKEN_HELP_TEXT = "includeInIdToken.tooltip";

    public static final String INCLUDE_IN_USERINFO = "userinfo.token.claim";
    public static final String INCLUDE_IN_USERINFO_LABEL = "includeInUserInfo.label";
    public static final String INCLUDE_IN_USERINFO_HELP_TEXT = "includeInUserInfo.tooltip";

    public static Object mapAttributeValue(ProtocolMapperModel mappingModel, Object attributeValue) {
        if (attributeValue == null) return null;

        if (attributeValue instanceof List) {
            List<Object> valueAsList = (List<Object>) attributeValue;
            if (valueAsList.isEmpty()) return null;

            if (isMultivalued(mappingModel)) {
                List<Object> result = new ArrayList<>();
                for (Object valueItem : valueAsList) {
                    result.add(mapAttributeValue(mappingModel, valueItem));
                }
                return result;
            } else {
                if (valueAsList.size() > 1) {
                    ServicesLogger.LOGGER.multipleValuesForMapper(attributeValue.toString(), mappingModel.getName());
                }

                attributeValue = valueAsList.get(0);
            }
        }

        String type = mappingModel.getConfig().get(JSON_TYPE);
        Object converted = convertToType(type, attributeValue);
        return converted != null ? converted : attributeValue;
    }

    private static <X, T> List<T> transform(List<X> attributeValue, Function<X, T> mapper) {
        return attributeValue.stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .collect(Collectors.toList());
    }

    private static Object convertToType(String type, Object attributeValue) {
        if (type == null) return attributeValue;
        switch (type) {
            case "boolean":
                Boolean booleanObject = getBoolean(attributeValue);
                if (booleanObject != null) return booleanObject;
                if (attributeValue instanceof List) {
                    return transform((List<Boolean>) attributeValue, OIDCAttributeMapperHelper::getBoolean);
                }
                throw new RuntimeException("cannot map type for token claim");
            case "String":
                if (attributeValue instanceof String) return attributeValue;
                if (attributeValue instanceof List) {
                    return transform((List<String>) attributeValue, OIDCAttributeMapperHelper::getString);
                }
                return attributeValue.toString();
            case "long":
                Long longObject = getLong(attributeValue);
                if (longObject != null) return longObject;
                if (attributeValue instanceof List) {
                    return transform((List<Long>) attributeValue, OIDCAttributeMapperHelper::getLong);
                }
                throw new RuntimeException("cannot map type for token claim");
            case "int":
                Integer intObject = getInteger(attributeValue);
                if (intObject != null) return intObject;
                if (attributeValue instanceof List) {
                    return transform((List<Integer>) attributeValue, OIDCAttributeMapperHelper::getInteger);
                }
                throw new RuntimeException("cannot map type for token claim");
            default:
                return null;
        }
    }

    private static String getString(Object attributeValue) {
        return attributeValue.toString();
    }


    private static Long getLong(Object attributeValue) {
        if (attributeValue instanceof Long) return (Long) attributeValue;
        if (attributeValue instanceof String) return Long.valueOf((String) attributeValue);
        return null;
    }

    private static Integer getInteger(Object attributeValue) {
        if (attributeValue instanceof Integer) return (Integer) attributeValue;
        if (attributeValue instanceof String) return Integer.valueOf((String) attributeValue);
        return null;
    }

    private static Boolean getBoolean(Object attributeValue) {
        if (attributeValue instanceof Boolean) return (Boolean) attributeValue;
        if (attributeValue instanceof String) return Boolean.valueOf((String) attributeValue);
        return null;
    }

    public static void mapClaim(IDToken token, ProtocolMapperModel mappingModel, Object attributeValue) {
        attributeValue = mapAttributeValue(mappingModel, attributeValue);
        if (attributeValue == null) return;

        String protocolClaim = mappingModel.getConfig().get(TOKEN_CLAIM_NAME);
        if (protocolClaim == null) {
            return;
        }
        String[] split = protocolClaim.split("\\.");
        Map<String, Object> jsonObject = token.getOtherClaims();
        for (int i = 0; i < split.length; i++) {
            if (i == split.length - 1) {
                jsonObject.put(split[i], attributeValue);
            } else {
                Map<String, Object> nested = (Map<String, Object>)jsonObject.get(split[i]);

                if (nested == null) {
                    nested = new HashMap<String, Object>();
                    jsonObject.put(split[i], nested);
                }

                jsonObject = nested;
            }
        }
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String userAttribute,
                                                        String tokenClaimName, String claimType,
                                                        boolean consentRequired, String consentText,
                                                        boolean accessToken, boolean idToken,
                                                        String mapperId) {
        return createClaimMapper(name, userAttribute,tokenClaimName, claimType, consentRequired, consentText, accessToken, idToken, true, mapperId);
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                  String userAttribute,
                                  String tokenClaimName, String claimType,
                                  boolean consentRequired, String consentText,
                                  boolean accessToken, boolean idToken, boolean userinfo,
                                  String mapperId) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setConsentRequired(consentRequired);
        mapper.setConsentText(consentText);
        Map<String, String> config = new HashMap<String, String>();
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, userAttribute);
        config.put(TOKEN_CLAIM_NAME, tokenClaimName);
        config.put(JSON_TYPE, claimType);
        if (accessToken) config.put(INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(INCLUDE_IN_ID_TOKEN, "true");
        if (userinfo) config.put(INCLUDE_IN_USERINFO, "true");
        mapper.setConfig(config);
        return mapper;
    }

    public static boolean includeInIDToken(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(INCLUDE_IN_ID_TOKEN));
    }

    public static boolean includeInAccessToken(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(INCLUDE_IN_ACCESS_TOKEN));
    }

    public static boolean isMultivalued(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(ProtocolMapperUtils.MULTIVALUED));
    }

    public static boolean includeInUserInfo(ProtocolMapperModel mappingModel){
        String includeInUserInfo = mappingModel.getConfig().get(INCLUDE_IN_USERINFO);

        // Backwards compatibility
        if (includeInUserInfo == null && includeInIDToken(mappingModel)) {
            return true;
        }

        return "true".equals(includeInUserInfo);
    }

    public static void addAttributeConfig(List<ProviderConfigProperty> configProperties, Class<? extends ProtocolMapper> protocolMapperClass) {
        addTokenClaimNameConfig(configProperties);
        addJsonTypeConfig(configProperties);

        addIncludeInTokensConfig(configProperties, protocolMapperClass);
    }

    public static void addTokenClaimNameConfig(List<ProviderConfigProperty> configProperties) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(TOKEN_CLAIM_NAME);
        property.setLabel(TOKEN_CLAIM_NAME_LABEL);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText(TOKEN_CLAIM_NAME_TOOLTIP);
        configProperties.add(property);
    }

    public static void addJsonTypeConfig(List<ProviderConfigProperty> configProperties) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(JSON_TYPE);
        property.setLabel(JSON_TYPE);
        List<String> types = new ArrayList(3);
        types.add("String");
        types.add("long");
        types.add("int");
        types.add("boolean");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(types);
        property.setHelpText(JSON_TYPE_TOOLTIP);
        configProperties.add(property);
    }

    public static void addIncludeInTokensConfig(List<ProviderConfigProperty> configProperties, Class<? extends ProtocolMapper> protocolMapperClass) {
        if (OIDCIDTokenMapper.class.isAssignableFrom(protocolMapperClass)) {
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName(INCLUDE_IN_ID_TOKEN);
            property.setLabel(INCLUDE_IN_ID_TOKEN_LABEL);
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue("true");
            property.setHelpText(INCLUDE_IN_ID_TOKEN_HELP_TEXT);
            configProperties.add(property);
        }

        if (OIDCAccessTokenMapper.class.isAssignableFrom(protocolMapperClass)) {
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName(INCLUDE_IN_ACCESS_TOKEN);
            property.setLabel(INCLUDE_IN_ACCESS_TOKEN_LABEL);
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue("true");
            property.setHelpText(INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT);
            configProperties.add(property);
        }

        if (UserInfoTokenMapper.class.isAssignableFrom(protocolMapperClass)) {
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName(INCLUDE_IN_USERINFO);
            property.setLabel(INCLUDE_IN_USERINFO_LABEL);
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue("true");
            property.setHelpText(INCLUDE_IN_USERINFO_HELP_TEXT);
            configProperties.add(property);
        }
    }
}
