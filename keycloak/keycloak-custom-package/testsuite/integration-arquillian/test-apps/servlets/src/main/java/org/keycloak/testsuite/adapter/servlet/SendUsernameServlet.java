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

package org.keycloak.testsuite.adapter.servlet;


import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.adapters.saml.SamlAuthenticationError;
import org.keycloak.adapters.saml.SamlPrincipal;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author mhajas
 * @version $Revision: 1 $
 */
@Path("/")
public class SendUsernameServlet {

    private static boolean checkRoles = false;
    private static SamlAuthenticationError authError;
    private static Principal sentPrincipal;
    private static List<String> checkRolesList = Collections.singletonList("manager");

    @Context
    private HttpServletRequest httpServletRequest;

    @GET
    @NoCache
    public Response doGet(@QueryParam("checkRoles") boolean checkRolesFlag) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doGet() check roles is " + (checkRolesFlag || checkRoles));
        if (httpServletRequest.getUserPrincipal() != null && (checkRolesFlag || checkRoles) && !checkRoles()) {
            return Response.status(Response.Status.FORBIDDEN).entity("Forbidden").build();
        }

        return Response.ok(getOutput()).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_TYPE + ";charset=UTF-8").build();
    }

    @POST
    @NoCache
    public Response doPost(@QueryParam("checkRoles") boolean checkRolesFlag) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doPost() check roles is " + (checkRolesFlag || checkRoles));

        if (httpServletRequest.getUserPrincipal() != null && (checkRolesFlag || checkRoles) && !checkRoles()) {
            throw new RuntimeException("User: " + httpServletRequest.getUserPrincipal() + " do not have required role");
        }

        return Response.ok(getOutput()).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_TYPE + ";charset=UTF-8").build();

    }

    @GET
    @Path("getAttributes")
    public Response getSentPrincipal() throws IOException {
        System.out.println("In SendUsername Servlet getSentPrincipal()");
        sentPrincipal = httpServletRequest.getUserPrincipal();

        return Response.ok(getAttributes()).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_TYPE + ";charset=UTF-8").build();

    }

    @GET
    @Path("{path}")
    public Response doGetElseWhere(@PathParam("path") String path, @QueryParam("checkRoles") boolean checkRolesFlag) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doGetElseWhere() - path: " + path);
        return doGet(checkRolesFlag);
    }

    @POST
    @Path("{path}")
    public Response doPostElseWhere(@PathParam("path") String path, @QueryParam("checkRoles") boolean checkRolesFlag) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doPostElseWhere() - path: " + path);
        return doPost(checkRolesFlag);
    }

    @POST
    @Path("error.html")
    public Response errorPagePost() {
        authError = (SamlAuthenticationError) httpServletRequest.getAttribute(AuthenticationError.class.getName());
        Integer statusCode = (Integer) httpServletRequest.getAttribute("javax.servlet.error.status_code");
        System.out.println("In SendUsername Servlet errorPage() status code: " + statusCode);

        return Response.ok(getErrorOutput(statusCode)).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_TYPE + ";charset=UTF-8").build();

    }

    @GET
    @Path("error.html")
    public Response errorPageGet() {
        return errorPagePost();
    }


    @GET
    @Path("checkRoles")
    public String checkRolesEndPoint() {
        checkRoles = true;
        System.out.println("Setting checkRoles to true");
        return "Roles will be checked";
    }

    @GET
    @Path("uncheckRoles")
    public String uncheckRolesEndPoint() {
        checkRoles = false;
        System.out.println("Setting checkRoles to false");
        checkRolesList = Collections.singletonList("manager");
        return "Roles will not be checked";
    }

    @GET
    @Path("setCheckRoles")
    public String setCheckRoles(@QueryParam("roles") String roles) {
        checkRolesList = Arrays.asList(roles.split(","));
        checkRoles = true;
        System.out.println("Setting checkRolesList to " + checkRolesList.toString());
        return "These roles will be checked: " + checkRolesList.toString();
    }


    private boolean checkRoles() {
        for (String role : checkRolesList) {
            System.out.println("In checkRoles() checking role " + role + " for user " + httpServletRequest.getUserPrincipal().getName());
            if (!httpServletRequest.isUserInRole(role)) {
                System.out.println("User is not in role " + role);
                return false;
            }
        }

        return true;
    }

    private String getOutput() {
        String output = "request-path: ";
        output += httpServletRequest.getServletPath();
        output += "\n";
        output += "principal=";
        Principal principal = httpServletRequest.getUserPrincipal();

        if (principal == null) {
            return output + "null";
        }

        sentPrincipal = principal;

        return output + principal.getName();
    }

    private String getErrorOutput(Integer statusCode) {
        String output = "<html><head><title>Error Page</title></head><body><h1>There was an error</h1>";
        if (statusCode != null)
            output += "<br/>HTTP status code: " + statusCode;
        if (authError != null)
            output += "<br/>Error info: " + authError.toString();
        return output + "</body></html>";
    }

    private String getAttributes() {
        SamlPrincipal principal = (SamlPrincipal) sentPrincipal;
        String output = "attribute email: " + principal.getAttribute(X500SAMLProfileConstants.EMAIL.get());
        output += "<br /> topAttribute: " + principal.getAttribute("topAttribute");
        output += "<br /> boolean-attribute: " + principal.getAttribute("boolean-attribute");
        output += "<br /> level2Attribute: " + principal.getAttribute("level2Attribute");
        output += "<br /> group: " + principal.getAttributes("group").toString();
        output += "<br /> friendlyAttribute email: " + principal.getFriendlyAttribute("email");
        output += "<br /> phone: " + principal.getAttribute("phone");
        output += "<br /> friendlyAttribute phone: " + principal.getFriendlyAttribute("phone");
        output += "<br /> hardcoded-attribute: ";
        for (String attr : principal.getAttributes("hardcoded-attribute")) {
            output += attr + ",";
        }

        return output;
    }
}
