/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.cli;

import com.google.common.base.Joiner;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.nuun.kernel.api.Kernel;
import io.nuun.kernel.api.config.KernelConfiguration;
import org.apache.commons.cli.*;
import org.seedstack.seed.Application;
import org.seedstack.seed.SeedException;
import org.seedstack.seed.cli.internal.CliErrorCode;
import org.seedstack.seed.cli.spi.CliContext;
import org.seedstack.seed.core.Seed;
import org.seedstack.seed.core.SeedMain;
import org.seedstack.seed.spi.SeedLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This class executes {@link CommandLineHandler}s found in the classpath. The main method of this class is deprecated
 * and {@link SeedMain#main(String[])} should be used instead.
 *
 * @author epo.jemba@ext.mpsa.com
 * @author adrien.lauer@mpsa.com
 */
public class CliLauncher implements SeedLauncher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CliLauncher.class);

    @Override
    public void launch(String[] args) throws Exception {
        int returnCode = execute(args);
        LOGGER.info("CLI command finished with return code {}", returnCode);
        System.exit(returnCode);
    }

    @Override
    public void shutdown() throws Exception {
        // nothing to do here
    }

    /**
     * Execute a Seed CLI command (implemented by a {@link CommandLineHandler}.
     *
     * @param args the command line arguments. First argument is the name of the CLI command. Subsequent arguments are
     *             passed to the CLI command.
     * @return the return code of the CLI command.
     * @throws Exception when the CLI command fails to complete.
     */
    public static int execute(String[] args) throws Exception {
        Kernel kernel = Seed.createKernel(new CliContext(args), null, true);

        try {
            Injector injector = kernel.objectGraph().as(Injector.class);
            CliConfig configuration = injector.getInstance(Application.class).getConfiguration().get(CliConfig.class);
            Callable<Integer> callable;

            if (configuration.hasDefaultCommand()) {
                LOGGER.debug("Executing default CLI command " + configuration.getDefaultCommand());
                callable = new SeedCallable(configuration.getDefaultCommand(), args);
            } else {
                if (args == null || args.length == 0 || args[0].isEmpty()) {
                    throw SeedException.createNew(CliErrorCode.NO_COMMAND_SPECIFIED);
                }

                // A command must be provided as first argument, it is extracted from the command line
                String[] effectiveArgs = new String[args.length - 1];
                System.arraycopy(args, 1, effectiveArgs, 0, effectiveArgs.length);

                callable = new SeedCallable(args[0], effectiveArgs);
            }

            injector.injectMembers(callable);

            return callable.call();
        } finally {
            Seed.disposeKernel(kernel);
        }
    }

    /**
     * Method to execute a callable as a CLI application.
     *
     * @param args     the command line arguments.
     * @param callable the callable to execute
     * @return the return code of the callable
     * @throws Exception when the CLI command fails to complete.
     */
    public static int execute(String[] args, Callable<Integer> callable) throws Exception {
        Kernel kernel = Seed.createKernel(new CliContext(args), null, true);

        try {
            kernel.objectGraph().as(Injector.class).injectMembers(callable);
            return callable.call();
        } finally {
            Seed.disposeKernel(kernel);
        }
    }

    /**
     * Method to execute a callable as a CLI application.
     *
     * @param args                the command line arguments.
     * @param callable            the callable to execute
     * @param kernelConfiguration a kernel configuration to use for the CLI application.
     * @return the return code of the callable
     * @throws Exception when the CLI command fails to complete.
     */
    public static int execute(String[] args, Callable<Integer> callable, KernelConfiguration kernelConfiguration) throws Exception {
        Kernel kernel = Seed.createKernel(new CliContext(args), kernelConfiguration, true);

        try {
            kernel.objectGraph().as(Injector.class).injectMembers(callable);
            return callable.call();
        } finally {
            Seed.disposeKernel(kernel);
        }
    }

    public static class SeedCallable implements Callable<Integer> {
        private final String cliCommand;
        private final String[] args;

        @Inject
        private Injector injector;

        public SeedCallable(String cliCommand, String[] args) {
            this.cliCommand = cliCommand;
            this.args = args;
        }

        @Override
        public Integer call() throws Exception {
            try {
                CommandLineHandler commandLineHandler = injector.getInstance(Key.get(CommandLineHandler.class, Names.named(cliCommand)));
                injectCommandLineHandler(commandLineHandler);

                LOGGER.info("Executing CLI command {}, handled by {}", cliCommand, commandLineHandler.getClass().getCanonicalName());

                return commandLineHandler.call();
            } catch (ConfigurationException e) {
                throw SeedException.wrap(e, CliErrorCode.COMMAND_LINE_HANDLER_NOT_FOUND).put("commandLineHandler", cliCommand);
            }
        }

        private void injectCommandLineHandler(CommandLineHandler commandLineHandler) {
            Options options = new Options();
            List<CliOption> optionAnnotations = new ArrayList<>();
            List<Field> optionFields = new ArrayList<>();
            Field argsField = null;
            int mandatoryArgsCount = 0;

            // Compute injection info
            for (Field field : commandLineHandler.getClass().getDeclaredFields()) {
                CliOption optionAnnotation = field.getAnnotation(CliOption.class);
                CliArgs argsAnnotation = field.getAnnotation(CliArgs.class);

                if (optionAnnotation != null) {
                    Option option = new Option(
                            optionAnnotation.name(),
                            optionAnnotation.longName(),
                            optionAnnotation.valueCount() > 0 || optionAnnotation.valueCount() == -1,
                            optionAnnotation.description()
                    );

                    if (optionAnnotation.valueCount() == -1) {
                        option.setArgs(Option.UNLIMITED_VALUES);
                    } else if (optionAnnotation.valueCount() > 0) {
                        option.setArgs(optionAnnotation.valueCount());
                    }

                    option.setValueSeparator(optionAnnotation.valueSeparator());
                    option.setRequired(optionAnnotation.mandatory());
                    option.setOptionalArg(!optionAnnotation.mandatoryValue());
                    optionAnnotations.add(optionAnnotation);
                    optionFields.add(field);
                    options.addOption(option);
                } else if (argsAnnotation != null) {
                    mandatoryArgsCount = argsAnnotation.mandatoryCount();
                    argsField = field;
                }
            }

            // Parse command line
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine;
            try {
                commandLine = parser.parse(options, args);
            } catch (ParseException e) {
                throw SeedException.wrap(e, CliErrorCode.ERROR_PARSING_COMMAND_LINE)
                        .put("command", cliCommand)
                        .put("commandLine", Joiner.on(' ').join(args));
            }

            // Inject options
            for (int i = 0; i < optionAnnotations.size(); i++) {
                CliOption cliOption = optionAnnotations.get(i);
                Field field = optionFields.get(i);
                String[] value = null;

                if (cliOption.valueCount() > 0 || cliOption.valueCount() == -1) {
                    if (commandLine.hasOption(cliOption.name())) {
                        value = commandLine.getOptionValues(cliOption.name());
                    }

                    if (value == null && cliOption.defaultValues().length > 0) {
                        value = cliOption.defaultValues();
                    }

                    if (value != null) {
                        if (cliOption.valueCount() != -1 && cliOption.valueCount() != value.length) {
                            throw SeedException.createNew(CliErrorCode.WRONG_NUMBER_OF_OPTION_ARGUMENTS).put("command", cliCommand);
                        }

                        try {
                            Class<?> fieldType = field.getType();

                            field.setAccessible(true);

                            if (String.class.isAssignableFrom(fieldType)) {
                                field.set(commandLineHandler, value[0]);
                            } else if (fieldType.isArray() && String.class.isAssignableFrom(fieldType.getComponentType())) {
                                field.set(commandLineHandler, value);
                            } else if (Map.class.isAssignableFrom(fieldType)) {
                                field.set(commandLineHandler, buildOptionArgumentMap(cliOption.name(), value));
                            } else {
                                throw SeedException.createNew(CliErrorCode.UNSUPPORTED_OPTION_FIELD_TYPE).put("command", cliCommand).put("fieldType", fieldType.getCanonicalName());
                            }
                        } catch (IllegalAccessException e) {
                            throw SeedException.wrap(e, CliErrorCode.UNABLE_TO_INJECT_OPTION).put("command", cliCommand).put("option", cliOption.name());
                        }
                    }
                } else {
                    try {
                        field.setAccessible(true);
                        field.set(commandLineHandler, commandLine.hasOption(cliOption.name()));
                    } catch (IllegalAccessException e) {
                        throw SeedException.wrap(e, CliErrorCode.UNABLE_TO_INJECT_OPTION).put("command", cliCommand).put("option", cliOption.name());
                    }
                }
            }

            // Inject args
            if (argsField != null) {
                if (commandLine.getArgs().length < mandatoryArgsCount) {
                    throw SeedException.createNew(CliErrorCode.MISSING_ARGUMENTS).put("command", cliCommand).put("required", mandatoryArgsCount).put("given", commandLine.getArgs().length);
                } else {
                    argsField.setAccessible(true);
                    try {
                        argsField.set(commandLineHandler, commandLine.getArgs());
                    } catch (IllegalAccessException e) {
                        throw SeedException.createNew(CliErrorCode.UNABLE_TO_INJECT_ARGUMENTS).put("command", cliCommand);
                    }
                }
            }
        }

        private Map<String, String> buildOptionArgumentMap(String optionName, String[] optionArguments) {
            Map<String, String> optionArgumentsMap = new HashMap<>();

            if (optionArguments.length % 2 == 0) {
                for (int i = 0; i < optionArguments.length; i = i + 2) {
                    optionArgumentsMap.put(optionArguments[i], optionArguments[i + 1]);
                }
            } else {
                throw SeedException.createNew(CliErrorCode.ODD_NUMBER_OF_OPTION_ARGUMENTS).put("command", cliCommand).put("option", optionName);
            }

            return optionArgumentsMap;
        }
    }
}