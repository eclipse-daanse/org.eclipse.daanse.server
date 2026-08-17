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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Access to the environment variables that configure the pivot server. Blank
 * values are treated as unset.
 */
public final class Env {

    private Env() {
    }

    public static Optional<String> get(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    public static String get(String name, String defaultValue) {
        return get(name).orElse(defaultValue);
    }

    public static boolean get(String name, boolean defaultValue) {
        return get(name).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    /** Splits a comma separated environment variable value into its entries. */
    public static String[] splitList(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    /** The given environment variables that are not set. */
    public static List<String> missing(String... names) {
        return Arrays.stream(names).filter(name -> get(name).isEmpty()).toList();
    }
}
