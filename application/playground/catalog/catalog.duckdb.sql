/*********************************************************************
* Copyright (c) 2026 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/

-- Session init SQL of the playground's in-memory DuckDB database. Without a
-- DUCKDB_CONNECTION_INIT_BELOW_MARKER comment the whole file runs once per
-- database instance, i.e. once per start of the DataSource.
CREATE TABLE fact ("key" VARCHAR, "value" INTEGER);
INSERT INTO fact VALUES ('A', 42), ('B', 8);
