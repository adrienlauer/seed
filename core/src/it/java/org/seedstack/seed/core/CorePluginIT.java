/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.core;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.seedstack.seed.Logging;
import org.seedstack.seed.core.fixtures.Service1;
import org.seedstack.seed.core.fixtures.Service2;
import org.seedstack.seed.core.fixtures.Service3;
import org.seedstack.seed.core.fixtures.TestSeedInitializer;
import org.seedstack.seed.core.rules.SeedITRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import some.other.pkg.ForeignClass;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

public class CorePluginIT {
    @Rule
    public SeedITRule rule = new SeedITRule(this);
    private Injector injector;

    static class LoggerHolder {
        private static final Logger logger = LoggerFactory.getLogger(LoggerHolder.class);

        @Logging
        protected Logger logger1;
    }

    private static class SubLoggerHolder1 extends LoggerHolder {
        @Logging
        private Logger logger2;
    }

    private static class SubLoggerHolder2 extends LoggerHolder {
        @Logging
        private Logger logger2;
    }

    private static class HolderNominal {
        @Inject
        Service1 s1;
        @Inject
        @Nullable
        Service3 s3;
        @Inject
        ForeignClass foreignClass;
    }

    private static class HolderException {
        @Inject
        Service2 s2;
    }

    @Before
    public void before() {
        injector = rule.getKernel().objectGraph().as(Injector.class).createChildInjector(
                binder -> {
                    binder.bind(HolderNominal.class);
                    binder.bind(LoggerHolder.class);
                    binder.bind(SubLoggerHolder1.class);
                    binder.bind(SubLoggerHolder2.class);
                }
        );
    }

    @Test
    public void initializers_are_called() {
        assertThat(TestSeedInitializer.getCallCount()).isEqualTo(1);
    }

    @Test
    public void modules_are_installed_correctly() {
        HolderNominal holder = injector.getInstance(HolderNominal.class);

        assertThat(holder).isNotNull();
        assertThat(holder.s1).isNotNull();
        assertThat(holder.s3).isNull();
    }

    @Test(expected = ConfigurationException.class)
    public void modules_without_install_are_not_installed_correctly() {
        injector.getInstance(HolderException.class);
    }

    @Test
    public void logger_injection_is_working() {
        LoggerHolder holder = injector.getInstance(LoggerHolder.class);

        assertThat(LoggerHolder.logger).isNotNull();
        assertThat(holder.logger1).isSameAs(LoggerHolder.logger);
    }

    @Test
    public void logger_injection_with_subclasses() {
        SubLoggerHolder1 subHolder1 = injector.getInstance(SubLoggerHolder1.class);
        SubLoggerHolder2 subHolder2 = injector.getInstance(SubLoggerHolder2.class);

        assertThat(subHolder1.logger2).isNotNull();
        assertThat(subHolder1.logger1).isNotNull();
        assertThat(subHolder2.logger2).isNotNull();
        assertThat(subHolder2.logger1).isNotNull();
    }

    @Test
    public void multiple_package_roots_can_be_used() {
        HolderNominal holder = injector.getInstance(HolderNominal.class);

        assertThat(holder.foreignClass).isNotNull();
    }
}
