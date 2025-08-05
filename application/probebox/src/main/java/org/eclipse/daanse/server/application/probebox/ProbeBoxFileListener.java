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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.server.application.probebox;

import static org.eclipse.daanse.jdbc.datasource.metatype.h2.api.Constants.DATASOURCE_PROPERTY_IDENTIFIER;
import static org.eclipse.daanse.jdbc.datasource.metatype.h2.api.Constants.DATASOURCE_PROPERTY_PLUGABLE_FILESYSTEM;
import static org.eclipse.daanse.jdbc.datasource.metatype.h2.api.Constants.OPTION_PLUGABLE_FILESYSTEM_MEM_FS;
import static org.eclipse.daanse.jdbc.datasource.metatype.h2.api.Constants.PID_DATASOURCE;
import static org.eclipse.daanse.jdbc.db.importer.csv.api.Constants.PID_CSV_DATA_IMPORTER;
import static org.eclipse.daanse.rolap.core.api.Constants.BASIC_CONTEXT_REF_NAME_CATALOG_MAPPING_SUPPLIER;
import static org.eclipse.daanse.rolap.core.api.Constants.BASIC_CONTEXT_REF_NAME_DATA_SOURCE;
import static org.eclipse.daanse.rolap.core.api.Constants.BASIC_CONTEXT_REF_NAME_DIALECT_FACTORY;
import static org.eclipse.daanse.rolap.core.api.Constants.BASIC_CONTEXT_REF_NAME_EXPRESSION_COMPILER_FACTORY;
import static org.eclipse.daanse.rolap.core.api.Constants.BASIC_CONTEXT_REF_NAME_MDX_PARSER_PROVIDER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.daanse.io.fs.watcher.api.FileSystemWatcherListener;
import org.eclipse.daanse.io.fs.watcher.api.FileSystemWatcherWhiteboardConstants;
import org.eclipse.daanse.io.fs.watcher.api.propertytypes.FileSystemWatcherListenerProperties;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = ProbeBoxFileListener.PID, configurationPolicy = ConfigurationPolicy.REQUIRE)
@RequireConfigurationAdmin
@RequireServiceComponentRuntime
@FileSystemWatcherListenerProperties(recursive = false)
public class ProbeBoxFileListener implements FileSystemWatcherListener {

    static final String MATCHER_KEY = "matcherKey";

    private static final Logger logger = LoggerFactory.getLogger(ProbeBoxFileListener.class);

    static final String PID = "org.eclipse.daanse.server.application.probebox.CatalogsFileListener";

    static final String KEY_FILE_CONTEXT_MATCHER = "file.context.matcher";

    public static final String PID_PARSER = "org.eclipse.daanse.mdx.parser.ccc.MdxParserProviderImpl";

    public static final String PID_EXP_COMP_FAC = "org.eclipse.daanse.olap.calc.base.compiler.BaseExpressionCompilerFactory";
    private static final String TARGET_EXT = ".target";

    @Reference
    ConfigurationAdmin ca;

    private Map<Path, Configuration> catalogFolderConfigsDS = new ConcurrentHashMap<>();
    private Map<Path, Configuration> catalogFolderConfigsCSV = new ConcurrentHashMap<>();
    private Map<Path, Configuration> catalogFolderConfigsContext = new ConcurrentHashMap<>();
    private Map<Path, Configuration> catalogFolderConfigsMapping = new ConcurrentHashMap<>();

    @Override
    public void handleBasePath(Path basePath) {
        // not relevant
    }

    @Override
    public void handleInitialPaths(List<Path> paths) {
        paths.forEach(this::addPath);
    }

