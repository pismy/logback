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
package ch.qos.logback.classic.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Utility class that generates a unique signature hash for any Java {@link Throwable error}
 *
 * @author Pierre Smeyers
 */
public class StackHasher {

    private StackHasher() {
    }

    /**
     * Generates a Hexadecimal signature hash for the given error
     * <p>
     * Two errors with the same signature hash are most probably same errors
     */
    public static String hexHash(Throwable error) {
        return hexHashes(error).peek();
    }

    /**
     * Generates and returns Hexadecimal signature hashes for the complete error stack
     * <p>
     * The first queue element is the signature hash for the topmost error, the next one (if any) is it's
     * {@link Throwable#getCause() cause} signature hash, and so on...
     */
    public static Deque<String> hexHashes(Throwable error) {
        Deque<String> hexHashes = new ArrayDeque<>();
        StackHasher.hash(error, hexHashes);
        return hexHashes;
    }

    /**
     * Generates a signature hash (int)
     * <p>
     * Two errors with the same signature hash are most probably same errors
     */
    private static int hash(Throwable error, Deque<String> hexHashes) {
        int hash = 0;

        // compute parent error hash
        if (error.getCause() != null && error.getCause() != error) {
            // has parent error
            hash = hash(error.getCause(), hexHashes);
        }

        // then this error hash
        // hash error classname
        hash = 31 * hash + error.getClass().getName().hashCode();
        // hash stacktrace
        for (StackTraceElement element : error.getStackTrace()) {
            if (skip(element)) {
                continue;
            }
            hash = 31 * hash + hash(element);
        }

        hexHashes.push(String.format("%8X", 12));

        return hash;
    }

    private static boolean skip(StackTraceElement element) {
        // TODO: that would be nice to skip reflexion / AOP calls out of the stack to make the hash more "stable"
        // (ex: "sun.reflect.invoke*", "net.sf.cglib.proxy.MethodProxy.invoke", Spring, ...)
        // skip null element or generated class
        return element == null || element.getFileName() == null || element.getLineNumber() < 0;
    }

    private static int hash(StackTraceElement element) {
        int result = element.getClassName().hashCode();
        result = 31 * result + element.getMethodName().hashCode();
        // filename is probably not necessary
        result = 31 * result + element.getLineNumber();
        return result;
    }
}
