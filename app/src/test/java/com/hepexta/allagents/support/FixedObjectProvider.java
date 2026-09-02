package com.hepexta.allagents.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

public class FixedObjectProvider<T> implements ObjectProvider<T> {

    private final T value;

    public FixedObjectProvider(T value) {
        this.value = value;
    }

    @Override
    public T getObject(Object... args) throws BeansException {
        return value;
    }

    @Override
    public T getIfAvailable() throws BeansException {
        return value;
    }

    @Override
    public T getIfUnique() throws BeansException {
        return value;
    }

    @Override
    public T getObject() throws BeansException {
        return value;
    }
}
