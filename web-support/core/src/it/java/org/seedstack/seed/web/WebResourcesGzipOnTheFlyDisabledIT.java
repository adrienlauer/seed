/**
 * Copyright (c) 2013-2015 by The SeedStack authors. All rights reserved.
 *
 * This file is part of SeedStack, An enterprise-oriented full development stack.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.web;

import org.seedstack.seed.it.AbstractSeedWebIT;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

import java.net.URL;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class WebResourcesGzipOnTheFlyDisabledIT extends AbstractSeedWebIT {
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class).addAsResource("configuration/org.seedstack.seed.web.gzip-otf-disabled.properties", "META-INF/configuration/org.seedstack.seed.web.gzip-otf-disabled.properties").addAsWebResource("META-INF/resources/test.js", "/docroot-test.js").addAsWebResource("META-INF/resources/test.js.gz", "/docroot-test.js.gz").addAsWebResource("META-INF/resources/test.min.js", "/docroot-test.min.js").addAsWebResource("META-INF/resources/test.min.js.gz", "/docroot-test.min.js.gz").addAsWebResource("META-INF/resources/test2.js", "/docroot-test2.js").setWebXML("WEB-INF/web.xml");
    }

    @Test
    @RunAsClient
    public void classpath_pre_gzipped_webresources_with_gzip_otf_disabled_are_still_gzipped(@ArquillianResource URL baseURL) throws Exception {
        expect().statusCode(200).header("Content-Encoding", equalTo("gzip")).body(containsString("var minifiedJS = {};")).when().get(baseURL.toString() + "test.js");
    }

    @Test
    @RunAsClient
    public void docroot_pre_gzipped_webresources_with_gzip_otf_disabled_are_still_gzipped(@ArquillianResource URL baseURL) throws Exception {
        expect().statusCode(200).header("Content-Encoding", equalTo("gzip")).body(containsString("var minifiedJS = {};")).when().get(baseURL.toString() + "docroot-test.js");
    }

    @Test
    @RunAsClient
    public void classpath_webresources_with_gzip_otf_disabled_are_not_gzipped(@ArquillianResource URL baseURL) throws Exception {
        expect().statusCode(200).header("Content-Encoding", not(equalTo("gzip"))).body(containsString("var JS2 = {};")).when().get(baseURL.toString() + "test2.js");
    }

    @Test
    @RunAsClient
    public void docroot_webresources_with_gzip_otf_disabled_are_not_gzipped(@ArquillianResource URL baseURL) throws Exception {
        expect().statusCode(200).header("Content-Encoding", not(equalTo("gzip"))).body(containsString("var JS2 = {};")).when().get(baseURL.toString() + "docroot-test2.js");
    }
}
