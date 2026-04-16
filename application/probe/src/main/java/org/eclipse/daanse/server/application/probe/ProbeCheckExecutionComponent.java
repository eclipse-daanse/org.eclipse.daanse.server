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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.check.model.check.CheckExecutionResult;
import org.eclipse.daanse.olap.check.model.check.OlapCheckSuite;
import org.eclipse.daanse.olap.check.reporter.api.CheckResultReporter;
import org.eclipse.daanse.olap.check.runtime.api.CheckExecutor;
import org.eclipse.daanse.olap.check.runtime.api.OlapCheckSuiteSupplier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class ProbeCheckExecutionComponent {

    private static final Logger logger = LoggerFactory.getLogger(ProbeCheckExecutionComponent.class);
    private static final long EXECUTION_DELAY_SECONDS = 5;

    @Reference
    private CheckExecutor checkExecutor;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<CheckResultReporter> reporters;

    private BundleContext bundleContext;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ScheduledFuture<?>> pendingExecutions = new ConcurrentHashMap<>();

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void bindCheckSuiteSupplier(OlapCheckSuiteSupplier supplier, Map<String, Object> props) {
        String matcherKey = (String) props.get(ProbeFileListener.KEY_FILE_CONTEXT_MATCHER);
        if (matcherKey == null) {
            logger.debug("CheckSuiteSupplier without file.context.matcher, skipping");
            return;
        }
        logger.info("CheckSuiteSupplier bound with matcher key: {}, scheduling execution in {} seconds", matcherKey,
                EXECUTION_DELAY_SECONDS);
        scheduleExecution(matcherKey, supplier);
    }

    void unbindCheckSuiteSupplier(OlapCheckSuiteSupplier supplier, Map<String, Object> props) {
        String matcherKey = (String) props.get(ProbeFileListener.KEY_FILE_CONTEXT_MATCHER);
        if (matcherKey != null) {
            ScheduledFuture<?> future = pendingExecutions.remove(matcherKey);
            if (future != null) {
                future.cancel(false);
            }
        }
    }

    private void scheduleExecution(String matcherKey, OlapCheckSuiteSupplier supplier) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                executeChecks(matcherKey, supplier);
            } catch (Exception e) {
                logger.error("Check execution failed for matcher key: {}", matcherKey, e);
            } finally {
                pendingExecutions.remove(matcherKey);
            }
        }, EXECUTION_DELAY_SECONDS, TimeUnit.SECONDS);
        pendingExecutions.put(matcherKey, future);
    }

    @SuppressWarnings("unchecked")
    private void executeChecks(String matcherKey, OlapCheckSuiteSupplier supplier) {
        logger.info("Starting check execution for matcher key: {}", matcherKey);

        Context<?> context = findContextByMatcherKey(matcherKey);
        if (context == null) {
            logger.warn("No Context found for matcher key: {}, retrying in {} seconds", matcherKey,
                    EXECUTION_DELAY_SECONDS);
            scheduleExecution(matcherKey, supplier);
            return;
        }

        OlapCheckSuite suite = supplier.get();
        String suiteName = suite.getName() != null ? suite.getName() : matcherKey;
        List<CheckExecutionResult> results = checkExecutor.execute(suite, context);

        logger.info("Check execution completed for '{}': dispatching to {} reporter(s)", suiteName,
                reporters != null ? reporters.size() : 0);

        if (reporters != null) {
            for (CheckResultReporter reporter : reporters) {
                try {
                    reporter.report(results, suiteName);
                } catch (Exception e) {
                    logger.error("Reporter {} failed for suite '{}'", reporter.getClass().getSimpleName(), suiteName,
                            e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Context<?> findContextByMatcherKey(String matcherKey) {
        try {
            String filter = "(" + ProbeFileListener.KEY_FILE_CONTEXT_MATCHER + "=" + matcherKey + ")";
            Collection<ServiceReference<Context>> refs = (Collection<ServiceReference<Context>>) (Collection<?>) bundleContext
                    .getServiceReferences(Context.class, filter);
            if (refs != null && !refs.isEmpty()) {
                ServiceReference<Context> ref = refs.iterator().next();
                return bundleContext.getService(ref);
            }
        } catch (InvalidSyntaxException e) {
            logger.error("Invalid filter syntax for matcher key: {}", matcherKey, e);
        }
        return null;
    }

    @Deactivate
    public void deactivate() {
        scheduler.shutdownNow();
        pendingExecutions.clear();
    }
}
