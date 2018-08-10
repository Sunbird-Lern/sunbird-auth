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

package org.keycloak.storage.ldap.idm.query;

import java.io.UnsupportedEncodingException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum EscapeStrategy {


    // LDAP special characters like * ( ) \ are not escaped. Only non-ASCII characters like é are escaped
    NON_ASCII_CHARS_ONLY {

        @Override
        public String escape(String input) {
            try {
                StringBuilder output = new StringBuilder();

                for (byte b : input.getBytes("UTF-8")) {
                    appendByte(b, output);
                }

                return output.toString();
            } catch (UnsupportedEncodingException uee) {
                throw new RuntimeException(uee);
            }
        }

    },


    // Escaping of LDAP special characters including non-ASCII characters like é
    DEFAULT {


        @Override
        public String escape(String input) {
            try {
                StringBuilder output = new StringBuilder();

                for (byte b : input.getBytes("UTF-8")) {
                    switch (b) {
                        case 0x5c:
                            output.append("\\5c"); // \
                            break;
                        case 0x2a:
                            output.append("\\2a"); // *
                            break;
                        case 0x28:
                            output.append("\\28"); // (
                            break;
                        case 0x29:
                            output.append("\\29"); // )
                            break;
                        case 0x00:
                            output.append("\\00"); // \u0000
                            break;
                        default: {
                            appendByte(b, output);
                        }
                    }
                }

                return output.toString();
            } catch (UnsupportedEncodingException uee) {
                throw new RuntimeException(uee);
            }
        }

    };


    public abstract String escape(String input);


    protected void appendByte(byte b, StringBuilder output) {
        if (b >= 0) {
            output.append((char) b);
        } else {
            int i = -256 ^ b;
            output.append("\\").append(Integer.toHexString(i));
        }
    }

}
