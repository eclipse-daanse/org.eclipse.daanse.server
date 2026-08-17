/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.server.application.probe;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.daanse.io.fs.watcher.api.FileSystemWatcherWhiteboardConstants;
import org.eclipse.daanse.olap.core.api.Constants;
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

@Component(immediate = true)
@RequireConfigurationAdmin
@RequireServiceComponentRuntime
public class Probe {

    private static final String DAANSE_PROBE_CATALOG_DIR = "daanse.probe.catalog.dir";
    private static final String DAANSE_PROBE_REQUIRE_LOGIN = "daanse.probe.requireLogin";

    private static final Logger logger = LoggerFactory.getLogger(Probe.class);

    private static final String CONFIG_IDENT = "probe";
    private static final String TARGET_EXT = ".target";

    private static final String PID_XMLA_SERVICE = "daanse.olap.xmla.connector.OlapXmlaConnector";
    private static final String PID_XMLA_SERVLET =
            "org.eclipse.daanse.xmla.server.whiteboard.servlet.XmlaServlet";
    private static final String PID_CONTEXT_GROUP = "daanse.olap.core.BasicContextGroup";
    private static final String PID_FIXED_IDENTITY =
            "org.eclipse.daanse.xmla.server.auth.dummy.FixedIdentityAuthenticator";
    private static final String PID_BASIC_PIPE_ROLES =
            "org.eclipse.daanse.xmla.server.auth.dummy.BasicAuthPipeRoleAuthenticator";

    @Reference
    ConfigurationAdmin ca;

    private Configuration confContextGroupXmlaService;
    private Configuration configXmlaServlet;
    private Configuration configFixedIdentity;
    private Configuration configBasicPipeRoles;
    private Configuration confDataSource;
    private Configuration confContextGroup;

    private Configuration configDocumenterMarkdown;
    private Configuration configAutoDocumenter;

    private Configuration configCorsFilter;

    private Configuration configOdcWriter;

    private Configuration configAutoODC;

    private Configuration configFileReporter;

    @Activate
    public void activate() throws IOException {
        logger.info("Activating ProbeSetup");

        initCorsFilter();
        initXmlaServlet();
        initFixedIdentity();
        initBasicPipeRoles();
        initXmlaService();
        initFileListener();
        initContextGroup();
        initDocumenter();
        initODC();
        initCheckReporter();

        logger.info("ProbeSetup activation completed");
    }

    private void initFileListener() throws IOException {

        String catalogPath = System.getProperty(DAANSE_PROBE_CATALOG_DIR, "./catalog");

        confDataSource = ca.getFactoryConfiguration(ProbeFileListener.PID, CONFIG_IDENT, "?");

        Dictionary<String, Object> propsDS = new Hashtable<>();
        propsDS.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, catalogPath);

