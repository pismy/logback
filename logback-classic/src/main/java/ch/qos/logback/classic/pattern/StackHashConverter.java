/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2017, QOS.ch. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 * 
 * or (per the licensee's choosing)
 * 
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic.pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.util.StackHasher;
import ch.qos.logback.core.CoreConstants;

/**
 * A {@link ch.qos.logback.core.pattern.Converter} able to generate a {@code stack_hash} for a log with a stack trace
 *
 * @author Pierre Smeyers
 */
public class StackHashConverter extends ClassicConverter {

    String defaultValue;

    @Override
    public void start() {
        defaultValue = getFirstOption();
        if(defaultValue == null) {
            defaultValue = CoreConstants.EMPTY_STRING;
        }
        super.start();
    }

    @Override
    public String convert(ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return defaultValue;
        }
        return StackHasher.hexHash(((ThrowableProxy)event.getThrowableProxy()).getThrowable());
    }
}
