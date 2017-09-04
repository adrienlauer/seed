/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.security.internal;

import org.seedstack.seed.security.RequiresPermissions;
import org.seedstack.seed.security.RequiresRest;
import org.seedstack.seed.security.RequiresRoles;
import org.seedstack.seed.security.internal.authorization.RequiresPermissionsInterceptor;
import org.seedstack.seed.security.internal.authorization.RequiresRestInterceptor;
import org.seedstack.seed.security.internal.authorization.RequiresRolesInterceptor;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

class SecurityAopModule extends AbstractModule {
    @Override
    protected void configure() {
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(RequiresRoles.class), new RequiresRolesInterceptor(new ShiroSecuritySupport()));
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(RequiresPermissions.class), new RequiresPermissionsInterceptor(new ShiroSecuritySupport()));
        bindRestInterceptor();
    }
    
    
    private void bindRestInterceptor() {
      RequiresRestInterceptor requiresRestInterceptor = new RequiresRestInterceptor(new ShiroSecuritySupport());
      //Allows a single annotation at class level, or single annotations on each method
      bindInterceptor(Matchers.annotatedWith(RequiresRest.class), Matchers.any(), requiresRestInterceptor);
      bindInterceptor(Matchers.any(), Matchers.annotatedWith(RequiresRest.class), requiresRestInterceptor);
    }
    
}
