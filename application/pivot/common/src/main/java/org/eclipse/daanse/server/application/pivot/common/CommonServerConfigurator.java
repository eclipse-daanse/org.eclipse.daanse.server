/*
* Copyright (c) 2026 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.server.application.pivot.common;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires the database independent parts of the pivot server from environment
 * variables: catalog mapping provider, context group, XMLA connector, the XMLA
 * servlet on the HTTP whiteboard and the optional CORS filter. The database specific configurator of
 * each image contributes the DataSource and the BasicContext.
 */
@Component(immediate = true)
@RequireConfigurationAdmin
@RequireServiceComponentRuntime
public class CommonServerConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(CommonServerConfigurator.class);

    private static final String PID_XMLA_SERVLET =
            "org.eclipse.daanse.xmla.server.whiteboard.servlet.XmlaServlet";
    private static final String PID_XMLA_SERVICE = "daanse.olap.xmla.connector.OlapXmlaConnector";
    private static final String PID_BASIC_AUTHENTICATOR =
            "org.eclipse.daanse.xmla.server.auth.basic.BasicAuthenticator";
    private static final String PID_LDAP_CREDENTIALS =
            "org.eclipse.daanse.xmla.server.auth.store.ldap.LdapCredentials";
    private static final String PID_LDAP_ROLE_PROVIDER =
            "org.eclipse.daanse.xmla.server.auth.store.ldap.LdapRoleProvider";

    @Reference
    ConfigurationAdmin ca;

    private Configuration confMappingProvider;
    private Configuration confContextGroup;
    private Configuration confXmlaService;
    private Configuration confXmlaServlet;
    private Configuration confCorsFilter;
    private Configuration confBasicAuthenticator;
    private Configuration confLdapCredentials;
    private Configuration confLdapRoleProvider;

    @Activate
    public void activate() throws IOException {
        logger.info("Activating pivot server setup");

        initMappingProvider();
        initContextGroup();
        initXmlaService();
        initXmlaServlet();
        initCorsFilter();
        initAuth();

        logger.info("Pivot server setup completed");
    }

    private void initMappingProvider() throws IOException {
        confMappingProvider = ca.getFactoryConfiguration(
                org.eclipse.daanse.rolap.mapping.model.provider.Constants.PID_EMF_MAPPING_PROVIDER,
                ServerConstants.CONFIG_IDENT, "?");

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(ServerConstants.PROP_IDENT, ServerConstants.IDENT_MAPPING);
        props.put(org.eclipse.daanse.rolap.mapping.model.provider.Constants.RESOURCE_URL,
                Env.get(ServerConstants.ENV_CATALOG_RESOURCE, ServerConstants.DEFAULT_CATALOG_RESOURCE));
        Env.get(ServerConstants.ENV_CATALOG_ADDITIONAL_GLOBS)
                .ifPresent(globs -> props.put(
                        org.eclipse.daanse.rolap.mapping.model.provider.Constants.ADDITIONAL_RESOURCE_GLOBS,
                        Env.splitList(globs)));

        confMappingProvider.update(props);
    }

    private void initContextGroup() throws IOException {
        confContextGroup = ca.getFactoryConfiguration(
                org.eclipse.daanse.olap.core.api.Constants.BASIC_CONTEXT_GROUP_PID, ServerConstants.CONFIG_IDENT, "?");

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(org.eclipse.daanse.olap.core.api.Constants.BASIC_CONTEXT_GROUP_REF_NAME_CONTEXTS
                + ServerConstants.TARGET_EXT,
                "(" + ServerConstants.PROP_IDENT + "=" + ServerConstants.IDENT_CONTEXT + ")");

        confContextGroup.update(props);
    }

    private void initXmlaService() throws IOException {
        confXmlaService = ca.getFactoryConfiguration(PID_XMLA_SERVICE, ServerConstants.CONFIG_IDENT, "?");

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("contextGroup" + ServerConstants.TARGET_EXT, "(service.pid=*)");

        confXmlaService.update(props);
    }

    private void initXmlaServlet() throws IOException {
        confXmlaServlet = ca.getFactoryConfiguration(PID_XMLA_SERVLET, ServerConstants.CONFIG_IDENT, "?");

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("osgi.http.whiteboard.servlet.pattern",
                Env.get(ServerConstants.ENV_XMLA_PATH, ServerConstants.DEFAULT_XMLA_PATH));
        // Anonymous requests are served (with no roles) unless switched off.
        props.put("requirePrincipal", !Env.get(ServerConstants.ENV_AUTH_ANONYMOUS, true));

        confXmlaServlet.update(props);
    }

    private void initCorsFilter() throws IOException {
        if (!Env.get(ServerConstants.ENV_CORS_ENABLED, true)) {
            logger.info("CORS filter disabled via {}", ServerConstants.ENV_CORS_ENABLED);
            return;
        }

        confCorsFilter = ca.getFactoryConfiguration(
                org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PID_FILTER_CORS,
                ServerConstants.CONFIG_IDENT, "?");

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("osgi.http.whiteboard.filter.pattern", "/*");
        props.put(org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PROPERTY_ALLOW_CREDENTIALS_PARAM,
                Env.get(ServerConstants.ENV_CORS_ALLOW_CREDENTIALS, true));
        props.put(org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PROPERTY_ALLOWED_ORIGINS_PARAM,
                Env.splitList(Env.get(ServerConstants.ENV_CORS_ALLOWED_ORIGINS, "*")));
        props.put(org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PROPERTY_ALLOWED_HEADERS_PARAM,
                Env.splitList(Env.get(ServerConstants.ENV_CORS_ALLOWED_HEADERS, "*")));

        confCorsFilter.update(props);
    }

    private void initAuth() throws IOException {
        // Without an LDAP url the credential store stays unconfigured, the basic
        // authenticator never registers and the endpoint serves anonymously.
        Optional<String> url = Env.get(ServerConstants.ENV_LDAP_URL);
        if (url.isEmpty()) {
            return;
        }

        confLdapCredentials = ca.getConfiguration(PID_LDAP_CREDENTIALS, "?");
        confLdapCredentials.update(ldapConnectionProps(url.get()));

        // Roles from LDAP groups; without a group search base callers
        // authenticate but carry no roles.
        Optional<String> groupSearchBase = Env.get(ServerConstants.ENV_LDAP_GROUP_SEARCH_BASE);
        if (groupSearchBase.isPresent()) {
            confLdapRoleProvider = ca.getConfiguration(PID_LDAP_ROLE_PROVIDER, "?");

            Dictionary<String, Object> props = ldapConnectionProps(url.get());
            props.put("groupSearchBase", groupSearchBase.get());
            Env.get(ServerConstants.ENV_LDAP_GROUP_SEARCH_FILTER)
                    .ifPresent(v -> props.put("groupSearchFilter", v));
            Env.get(ServerConstants.ENV_LDAP_GROUP_NAME_ATTRIBUTE)
                    .ifPresent(v -> props.put("groupNameAttribute", v));
            Env.get(ServerConstants.ENV_LDAP_MEMBER_OF_ATTRIBUTE)
                    .ifPresent(v -> props.put("memberOfAttribute", v));

            confLdapRoleProvider.update(props);
        }

        confBasicAuthenticator = ca.getConfiguration(PID_BASIC_AUTHENTICATOR, "?");
        Dictionary<String, Object> basicProps = new Hashtable<>();
        Env.get(ServerConstants.ENV_AUTH_REALM).ifPresent(v -> basicProps.put("realm", v));
        confBasicAuthenticator.update(basicProps);

        logger.info("LDAP backed basic authentication enabled against {}", url.get());
    }

    private Dictionary<String, Object> ldapConnectionProps(String url) {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("url", url);
        Env.get(ServerConstants.ENV_LDAP_USER_DN_PATTERN).ifPresent(v -> props.put("userDnPattern", v));
        Env.get(ServerConstants.ENV_LDAP_USER_SEARCH_BASE).ifPresent(v -> props.put("userSearchBase", v));
        Env.get(ServerConstants.ENV_LDAP_USER_SEARCH_FILTER).ifPresent(v -> props.put("userSearchFilter", v));
        Env.get(ServerConstants.ENV_LDAP_SERVICE_BIND_DN).ifPresent(v -> props.put("serviceBindDn", v));
        Env.get(ServerConstants.ENV_LDAP_SERVICE_BIND_PASSWORD)
                .ifPresent(v -> props.put("serviceBindPassword", v));
        Env.get(ServerConstants.ENV_LDAP_TRANSPORT_SECURITY).ifPresent(v -> props.put("transportSecurity", v));
        Env.get(ServerConstants.ENV_LDAP_ALLOW_UNENCRYPTED)
                .ifPresent(v -> props.put("allowUnencrypted", Boolean.parseBoolean(v)));
        Env.get(ServerConstants.ENV_LDAP_CONNECT_TIMEOUT_MILLIS)
                .ifPresent(v -> props.put("connectTimeoutMillis", Integer.parseInt(v)));
        Env.get(ServerConstants.ENV_LDAP_READ_TIMEOUT_MILLIS)
                .ifPresent(v -> props.put("readTimeoutMillis", Integer.parseInt(v)));
        Env.get(ServerConstants.ENV_LDAP_REFERRAL).ifPresent(v -> props.put("referral", v));
        return props;
    }

    @Deactivate
    public void deactivate() throws IOException {
        logger.info("Deactivating pivot server setup");

        if (confMappingProvider != null) {
            confMappingProvider.delete();
        }
        if (confContextGroup != null) {
            confContextGroup.delete();
        }
        if (confXmlaService != null) {
            confXmlaService.delete();
        }
        if (confXmlaServlet != null) {
            confXmlaServlet.delete();
        }
        if (confCorsFilter != null) {
            confCorsFilter.delete();
        }
        if (confBasicAuthenticator != null) {
            confBasicAuthenticator.delete();
        }
        if (confLdapCredentials != null) {
            confLdapCredentials.delete();
        }
        if (confLdapRoleProvider != null) {
            confLdapRoleProvider.delete();
        }
    }
}