        confDataSource.update(propsDS);
    }

    private void initContextGroup() throws IOException {

        confContextGroup = ca.getFactoryConfiguration(PID_CONTEXT_GROUP, CONFIG_IDENT, "?");

        Dictionary<String, Object> propsCG = new Hashtable<>();
        propsCG.put(Constants.BASIC_CONTEXT_GROUP_REF_NAME_CONTEXTS + TARGET_EXT, "(service.pid=*)");

        confContextGroup.update(propsCG);
    }

    /** The XMLA endpoint on the HTTP whiteboard (Jetty, port 8080). */
    private void initXmlaServlet() throws IOException {
        configXmlaServlet = ca.getFactoryConfiguration(PID_XMLA_SERVLET, CONFIG_IDENT, "?");
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("osgi.http.whiteboard.servlet.pattern", "/xmla");
        // A client only sends credentials after a 401; without the challenge it
        // arrives anonymous and falls through to the fixed identity below.
        dict.put("requirePrincipal", Boolean.getBoolean(DAANSE_PROBE_REQUIRE_LOGIN));
        configXmlaServlet.update(dict);
    }

    /**
     * The probe authenticates nobody: anonymous callers become the same
     * configured user, enough to exercise the roles a catalog defines.
     */
    private void initFixedIdentity() throws IOException {
        configFixedIdentity = ca.getConfiguration(PID_FIXED_IDENTITY, "?");
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("acknowledgeUnauthenticated", Boolean.TRUE);
        dict.put("userName", "probe");
        // Roles are intersected with the catalog's own; the name must be one a
        // catalog declares (FoodMart declares "Administrator").
        dict.put("roles", new String[] { "Administrator" });
        configFixedIdentity.update(dict);
    }

    /** Log in as {@code UserName|Role1|Role2}; nothing is verified. */
    private void initBasicPipeRoles() throws IOException {
        configBasicPipeRoles = ca.getConfiguration(PID_BASIC_PIPE_ROLES, "?");
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("acknowledgeUnverifiedCredentials", Boolean.TRUE);
        dict.put("realm", "Daanse Probe");
        configBasicPipeRoles.update(dict);
    }

    private void initXmlaService() throws IOException {

        confContextGroupXmlaService = ca.getFactoryConfiguration(PID_XMLA_SERVICE, CONFIG_IDENT, "?");

        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("contextGroup" + TARGET_EXT, "(service.pid=*)");

        confContextGroupXmlaService.update(dict);
    }

    private void initCorsFilter() throws IOException {

        configCorsFilter = ca.getFactoryConfiguration(
                org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PID_FILTER_CORS, CONFIG_IDENT, "?");
        Dictionary<String, Object> dict = new Hashtable<>();

        dict.put("osgi.http.whiteboard.filter.pattern", "/*");
        dict.put(org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PROPERTY_ALLOW_CREDENTIALS_PARAM, true);

        dict.put(org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PROPERTY_ALLOWED_ORIGINS_PARAM, "*");
        dict.put(org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PROPERTY_ALLOWED_HEADERS_PARAM, "*");
        configCorsFilter.update(dict);

    }

    private void initDocumenter() throws IOException {

        configDocumenterMarkdown = ca.getFactoryConfiguration(
                org.eclipse.daanse.rolap.documentation.common.api.Constants.DOC_PROVIDER_MARKDOWN_PID, CONFIG_IDENT,
                "?");
        Dictionary<String, Object> dict = new Hashtable<>();
        configDocumenterMarkdown.update(dict);

        configAutoDocumenter = ca.getFactoryConfiguration(
                org.eclipse.daanse.rolap.documentation.common.api.Constants.AUTO_DOCUMENTER_PID, CONFIG_IDENT, "?");
        dict = new Hashtable<>();
        configAutoDocumenter.update(dict);

    }

    private void initODC() throws IOException {
        configOdcWriter = ca.getFactoryConfiguration(org.eclipse.daanse.olap.odc.simple.api.Constants.CREATOR_PID,
                CONFIG_IDENT, "?");
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(org.eclipse.daanse.olap.odc.simple.api.Constants.CREATOR_PROPERTY_DATASOURCE,
                "http://localhost:8080/xmla");
        configOdcWriter.update(dict);

        configAutoODC = ca.getFactoryConfiguration(org.eclipse.daanse.olap.odc.simple.api.Constants.AUTO_ODC_PID,
                CONFIG_IDENT, "?");
        dict = new Hashtable<>();
        configAutoODC.update(dict);

    }

    private void initCheckReporter() throws IOException {
        configFileReporter = ca.getFactoryConfiguration("daanse.olap.check.reporter.file", CONFIG_IDENT, "?");
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("output.dir", "./output/check-results");
        configFileReporter.update(dict);
    }

    @Deactivate
    public void deactivate() throws IOException {
        logger.info("Deactivating ProbeSetup");

        if (configFixedIdentity != null) {
            configFixedIdentity.delete();
        }
        if (configBasicPipeRoles != null) {
            configBasicPipeRoles.delete();
        }
        if (configXmlaServlet != null) {
            configXmlaServlet.delete();
        }
        if (configCorsFilter != null) {
            configCorsFilter.delete();
        }
        if (configDocumenterMarkdown != null) {
            configDocumenterMarkdown.delete();
        }

        if (configAutoDocumenter != null) {
            configAutoDocumenter.delete();
        }

        if (configAutoODC != null) {
            configAutoODC.delete();
        }

        if (configOdcWriter != null) {
            configOdcWriter.delete();
        }

        if (configFileReporter != null) {
            configFileReporter.delete();
        }

        logger.info("ProbeSetup deactivation completed");
    }

}
