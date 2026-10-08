package com.example.lending.loan.ops.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

final class PrincipalPropertyReader {

    private static final Logger log = LoggerFactory.getLogger(PrincipalPropertyReader.class);

    private PrincipalPropertyReader() {
    }

    static String getPrincipalProperty(Object principal, String property) throws IOException {
        String strValue = null;

        try {
            BeanInfo bi = Introspector.getBeanInfo(principal.getClass());

            // Loop through the properties to get the string value of the specified property
            boolean foundProperty = false;
            for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
                if (pd.getName().equals(property) && (Modifier.isPublic(pd.getReadMethod().getModifiers()))) {
                    Object value = null;
                    try {
                        pd.getReadMethod().setAccessible(true);
                        value = pd.getReadMethod().invoke(principal, (Object[]) null);
                    } finally {
                        pd.getReadMethod().setAccessible(false);
                    }
                    strValue = String.valueOf(value);
                    foundProperty = true;
                    break;
                }
            }

            if (!foundProperty) {
                final String message = "Property [" + property + "] not found in principal of type ["
                        + principal.getClass().getName() + "]";
                if (log.isErrorEnabled()) {
                    log.error(message);
                }
                throw new IOException(message);
            }

        } catch (IntrospectionException | IOException | IllegalAccessException | InvocationTargetException e) {
            final String message = "Error reading property [" + property + "] from principal of type ["
                    + principal.getClass().getName() + "]";
            if (log.isErrorEnabled()) {
                log.error(message, e);
            }
            throw new IOException(message);
        }

        return strValue;
    }
}
