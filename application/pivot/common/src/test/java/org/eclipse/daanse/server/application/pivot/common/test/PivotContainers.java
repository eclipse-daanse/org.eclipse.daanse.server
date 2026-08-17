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
package org.eclipse.daanse.server.application.pivot.common.test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Support for the per-database pivot integration tests: provides the real
 * container image of a database module and queries its XMLA endpoint with MDX.
 */
public final class PivotContainers {

    /**
     * System property naming a pre-built pivot image to verify. When set (e.g.
     * {@code -Dpivot.image=daanse-pivot-postgres:ci}) the tests run against
     * exactly that image instead of building one — this is how CI verifies the
     * image it later pushes.
     */
    public static final String IMAGE_PROPERTY = "pivot.image";

    /** Catalog name of the static test mapping (src/test/resources/catalog.xmi). */
    public static final String TEST_CATALOG = "Daanse Tutorial - Cube Minimal";
    /** MDX query summing the single measure of the static test mapping. */
    public static final String TEST_MDX = "SELECT [Measures].[Measure-Sum] ON COLUMNS FROM [MinimalCube]";

    private PivotContainers() {
    }

    /**
     * The pivot image of the given database module: the pre-built image named
     * by the {@value #IMAGE_PROPERTY} system property if set, otherwise built
     * from the module's real Dockerfile (runs with the module directory as
     * working directory and provides the files the Dockerfile copies as build
     * context).
     */
    public static Future<String> pivotImage(String db) {
        String preBuilt = System.getProperty(IMAGE_PROPERTY);
        if (preBuilt != null && !preBuilt.isBlank()) {
            return CompletableFuture.completedFuture(preBuilt.trim());
        }
        String prefix = "application/pivot/" + db + "/";
        return new ImageFromDockerfile()
                .withFileFromPath("Dockerfile", Path.of("container/Dockerfile"))
                .withFileFromPath(prefix + "target/daanse.pivot." + db + ".jar",
                        Path.of("target/daanse.pivot." + db + ".jar"))
                .withFileFromPath(prefix + "start", Path.of("start"))
                .withFileFromPath(prefix + "logback.xml", Path.of("logback.xml"));
    }

    /** Sends an XMLA Execute request with the given MDX statement. */
    public static String executeMdx(String host, int port, String catalog, String mdx)
            throws IOException, InterruptedException {
        return executeMdx(host, port, catalog, mdx, null).body();
    }

    /**
     * As {@link #executeMdx(String, int, String, String)}, optionally with HTTP
     * Basic credentials ({@code user:password}), returning the full response so
     * tests can assert the status code (e.g. 401).
     */
    public static HttpResponse<String> executeMdx(String host, int port, String catalog, String mdx,
            String basicCredentials) throws IOException, InterruptedException {
        String envelope = """
                <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
                  <SOAP-ENV:Header/>
                  <SOAP-ENV:Body>
                    <Execute xmlns="urn:schemas-microsoft-com:xml-analysis">
                      <Command>
                        <Statement>%s</Statement>
                      </Command>
                      <Properties>
                        <PropertyList>
                          <Catalog>%s</Catalog>
                          <Format>Multidimensional</Format>
                          <AxisFormat>TupleFormat</AxisFormat>
                        </PropertyList>
                      </Properties>
                    </Execute>
                  </SOAP-ENV:Body>
                </SOAP-ENV:Envelope>
                """.formatted(mdx, catalog);

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://" + host + ":" + port + "/xmla"))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"urn:schemas-microsoft-com:xml-analysis:Execute\"")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(envelope));
        if (basicCredentials != null) {
            request.header("Authorization", "Basic "
                    + Base64.getEncoder().encodeToString(basicCredentials.getBytes(StandardCharsets.UTF_8)));
        }

        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    /**
     * Polls the XMLA endpoint until the MDX query returns a cell with the
     * expected value — the OLAP context needs a moment to connect to the
     * database after container start.
     */
    public static String awaitMdxCell(String host, int port, String catalog, String mdx, String expectedValue)
            throws InterruptedException {
        return awaitMdxCell(host, port, catalog, mdx, expectedValue, null);
    }

    /** As {@link #awaitMdxCell(String, int, String, String, String)}, with HTTP Basic credentials. */
    public static String awaitMdxCell(String host, int port, String catalog, String mdx, String expectedValue,
            String basicCredentials) throws InterruptedException {
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(3).toMillis();
        String last = "";
        while (System.currentTimeMillis() < deadline) {
            try {
                last = executeMdx(host, port, catalog, mdx, basicCredentials).body();
                if (last.contains(">" + expectedValue + "</Value>")
                        || last.contains(expectedValue + "</Value>")) {
                    return last;
                }
            } catch (IOException e) {
                last = e.toString();
            }
            Thread.sleep(2_000);
        }
        throw new AssertionError(
                "MDX did not return expected cell value " + expectedValue + "; last response:\n" + last);
    }
}
