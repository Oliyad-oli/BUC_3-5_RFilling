package com.itas.taxfiling.infrastructure.persistence.mapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ReflectionMapper {

    public static <T> T createInstance(Class<T> clazz) {
        try {
            java.lang.reflect.Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate class " + clazz.getName(), e);
        }
    }

    public static void setField(Object obj, String fieldName, Object value) {
        if (value == null) return;
        try {
            Field field = getField(obj.getClass(), fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName + " on " + obj.getClass().getName(), e);
        }
    }

    private static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return getField(clazz.getSuperclass(), fieldName);
            }
            throw e;
        }
    }

    public static Object getField(Object obj, String fieldName) {
        if (obj == null) return null;
        try {
            Field field = getField(obj.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field " + fieldName + " on " + obj.getClass().getName(), e);
        }
    }
}
