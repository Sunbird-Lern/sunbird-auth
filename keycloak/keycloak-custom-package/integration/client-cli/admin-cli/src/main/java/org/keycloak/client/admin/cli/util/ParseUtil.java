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
package org.keycloak.client.admin.cli.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.client.admin.cli.common.AttributeOperation;
import org.keycloak.client.admin.cli.common.CmdStdinContext;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;

import static org.keycloak.client.admin.cli.util.IoUtil.readFileOrStdin;
import static org.keycloak.client.admin.cli.util.ReflectionUtil.setAttributes;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ParseUtil {

    public static String[] parseKeyVal(String keyval) {
        // we expect = as a separator
        int pos = keyval.indexOf("=");
        if (pos <= 0) {
            throw new RuntimeException("Invalid key=value parameter: [" + keyval + "]");
        }

        String [] parsed = new String[2];
        parsed[0] = keyval.substring(0, pos);
        parsed[1] = keyval.substring(pos+1);

        return parsed;
    }

    public static CmdStdinContext<JsonNode> parseFileOrStdin(String file) {

        String content = readFileOrStdin(file).trim();
        JsonNode result = null;

        if (content.length() == 0) {
            throw new RuntimeException("Document provided by --file option is empty");
        }

        try {
            result = JsonSerialization.readValue(content, JsonNode.class);
        } catch (JsonParseException e) {
            throw new RuntimeException("Not a valid JSON document - " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the input document as JSON: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Not a valid JSON document", e);
        }

        CmdStdinContext<JsonNode> ctx = new CmdStdinContext<>();
        ctx.setContent(content);
        ctx.setResult(result);
        return ctx;
    }

    public static <T> CmdStdinContext<JsonNode> mergeAttributes(CmdStdinContext<JsonNode> ctx, ObjectNode newObject, List<AttributeOperation> attrs) {
        String content = ctx.getContent();
        JsonNode node = ctx.getResult();
        if (node != null && !node.isObject()) {
            throw new RuntimeException("Not a JSON object: " + node);
        }
        ObjectNode result = (ObjectNode) node;
        try {

            if (result == null) {
                try {
                    result = newObject;
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to instantiate object: " + e.getMessage(), e);
                }
            }

            if (result != null) {
                try {
                    setAttributes(result, attrs);
                } catch (AttributeException e) {
                    throw new RuntimeException("Failed to set attribute '" + e.getAttributeName() + "' on document type '" + result.getClass().getName() + "'", e);
                }
                content = JsonSerialization.writeValueAsString(result);
            } else {
                throw new RuntimeException("Setting attributes is not supported for type: " + result.getClass().getName());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge set attributes with configuration from file", e);
        }

        ctx.setContent(content);
        ctx.setResult(result);
        return ctx;
    }
}
