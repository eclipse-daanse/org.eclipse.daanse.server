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
package org.eclipse.daanse.server.application.probe;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
@Component(service = FileSystemWatcherListener.class, configurationPid = CheckSuiteXmiFileListener.PID, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CheckSuiteXmiFileListener implements FileSystemWatcherListener {

    private static final Logger logger = LoggerFactory.getLogger(CheckSuiteXmiFileListener.class);

    public static final String PID = "daanse.server.application.probe.CheckSuiteXmiFileListener";

    private static final String PID_CHECK_SUITE_PROVIDER = "org.eclipse.daanse.olap.check.suite.provider";

    @Reference
    private ConfigurationAdmin ca;

    private String matcherKey;

    private final Map<Path, Configuration> configurations = new ConcurrentHashMap<>();

    @Activate
    public CheckSuiteXmiFileListener(Map<String, Object> props) {
        this.matcherKey = (String) props.get(ProbeFileListener.MATCHER_KEY);
    }

    @Override
    public void handleBasePath(Path basePath) {
        logger.info("Handling check suite base path: {}", basePath);
    }

    @Override
    public void handleInitialPaths(List<Path> paths) {
        if (paths.isEmpty()) {
            logger.debug("No check suite XMI files found, skipping");
            return;
        }
        logger.info("Handling check suite initial paths: {}", paths);
        for (Path path : paths) {
            addCheckSuiteConfig(path);
        }
    }

    @Override
    public void handlePathEvent(Path path, Kind<Path> kind) {
        logger.info("Handling check suite path event for: {} with kind: {}", path, kind);

        if (java.nio.file.StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
            removeCheckSuiteConfig(path);
        } else if (java.nio.file.StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
            addCheckSuiteConfig(path);
        } else if (java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {
            removeCheckSuiteConfig(path);
            addCheckSuiteConfig(path);
        }
    }

    private void addCheckSuiteConfig(Path path) {
        try {
            Configuration configuration = ca.getFactoryConfiguration(PID_CHECK_SUITE_PROVIDER,
                    UUID.randomUUID().toString(), "?");

            Dictionary<String, Object> props = new Hashtable<>();
            props.put("resource.url", path.toAbsolutePath().toString());
            props.put(ProbeFileListener.KEY_FILE_CONTEXT_MATCHER, matcherKey);

            configuration.update(props);
            configurations.put(path, configuration);
            logger.info("Configured check suite provider for: {}", path);
        } catch (IOException e) {
            logger.error("Failed to configure check suite reader for path: {}", path, e);
        }
    }

    private void removeCheckSuiteConfig(Path path) {
        Configuration configuration = configurations.remove(path);
        if (configuration != null) {
            try {
                configuration.delete();
            } catch (IOException e) {
                logger.error("Failed to delete configuration for path: {}", path, e);
            }
        }
    }

    @Deactivate
    private void deactivate() {
        logger.info("Deactivating CheckSuiteXmiFileListener");
        for (Configuration configuration : configurations.values()) {
            try {
                configuration.delete();
            } catch (IOException e) {
                logger.error("Failed to delete configuration", e);
            }
        }
        configurations.clear();
    }
}
