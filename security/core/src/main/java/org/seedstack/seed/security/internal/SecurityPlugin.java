/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.security.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.seedstack.seed.SeedException;
import org.seedstack.seed.core.internal.AbstractSeedPlugin;
import org.seedstack.seed.core.internal.el.ELPlugin;
import org.seedstack.seed.security.PrincipalCustomizer;
import org.seedstack.seed.security.Realm;
import org.seedstack.seed.security.RoleMapping;
import org.seedstack.seed.security.RolePermissionResolver;
import org.seedstack.seed.security.Scope;
import org.seedstack.seed.security.SecurityConfig;
import org.seedstack.seed.security.spi.CRUDActionResolver;
import org.seedstack.seed.security.spi.SecurityScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.nuun.kernel.api.plugin.InitState;
import io.nuun.kernel.api.plugin.context.InitContext;
import io.nuun.kernel.api.plugin.request.ClasspathScanRequest;

/**
 * This plugin provides core security infrastructure, based on Apache Shiro
 * implementation.
 */
public class SecurityPlugin extends AbstractSeedPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityPlugin.class);

    private final Map<String, Class<? extends Scope>> scopeClasses = new HashMap<>();
    private final Set<SecurityProvider> securityProviders = new HashSet<>();
    private SecurityConfigurer securityConfigurer;
    private boolean elAvailable;

    private Collection<Class<? extends CRUDActionResolver>> crudActionResolvers;

    @Override
    public String name() {
        return "security";
    }

    @Override
    public Collection<Class<?>> dependencies() {
        return Lists.newArrayList(ELPlugin.class, SecurityProvider.class);
    }

    @Override
    public Collection<ClasspathScanRequest> classpathScanRequests() {
        return classpathScanRequestBuilder()
                .descendentTypeOf(Realm.class)
                .descendentTypeOf(RoleMapping.class)
                .descendentTypeOf(RolePermissionResolver.class)
                .descendentTypeOf(Scope.class)
                .descendentTypeOf(PrincipalCustomizer.class)
                .descendentTypeOf(CRUDActionResolver.class)
                .build();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public InitState initialize(InitContext initContext) {
        SecurityConfig securityConfig = getConfiguration(SecurityConfig.class);
        Map<Class<?>, Collection<Class<?>>> scannedClasses = initContext.scannedSubTypesByAncestorClass();

        configureScopes(scannedClasses.get(Scope.class));
        configureCrudActionResolvers(scannedClasses.get(CRUDActionResolver.class));
        
        securityProviders.addAll(initContext.dependencies(SecurityProvider.class));
        
        elAvailable = initContext.dependency(ELPlugin.class).isFunctionMappingAvailable();

        Collection<Class<? extends PrincipalCustomizer<?>>> principalCustomizerClasses = (Collection) scannedClasses.get(PrincipalCustomizer.class);
        securityConfigurer = new SecurityConfigurer(securityConfig, scannedClasses, principalCustomizerClasses);

        return InitState.INITIALIZED;
    }


  @SuppressWarnings("unchecked")
  //Cast collection of undefined class to collection of class that extends CRUDActionResolver
  private void configureCrudActionResolvers(Collection<Class<?>> resolvers) {
    //If there's no resolver, a warning may come handy in place
    if(resolvers == null) {
     resolvers = Collections.emptySet(); 
    }else {
    crudActionResolvers = resolvers.stream()
        .map(x -> (Class<? extends CRUDActionResolver>) x)
        .collect(Collectors.toSet());
    }
  }

    @SuppressWarnings("unchecked")
    private void configureScopes(Collection<Class<?>> scopeClasses) {
        if (scopeClasses != null) {
            for (Class<?> scopeCandidateClass : scopeClasses) {
                if (Scope.class.isAssignableFrom(scopeCandidateClass)) {
                    SecurityScope securityScope = scopeCandidateClass.getAnnotation(SecurityScope.class);
                    String scopeName;

                    if (securityScope != null) {
                        scopeName = securityScope.value();
                    } else {
                        scopeName = scopeCandidateClass.getSimpleName();
                    }

                    try {
                        scopeCandidateClass.getConstructor(String.class);
                    } catch (NoSuchMethodException e) {
                        throw SeedException.wrap(e, SecurityErrorCode.MISSING_ADEQUATE_SCOPE_CONSTRUCTOR)
                                .put("scopeName", scopeName)
                                .put("class", scopeCandidateClass.getName());
                    }

                    if (this.scopeClasses.containsKey(scopeName)) {
                        throw SeedException.createNew(SecurityErrorCode.DUPLICATE_SCOPE_NAME)
                                .put("scopeName", scopeName)
                                .put("class1", this.scopeClasses.get(scopeName).getName())
                                .put("class2", scopeCandidateClass.getName());
                    }

                    this.scopeClasses.put(scopeName, (Class<? extends Scope>) scopeCandidateClass);
                }
            }
        }
    }

    @Override
    public Object nativeUnitModule() {
        return new SecurityModule(
                securityConfigurer,
                scopeClasses,
                elAvailable,
                securityProviders,
                crudActionResolvers
        );
    }
}
