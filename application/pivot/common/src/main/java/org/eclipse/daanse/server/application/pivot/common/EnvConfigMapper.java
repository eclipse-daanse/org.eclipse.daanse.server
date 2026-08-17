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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

/**
 * Maps environment variables onto OSGi configuration properties, driven by the
 * OCD config interface of a Daanse DataSource bundle. Every attribute of the
 * interface is configurable via {@code DAANSE_JDBC_<UPPER_SNAKE>}; attributes
 * whose variable is not set keep the default declared by the bundle.
 */
public final class EnvConfigMapper {

    private EnvConfigMapper() {
    }

    /**
     * Configuration properties for all attributes of the given config interface
     * whose {@code DAANSE_JDBC_*} variable is set.
     */
    public static Dictionary<String, Object> propsFromEnv(Class<?> configInterface) {
        return propsFromEnv(configInterface, ServerConstants.ENV_JDBC_PREFIX);
    }

    /** As {@link #propsFromEnv(Class)}, under a different variable prefix. */
    public static Dictionary<String, Object> propsFromEnv(Class<?> configInterface, String envPrefix) {
        Dictionary<String, Object> props = new Hashtable<>();
        for (Method method : attributeMethods(configInterface)) {
            String envName = envName(method.getName(), envPrefix);
            Env.get(envName).ifPresent(
                    value -> props.put(propertyName(method.getName()), convert(value, method.getReturnType(), envName)));
        }
        return props;
    }

    /** Environment variable names supported by the given config interface. */
    public static List<String> envNames(Class<?> configInterface) {
        return envNames(configInterface, ServerConstants.ENV_JDBC_PREFIX);
    }

    /** As {@link #envNames(Class)}, under a different variable prefix. */
    public static List<String> envNames(Class<?> configInterface, String envPrefix) {
        return attributeMethods(configInterface).stream().map(method -> envName(method.getName(), envPrefix)).sorted()
                .toList();
    }

    private static List<Method> attributeMethods(Class<?> configInterface) {
        List<Method> methods = new ArrayList<>();
        for (Method method : configInterface.getMethods()) {
            if (method.getParameterCount() == 0 && !method.isSynthetic() && !Modifier.isStatic(method.getModifiers())
                    && isSupported(method.getReturnType())) {
                methods.add(method);
            }
        }
        return methods;
    }

    private static boolean isSupported(Class<?> type) {
        return type == String.class || type == String[].class || type == boolean.class || type == Boolean.class
                || type == int.class || type == Integer.class || type == long.class || type == Long.class
                || type == short.class || type == Short.class || type == int[].class;
    }

    /**
     * Configuration property name of a config interface method, following the
     * OSGi component property mapping: {@code $} is removed, {@code $$} maps to
     * {@code $}, {@code _} maps to {@code .} and {@code __} maps to {@code _},
     * e.g. {@code _password()} configures {@code .password}.
     */
    public static String propertyName(String methodName) {
        StringBuilder sb = new StringBuilder(methodName.length());
        for (int i = 0; i < methodName.length(); i++) {
            char c = methodName.charAt(i);
            if (c == '$') {
                if (i + 1 < methodName.length() && methodName.charAt(i + 1) == '$') {
                    sb.append('$');
                    i++;
                }
            } else if (c == '_') {
                if (i + 1 < methodName.length() && methodName.charAt(i + 1) == '_') {
                    sb.append('_');
                    i++;
                } else {
                    sb.append('.');
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Environment variable name of a config interface method, e.g.
     * {@code currentSchema()} is set via {@code DAANSE_JDBC_CURRENT_SCHEMA} and
     * {@code _password()} via {@code DAANSE_JDBC_PASSWORD}.
     */
    public static String envName(String methodName) {
        return envName(methodName, ServerConstants.ENV_JDBC_PREFIX);
    }

    /** As {@link #envName(String)}, under a different variable prefix. */
    public static String envName(String methodName, String envPrefix) {
        String name = methodName.replace("$", "");
        while (name.startsWith("_")) {
            name = name.substring(1);
        }
        name = name.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
        name = name.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2");
        return envPrefix + name.replace("__", "_").toUpperCase(Locale.ROOT);
    }

    private static Object convert(String value, Class<?> type, String envName) {
        try {
            if (type == String.class) {
                return value;
            }
            if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            }
            if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            }
            if (type == short.class || type == Short.class) {
                return Short.parseShort(value);
            }
            if (type == String[].class) {
                return Env.splitList(value);
            }
            if (type == int[].class) {
                String[] parts = Env.splitList(value);
                int[] ints = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    ints[i] = Integer.parseInt(parts[i]);
                }
                return ints;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Environment variable " + envName + " has invalid value '" + value + "'", e);
        }
        throw new IllegalArgumentException("Unsupported attribute type " + type + " for " + envName);
    }
}
