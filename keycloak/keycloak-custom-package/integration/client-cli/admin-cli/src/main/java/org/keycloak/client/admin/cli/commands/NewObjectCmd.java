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
package org.keycloak.client.admin.cli.commands;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.admin.cli.common.AttributeOperation;
import org.keycloak.client.admin.cli.common.CmdStdinContext;
import org.keycloak.client.admin.cli.util.AccessibleBufferOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.keycloak.client.admin.cli.common.AttributeOperation.Type.SET;
import static org.keycloak.client.admin.cli.util.IoUtil.copyStream;
import static org.keycloak.client.admin.cli.util.IoUtil.printErr;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;
import static org.keycloak.client.admin.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.admin.cli.util.OutputUtil.MAPPER;
import static org.keycloak.client.admin.cli.util.ParseUtil.mergeAttributes;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseFileOrStdin;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseKeyVal;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "new-object", description = "Command to create new JSON objects locally")
public class NewObjectCmd extends AbstractGlobalOptionsCmd {

    @Option(shortName = 'f', name = "file", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    String file;

    @Option(shortName = 'c', name = "compressed", description = "Don't pretty print the output", hasValue = false)
    boolean compressed;

    //@OptionGroup(shortName = 's', name = "set", description = "Set attribute to the specified value")
    //Map<String, String> attributes = new LinkedHashMap<>();


    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            if (printHelp()) {
                return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
            }

            processGlobalOptions();

            return process(commandInvocation);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + suggestHelp(), e);
        } finally {
            commandInvocation.stop();
        }
    }

    public CommandResult process(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        List<AttributeOperation> attrs = new LinkedList<>();

        Iterator<String> it = args.iterator();

        while (it.hasNext()) {
            String option = it.next();
            switch (option) {
                case "-s":
                case "--set": {
                    if (!it.hasNext()) {
                        throw new IllegalArgumentException("Option " + option + " requires a value");
                    }
                    String[] keyVal = parseKeyVal(it.next());
                    attrs.add(new AttributeOperation(SET, keyVal[0], keyVal[1]));
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Invalid option: " + option);
                }
            }
        }

        InputStream body = null;

        CmdStdinContext<JsonNode> ctx = new CmdStdinContext<>();

        if (file != null) {
            ctx = parseFileOrStdin(file);
        }

        if (attrs.size() > 0) {
            ctx = mergeAttributes(ctx, MAPPER.createObjectNode(), attrs);
        }

        if (body == null && ctx.getContent() != null) {
            body = new ByteArrayInputStream(ctx.getContent().getBytes(Charset.forName("utf-8")));
        }

        AccessibleBufferOutputStream abos = new AccessibleBufferOutputStream(System.out);

        if (!compressed) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            copyStream(body, buffer);

            try {
                JsonNode rootNode = MAPPER.readValue(buffer.toByteArray(), JsonNode.class);
                // now pretty print it to output
                MAPPER.writeValue(abos, rootNode);
            } catch (Exception ignored) {
                copyStream(new ByteArrayInputStream(buffer.toByteArray()), abos);
            }
        } else {
            copyStream(body, System.out);
        }

        int lastByte = abos.getLastByte();
        if (lastByte != -1 && lastByte != 13 && lastByte != 10) {
            printErr("");
        }

        return CommandResult.SUCCESS;
    }


    @Override
    protected boolean nothingToDo() {
        return file == null && (args == null || args.size() == 0);
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help create' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " new-object [ARGUMENTS]");
        out.println();
        out.println("Command to compose JSON objects from attributes, and merge changes into existing JSON documents.");
        out.println();
        out.println("This is a local command that does not perform any server requests. It's functionality is fully ");
        out.println("integrated into 'create', 'update' and 'delete' commands. It's supposed to be a helper tool only.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                    Print full stack trace when exiting with error");
        out.println();
        out.println("  Command specific options:");
        out.println("    -s, --set NAME=VALUE  Set a specific attribute NAME to a specified value VALUE");
        out.println("    -f, --file FILENAME   Read object from file or standard input if FILENAME is set to '-'");
        out.println("    -c, --compressed      Don't pretty print the output");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Create a new JSON document with two top level attributes:");
        out.println("  " + PROMPT + " " + CMD + " new-object -s realm=demorealm -s enabled=true");
        out.println();
        out.println("Read a JSON document and apply changes on top of it:");
        if (OS_ARCH.isWindows()) {
            out.println("  " + PROMPT + " echo { \"clientId\": \"my_client\" } | " + CMD + " new-object -s enabled=true -f -");
        } else {
            out.println("  " + PROMPT + " " + CMD + " new-object -s enabled=true -f - << EOF");
            out.println("  {");
            out.println("    \"clientId\": \"my_client\"");
            out.println("  }");
            out.println("  EOF");
        }
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }

}
