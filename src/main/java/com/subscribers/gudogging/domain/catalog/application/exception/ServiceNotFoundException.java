package com.subscribers.gudogging.domain.catalog.application.exception;

import com.subscribers.gudogging.global.common.exception.ApplicationException;

public class ServiceNotFoundException extends ApplicationException {

    public ServiceNotFoundException() {
        super(CatalogErrorCode.SERVICE_NOT_FOUND);
    }
}
