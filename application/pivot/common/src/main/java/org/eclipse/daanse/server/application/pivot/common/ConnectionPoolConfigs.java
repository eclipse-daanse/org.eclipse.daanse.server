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

import org.eclipse.daanse.jdbc.datasource.pools.api.Constants;
import org.eclipse.daanse.jdbc.datasource.pools.api.ocd.BaseConfig;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Creates the connection pool the context draws from. A single MDX query fans
 * out to as many statements as the segment cache has threads, so the engine
 * talks to a pool and never to the DataSource directly.
 * <p>
 * The pool attributes are configurable via {@code DAANSE_POOL_*}; unset
 * variables keep the defaults of the pool bundle. Which DataSource is pooled is
 * not configurable - it is the one this image builds from {@code DAANSE_JDBC_*}.
 */
public final class ConnectionPoolConfigs {

    private ConnectionPoolConfigs() {
    }

    public static Configuration createEnvPool(ConfigurationAdmin ca) throws IOException {
        return createEnvPool(ca, null);
    }

    /**
     * As {@link #createEnvPool(ConfigurationAdmin)}, with a default for the
     * pool's read-only mode. The pool marks its connections read-only or
     * read-write; a DataSource whose connections are fixed to one mode (e.g. a
     * read-only DuckDB file) rejects the opposite marking, so its configurator
     * passes the matching mode here. {@code DAANSE_POOL_READ_ONLY} still wins.
     */
    public static Configuration createEnvPool(ConfigurationAdmin ca, Boolean readOnlyDefault) throws IOException {
        Configuration configuration = ca.getFactoryConfiguration(
                org.eclipse.daanse.jdbc.datasource.pools.hikari.api.Constants.PID_CONNECTION_POOL,
                ServerConstants.CONFIG_IDENT, "?");

        Dictionary<String, Object> props = EnvConfigMapper.propsFromEnv(BaseConfig.class,
                ServerConstants.ENV_POOL_PREFIX);
        if (readOnlyDefault != null && props.get(Constants.POOL_PROPERTY_READ_ONLY) == null) {
            props.put(Constants.POOL_PROPERTY_READ_ONLY, readOnlyDefault);
        }
        props.put(ServerConstants.PROP_IDENT, ServerConstants.IDENT_POOL);
        props.put(Constants.POOL_PROPERTY_DATASOURCE_TARGET,
                "(" + ServerConstants.PROP_IDENT + "=" + ServerConstants.IDENT_DATASOURCE + ")");

        configuration.update(props);
        return configuration;
    }
}
