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
package org.eclipse.daanse.server.application.pivot.h2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.eclipse.daanse.server.application.pivot.common.test.PivotContainers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * End-to-end test of the LDAP backed basic authentication: an LLDAP side
 * container is seeded by LLDAP's official bootstrap script from the JSON files
 * shipped in {@code common/example/ldap/bootstrap}, exactly as the container
 * README describes, and the pivot image authenticates against it. Verifies the
 * documented behaviour: valid credentials answer, wrong credentials are
 * rejected, anonymous requests are served by default and rejected once
 * {@code DAANSE_AUTH_ANONYMOUS=false}.
 */
@Testcontainers(disabledWithoutDocker = true)
class LdapAuthPivotIT {

    private static final Path BOOTSTRAP_DIR = Path.of("../common/example/ldap/bootstrap");

    @TempDir
    static Path tempDir;

    @Test
    void basicAuthAgainstLdapSideContainer() throws Exception {
        Path databaseBase = tempDir.resolve("database");
        try (Connection connection = DriverManager.getConnection("jdbc:h2:file:" + databaseBase);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE \"Fact\" (\"KEY\" VARCHAR(100), \"VALUE\" INTEGER)");
            statement.executeUpdate("INSERT INTO \"Fact\" VALUES ('A', 1), ('B', 2), ('C', 3)");
        }
        Path databaseFile = tempDir.resolve("database.mv.db");

        try (Network network = Network.newNetwork();
                GenericContainer<?> lldap = new GenericContainer<>("lldap/lldap:stable")
                        .withNetwork(network).withNetworkAliases("lldap")
                        .withEnv("LLDAP_LDAP_BASE_DN", "dc=example,dc=org")
                        .withEnv("LLDAP_LDAP_USER_PASS", "admin123")
                        .withEnv("LLDAP_KEY_SEED", "integration-test")
                        .withEnv("LLDAP_JWT_SECRET", "integration-test-jwt-secret-0123456789")
                        .withCopyFileToContainer(MountableFile.forHostPath(BOOTSTRAP_DIR), "/bootstrap")
                        .withExposedPorts(3890, 17170)
                        .waitingFor(Wait.forListeningPorts(3890, 17170))) {
            lldap.start();

            // LLDAP's own bootstrap script loads the example users and groups.
            ExecResult bootstrap = lldap.execInContainer("/bin/sh", "-c",
                    "LLDAP_URL=http://localhost:17170 LLDAP_ADMIN_USERNAME=admin "
                            + "LLDAP_ADMIN_PASSWORD=admin123 DO_CLEANUP=false /app/bootstrap.sh");
            assertEquals(0, bootstrap.getExitCode(), bootstrap.getStdout() + bootstrap.getStderr());

            try (GenericContainer<?> pivot = pivotWithLdap(databaseFile, network)) {
                pivot.start();
                String host = pivot.getHost();
                int port = pivot.getMappedPort(8080);

                // Polling with credentials warms up the OLAP context.
                String authenticated = PivotContainers.awaitMdxCell(host, port,
                        PivotContainers.TEST_CATALOG, PivotContainers.TEST_MDX, "6", "admin:admin123");
                assertTrue(authenticated.contains("6</Value>"), authenticated);

                // analyst from the bootstrap user configs works as well
                HttpResponse<String> analyst = PivotContainers.executeMdx(host, port,
                        PivotContainers.TEST_CATALOG, PivotContainers.TEST_MDX, "analyst:analyst123");
                assertEquals(200, analyst.statusCode(), analyst.body());

                // anonymous is accepted by default
                HttpResponse<String> anonymous = PivotContainers.executeMdx(host, port,
                        PivotContainers.TEST_CATALOG, PivotContainers.TEST_MDX, null);
                assertEquals(200, anonymous.statusCode(), anonymous.body());

                HttpResponse<String> wrongPassword = PivotContainers.executeMdx(host, port,
                        PivotContainers.TEST_CATALOG, PivotContainers.TEST_MDX, "admin:wrong");
                assertEquals(401, wrongPassword.statusCode(), wrongPassword.body());

                // the role provider read the groups without complaining
                assertFalse(pivot.getLogs().contains("could not read the groups"), pivot.getLogs());
            }

            try (GenericContainer<?> pivot = pivotWithLdap(databaseFile, network)
                    .withEnv("DAANSE_AUTH_ANONYMOUS", "false")) {
                pivot.start();
                String host = pivot.getHost();
                int port = pivot.getMappedPort(8080);

                PivotContainers.awaitMdxCell(host, port, PivotContainers.TEST_CATALOG,
                        PivotContainers.TEST_MDX, "6", "admin:admin123");

                HttpResponse<String> anonymous = PivotContainers.executeMdx(host, port,
                        PivotContainers.TEST_CATALOG, PivotContainers.TEST_MDX, null);
                assertEquals(401, anonymous.statusCode(), anonymous.body());
            }
        }
    }

    private GenericContainer<?> pivotWithLdap(Path databaseFile, Network network) {
        return new GenericContainer<>(PivotContainers.pivotImage("h2"))
                .withNetwork(network)
                .withCopyFileToContainer(MountableFile.forClasspathResource("catalog.xmi"),
                        "/app/catalog/catalog.xmi")
                .withCopyFileToContainer(MountableFile.forHostPath(databaseFile), "/app/data/database.mv.db")
                .withEnv("DAANSE_LDAP_URL", "ldap://lldap:3890")
                .withEnv("DAANSE_LDAP_TRANSPORT_SECURITY", "NONE")
                .withEnv("DAANSE_LDAP_ALLOW_UNENCRYPTED", "true")
                .withEnv("DAANSE_LDAP_USER_DN_PATTERN", "uid={0},ou=people,dc=example,dc=org")
                .withEnv("DAANSE_LDAP_SERVICE_BIND_DN", "uid=admin,ou=people,dc=example,dc=org")
                .withEnv("DAANSE_LDAP_SERVICE_BIND_PASSWORD", "admin123")
                .withEnv("DAANSE_LDAP_GROUP_SEARCH_BASE", "ou=groups,dc=example,dc=org")
                .withExposedPorts(8080)
                .waitingFor(Wait.forListeningPort());
    }
}
