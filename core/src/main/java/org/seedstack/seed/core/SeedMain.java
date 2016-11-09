/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.core;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.seedstack.seed.SeedException;
import org.seedstack.seed.core.internal.CoreErrorCode;
import org.seedstack.seed.core.internal.ToolLauncher;
import org.seedstack.seed.spi.SeedLauncher;

import java.util.List;
import java.util.ServiceLoader;

/**
 * <p>
 * Main Seed Java application entry point. It searches classes implementing {@link SeedLauncher} through the
 * {@link ServiceLoader} mechanism. If no class or more than one class is found, it throws an exception. If exactly one
 * class is found, it delegates the Seed application startup to its {@link SeedLauncher#launch(String[])} method.
 * </p>
 * <p>
 * High-level exception handling and diagnostic is done directly in this class.
 * </p>
 */
public class SeedMain {
    /**
     * Entry point of Seed standalone applications (launched from the command-line).
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        String toolName = System.getProperty("tool");
        SeedLauncher seedLauncher;

        if (!Strings.isNullOrEmpty(toolName)) {
            seedLauncher = getToolLauncher(toolName);
        } else {
            seedLauncher = getLauncher();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    seedLauncher.shutdown();
                    Seed.close();
                } catch (Exception e) {
                    handleException(e);
                }
            }
        });

        try {
            seedLauncher.launch(args);
        } catch (Exception e) {
            handleException(e);
            System.exit(-1);
        }
    }

    /**
     * Discover implementations of {@link SeedLauncher} through the {@link ServiceLoader} mechanism and if exactly one
     * implementation is available, returns it. Otherwise, throws an exception.
     *
     * @return an instance of the unique {@link SeedLauncher} implementation.
     */
    public static SeedLauncher getLauncher() {
        List<SeedLauncher> entryPointServices = Lists.newArrayList(ServiceLoader.load(SeedLauncher.class));

        if (entryPointServices.size() < 1) {
            throw SeedException.createNew(CoreErrorCode.MISSING_SEED_LAUNCHER);
        } else if (entryPointServices.size() > 1) {
            throw SeedException.createNew(CoreErrorCode.MULTIPLE_SEED_LAUNCHERS);
        }

        return entryPointServices.get(0);
    }

    /**
     * Returns an instance of the {@link ToolLauncher} configured for the specified tool.
     *
     * @param toolName the tool to execute.
     * @return the {@link ToolLauncher} instance.
     */
    public static SeedLauncher getToolLauncher(String toolName) {
        return new ToolLauncher(toolName);
    }

    private static void handleException(Exception e) {
        Seed.diagnostic().dumpDiagnosticReport(e);
        if (e instanceof SeedException) {
            e.printStackTrace(System.err);
        } else {
            SeedException.wrap(e, CoreErrorCode.UNEXPECTED_EXCEPTION).printStackTrace(System.err);
        }
    }
}
