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

import static org.eclipse.daanse.rolap.mapping.model.provider.Constants.PID_EMF_MAPPING_PROVIDER;
import static org.eclipse.daanse.rolap.mapping.model.provider.Constants.RESOURCE_URL;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.daanse.io.fs.watcher.api.FileSystemWatcherListener;
import org.eclipse.daanse.io.fs.watcher.api.propertytypes.FileSystemWatcherListenerProperties;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FileSystemWatcherListenerProperties(recursive = true, pattern = ".*.xmi")
@Component(service = FileSystemWatcherListener.class, configurationPid = CatalogXmiFileListener.PID, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CatalogXmiFileListener implements FileSystemWatcherListener {

    private static final Logger logger = LoggerFactory.getLogger(CatalogXmiFileListener.class);

    public static final String PID = "daanse.server.application.probe.CatalogXmiFileListener";

    @Reference
    private ConfigurationAdmin ca;

    private String matcherKey;

    @Activate
    public CatalogXmiFileListener(Map<String, Object> props) {
        this.matcherKey = (String) props.get(ProbeFileListener.MATCHER_KEY);
    }

    private Path basePath;
    private Configuration configuration;

    @Override
    public void handleBasePath(Path basePath) {
        this.basePath = basePath;
        logger.info("Handling base path: {}", basePath);
    }

    @Override
    public void handleInitialPaths(List<Path> paths) {
        logger.info("Handling initial paths: {}", paths);
        configMappingReader();
    }

    @Override
    public void handlePathEvent(Path path, Kind<Path> kind) {
        logger.info("Handling path event for: {} with kind: {}", path, kind);
        configMappingReader();
    }

    private void configMappingReader() {
        deleteConfig();

        try {
            configuration = ca.getFactoryConfiguration(PID_EMF_MAPPING_PROVIDER, UUID.randomUUID().toString(), "?");

            Dictionary<String, Object> props = new Hashtable<>();
            props.put(RESOURCE_URL, basePath.resolve("catalog.xmi").toAbsolutePath().toString());
            props.put(ProbeFileListener.KEY_FILE_CONTEXT_MATCHER, matcherKey);

            configuration.update(props);
        } catch (IOException e) {
            logger.error("Failed to configure mapping reader for path: {}", basePath, e);
        }

    }

    private void deleteConfig() {
        if (configuration != null) {
            try {
                configuration.delete();
            } catch (IOException e) {
                logger.error("Failed to delete configuration", e);
            }
        }
    }

    @Deactivate
    private void deactivate() {
        logger.info("Deactivating MappingFilesWatcher");
        deleteConfig();
    }
}