    @Override
    public void handlePathEvent(Path path, Kind<Path> kind) {

        if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {
            removePath(path);
            addPath(path);
        } else if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
            addPath(path);
        } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
            removePath(path);
        }
    }

    private void removePath(Path path) {
        if (!Files.isDirectory(path)) {
            return;
        }

        try {
            Configuration c = catalogFolderConfigsDS.remove(path);
            c.delete();
        } catch (IOException e) {
            logger.error("Failed to delete DS configuration for path: {}", path, e);
        }

        try {
            Configuration c = catalogFolderConfigsCSV.remove(path);
            c.delete();
        } catch (IOException e) {
            logger.error("Failed to delete CSV configuration for path: {}", path, e);
        }

        try {
            Configuration c = catalogFolderConfigsContext.remove(path);
            c.delete();
        } catch (IOException e) {
            logger.error("Failed to delete context configuration for path: {}", path, e);
        }

        try {
            Configuration c = catalogFolderConfigsMapping.remove(path);
            c.delete();
        } catch (IOException e) {
            logger.error("Failed to delete mapping configuration for path: {}", path, e);
        }
    }

    private void addPath(Path path) {
        if (!Files.isDirectory(path)) {
            return;
        }
        logger.info("Adding catalog path: {}", path);
        String pathString = path.toString();

        String matcherKey = pathString.replace("\\", "-.-");
        matcherKey = UUID.randomUUID().toString();

        try {
            createH2DataSource(path, matcherKey);
            createCsvDatabaseImporter(path, matcherKey);
            createMapping(path, matcherKey);
            createContext(path, matcherKey);

        } catch (IOException e) {
            logger.error("Failed to setup configurations for path: {}", path, e);
        }

    }

    private void createMapping(Path path, String matcherKey) {

        try {
            Configuration configXmiFileListener = ca.getFactoryConfiguration(CatalogXmiFileListener.PID,
                    UUID.randomUUID().toString(), "?");

            Dictionary<String, Object> props = new Hashtable<>();
            String pathMapping = path.resolve("mapping").toAbsolutePath().toString();
            props.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, pathMapping);
            props.put(MATCHER_KEY, matcherKey);

            configXmiFileListener.update(props);

            catalogFolderConfigsMapping.put(path, configXmiFileListener);
        } catch (IOException e) {
            logger.error("Failed to create mapping configuration for path: {}", path, e);
        }
    }

    private void createContext(Path path, String matcherKey) throws IOException {

        Configuration configContext = ca.getFactoryConfiguration(
                org.eclipse.daanse.rolap.core.api.Constants.BASIC_CONTEXT_PID, UUID.randomUUID().toString(), "?");

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(BASIC_CONTEXT_REF_NAME_DATA_SOURCE + TARGET_EXT, filterOfMatcherKey(matcherKey));
        props.put(BASIC_CONTEXT_REF_NAME_EXPRESSION_COMPILER_FACTORY + TARGET_EXT,
                "(component.name=" + PID_EXP_COMP_FAC + ")");
        props.put(BASIC_CONTEXT_REF_NAME_DIALECT_FACTORY + TARGET_EXT, "(org.eclipse.daanse.dialect.name=H2)");
        props.put(BASIC_CONTEXT_REF_NAME_CATALOG_MAPPING_SUPPLIER + TARGET_EXT, filterOfMatcherKey(matcherKey));
        props.put(BASIC_CONTEXT_REF_NAME_MDX_PARSER_PROVIDER + TARGET_EXT, "(component.name=" + PID_PARSER + ")");

        String catalog_path = path.toString();
        String theDescription = "theDescription for " + catalog_path;

        String name = path.toString();
        if (name == null || name.isEmpty()) {
            name = "not_set" + UUID.randomUUID().toString();
        }
        props.put("name", name);
        props.put("description", theDescription);
        props.put("catalog.path", catalog_path);
        props.put("useAggregates", true);
        configContext.update(props);

        catalogFolderConfigsContext.put(path, configContext);

    }

    private void createCsvDatabaseImporter(Path path, String matcherKey) throws IOException {
        String pathStringData = path.resolve("data").toString();
        Configuration configCsv = ca.getFactoryConfiguration(PID_CSV_DATA_IMPORTER, UUID.randomUUID().toString(), "?");

        Dictionary<String, Object> propsCSV = new Hashtable<>();
        propsCSV.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, pathStringData);
        propsCSV.put("dataSource.target", filterOfMatcherKey(matcherKey));
        configCsv.update(propsCSV);

        catalogFolderConfigsCSV.put(path, configCsv);
    }

    private void createH2DataSource(Path path, String matcherKey) throws IOException {
        Configuration configH2 = ca.getFactoryConfiguration(PID_DATASOURCE, UUID.randomUUID().toString(), "?");

        Dictionary<String, Object> propsH2 = new Hashtable<>();

        propsH2.put(DATASOURCE_PROPERTY_IDENTIFIER, UUID.randomUUID().toString());
        propsH2.put(DATASOURCE_PROPERTY_PLUGABLE_FILESYSTEM, OPTION_PLUGABLE_FILESYSTEM_MEM_FS);
        propsH2.put(KEY_FILE_CONTEXT_MATCHER, matcherKey);
        configH2.update(propsH2);

        catalogFolderConfigsDS.put(path, configH2);
    }

    private static final String filterOfMatcherKey(String matcherKey) {
        return "(" + KEY_FILE_CONTEXT_MATCHER + "=" + matcherKey + ")";
    }

}
