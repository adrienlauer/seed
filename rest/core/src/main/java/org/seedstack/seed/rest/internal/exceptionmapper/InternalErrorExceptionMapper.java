/*
 * Copyright © 2013-2024, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.rest.internal.exceptionmapper;

import org.seedstack.seed.Application;
import org.seedstack.seed.rest.RestConfig;
import org.seedstack.shed.exception.BaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

/**
 * Default exception mapper for an caught exception with no exception mapper associated.
 * It returns an HTTP status 500 (internal server error).
 */
@Provider
public class InternalErrorExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalErrorExceptionMapper.class);
    private final RestConfig.ExceptionMappingConfig exceptionMappingConfig;
    @Context
    private HttpServletRequest request;

    @Inject
    public InternalErrorExceptionMapper(Application application) {
        this.exceptionMappingConfig = application.getConfiguration().get(RestConfig.ExceptionMappingConfig.class);
    }

    @Override
    public Response toResponse(Exception exception) {
        String uuid = UUID.randomUUID().toString();
        LOGGER.error(buildServerMessage(uuid, exception), exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(buildUserMessage(uuid, exception))
                .build();
    }

    private String buildUserMessage(String uuid, Exception exception) {
        StringBuilder sb = new StringBuilder(16384);
        sb.append("Internal server error [").append(uuid).append("]");
        if (exceptionMappingConfig.isDetailedUserMessage()) {
            sb.append(": ").append(exception instanceof BaseException ? ((BaseException) exception).getDescription() : exception.getMessage());
        }
        return sb.toString();
    }

    private String buildServerMessage(String uuid, Exception exception) {
        StringBuilder sb = new StringBuilder(16384);
        sb.append("JAX-RS request error [").append(uuid).append("] on ").append(request.getRequestURI()).append("\n");
        if (exceptionMappingConfig.isDetailedLog()) {
            sb.append(exception.toString());
        } else {
            sb.append(exception.getMessage());
        }
        return sb.toString();
    }
}
