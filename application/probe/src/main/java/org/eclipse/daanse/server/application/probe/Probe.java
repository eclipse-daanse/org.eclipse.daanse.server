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
import org.eclipse.daanse.jakarta.servlet.filter.auth.dummy.role.BasicAuthPipeRoleFilter;
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

    private static final Logger logger = LoggerFactory.getLogger(Probe.class);

    private static final String CONFIG_IDENT = "probe";
    private static final String TARGET_EXT = ".target";

    private static final String PID_MS_SOAP_MSG_SAAJ = "daanse.xmla.server.jakarta.saaj.XmlaServlet";
    private static final String PID_XMLA_SERVICE = "daanse.olap.xmla.bridge.ContextGroupXmlaService";
    private static final String PID_CONTEXT_GROUP = "daanse.olap.core.BasicContextGroup";

    @Reference
    ConfigurationAdmin ca;

    private Configuration configXmlaEndpoint;
    private Configuration confSoapLoggingHandler;
    private Configuration confContextGroupXmlaService;
    private Configuration confDataSource;
    private Configuration confContextGroup;
    private Configuration configRoleAuthFilter;

    private Configuration configCorsFilter;

    @Activate
    public void activate() throws IOException {
        logger.info("Activating ProbeSetup");

        initXmlaEndPoint();
        initRoleAuthFilter();
        initCorsFilter();
        initXmlaService();
        initFileListener();
        initContextGroup();

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

    private void initXmlaService() throws IOException {

        confContextGroupXmlaService = ca.getFactoryConfiguration(PID_XMLA_SERVICE, CONFIG_IDENT, "?");

        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("contextGroup" + TARGET_EXT, "(service.pid=*)");

        confContextGroupXmlaService.update(dict);
    }

    private void initRoleAuthFilter() throws IOException {

        configRoleAuthFilter = ca.getFactoryConfiguration(BasicAuthPipeRoleFilter.PID, CONFIG_IDENT, "?");
        Dictionary<String, Object> dict = new Hashtable<>();

        dict.put("osgi.http.whiteboard.filter.pattern", "/*");
        configRoleAuthFilter.update(dict);

    }

    private void initCorsFilter() throws IOException {

        configCorsFilter = ca.getFactoryConfiguration(
                org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PID_FILTER_CORS, CONFIG_IDENT, "?");
        Dictionary<String, Object> dict = new Hashtable<>();

        dict.put("osgi.http.whiteboard.filter.pattern", "/*");
        dict.put(org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PROPERTY_ALLOW_CREDENTIALS_PARAM, true);
        dict.put(org.eclipse.daanse.jakarta.servlet.filter.cors.api.Constants.PROPERTY_ALLOWED_ORIGINS_PARAM, "*");
        configCorsFilter.update(dict);

    }

    private void initXmlaEndPoint() throws IOException {

        configXmlaEndpoint = ca.getFactoryConfiguration(PID_MS_SOAP_MSG_SAAJ, CONFIG_IDENT, "?");

        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("xmlaService.target", "(service.pid=*)");
        dict.put("osgi.http.whiteboard.servlet.pattern", "/xmla");

        configXmlaEndpoint.update(dict);

    }

    @Deactivate
    public void deactivate() throws IOException {
        logger.info("Deactivating ProbeSetup");

        if (configXmlaEndpoint != null) {
            configXmlaEndpoint.delete();
        }
        if (configRoleAuthFilter != null) {
            configRoleAuthFilter.delete();
        }
        if (configCorsFilter != null) {
            configCorsFilter.delete();
        }
        if (confSoapLoggingHandler != null) {
            confSoapLoggingHandler.delete();
        }

        logger.info("ProbeSetup deactivation completed");
    }

}
