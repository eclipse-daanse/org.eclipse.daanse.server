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
package org.eclipse.daanse.server.application.pivot.mssql;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;

import org.eclipse.daanse.jdbc.datasource.mssqlserver.api.Constants;
import org.eclipse.daanse.jdbc.datasource.mssqlserver.api.ocd.DsConfig;
import org.eclipse.daanse.server.application.pivot.common.BasicContextConfigs;
import org.eclipse.daanse.server.application.pivot.common.Env;
import org.eclipse.daanse.server.application.pivot.common.EnvConfigMapper;
import org.eclipse.daanse.server.application.pivot.common.ServerConstants;
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
 * Registers the MS SQL Server DataSource from the {@code DAANSE_JDBC_*}
 * environment variables and creates the BasicContext against it. Every
 * attribute of {@link DsConfig} is supported.
 */
@Component(immediate = true)
@RequireConfigurationAdmin
@RequireServiceComponentRuntime
public class MssqlEnvConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(MssqlEnvConfigurator.class);

    private static final String DIALECT_NAME = "MSSQL";

    private static final String ENV_USER = "DAANSE_JDBC_USER";
    private static final String ENV_PASSWORD = "DAANSE_JDBC_PASSWORD";
    private static final String ENV_DATABASE_NAME = "DAANSE_JDBC_DATABASE_NAME";

    @Reference
    ConfigurationAdmin ca;

    private Configuration confDataSource;
    private List<Configuration> confPoolAndContext = List.of();

    @Activate
    public void activate() throws IOException {
        List<String> missing = Env.missing(ENV_USER, ENV_PASSWORD, ENV_DATABASE_NAME);
        if (!missing.isEmpty()) {
            logger.error(
                    "Not configuring the MS SQL Server DataSource, required environment variables are not set: {}",
                    missing);
            return;
        }

        Dictionary<String, Object> props = EnvConfigMapper.propsFromEnv(DsConfig.class);
        props.put(ServerConstants.PROP_IDENT, ServerConstants.IDENT_DATASOURCE);

        confDataSource = ca.getFactoryConfiguration(Constants.PID_DATASOURCE, ServerConstants.CONFIG_IDENT, "?");
        confDataSource.update(props);

        confPoolAndContext = BasicContextConfigs.createEnvPoolAndContext(ca, DIALECT_NAME);

        logger.info("MS SQL Server DataSource, connection pool and context configured from environment");
    }

    @Deactivate
    public void deactivate() throws IOException {
        if (confDataSource != null) {
            confDataSource.delete();
        }
        for (Configuration configuration : confPoolAndContext) {
            configuration.delete();
        }
        confPoolAndContext = List.of();
    }
}
