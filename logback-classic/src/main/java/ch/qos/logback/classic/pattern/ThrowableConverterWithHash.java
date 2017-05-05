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

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.classic.util.StackHasher;
import ch.qos.logback.core.CoreConstants;

import java.util.Deque;

/**
 * {@link ThrowableHandlingConverter} that computes and prepends a signature
 * hash in the stack trace
 *
 * <h2>overview</h2>
 *
 * With a log management system such as ELK, this will help you track an
 * incident:
 * <ul>
 * <li>you may join the error signature to the end user (as "technical details"
 * attached to the error message),
 * <li>from this signature, retrieve in instants the complete stack trace,
 * <li>with this unique signature you will even be able to see the error history
 * (when it occurred for the first time, number of occurrences, frequency, ...)
 * </ul>
 * <p>
 * Example of stack with inlined signatures:
 *
 * <pre style="font-size: small">
 * <span style="font-weight: bold; color: #FF5555">#07e70d1e</span>&gt; com.xyz.MyApp$MyClient$MyClientException: An error occurred while getting the things
 *   at com.xyz.MyApp$MyClient.getTheThings(MyApp.java:26)
 *   at com.xyz.MyApp.test_logging(MyApp.java:16)
 *   at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
 *   at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
 *   at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
 *   at java.lang.reflect.Method.invoke(Method.java:498)
 *   at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:47)
 *   at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
 *   ...
 * Caused by: <span style="font-weight: bold; color: #FF5555">#393b506a</span>&gt; com.xyz.MyApp$HttpStack$HttpError: I/O error on GET request for http://dummy/things
 *   at com.xyz.MyApp$HttpStack.get(MyApp.java:40)
 *   at com.xyz.MyApp$MyClient.getTheThings(MyApp.java:24)
 *   ... 23 common frames omitted
 * Caused by: <span style="font-weight: bold; color: #FF5555">#d6db326f</span>&gt; java.net.SocketTimeoutException: Read timed out
 *   at com.xyz.MyApp$HttpStack.get(MyApp.java:38)
 *   ... 24 common frames omitted
 * </pre>
 *
 * <h2>logback configuration</h2>
 *
 * <h3>using ThrowableConverterWithHash with any logback appender</h3>
 *
 * <pre style="font-size: medium">
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;configuration&gt;
 *
 * &lt;!-- using the ThrowableConverterWithHash with mostly any logback appender --&gt;
 * &lt;!-- 1: define "%sEx" as a conversion rule involving --&gt;
 * &lt;conversionRule conversionWord="sEx" converterClass="com.orange.experts.utils.logging.logback.ThrowableConverterWithHash" /&gt;
 *
 * &lt;appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender"&gt;
 * &lt;layout class="ch.qos.logback.classic.PatternLayout"&gt;
 * &lt;!-- 2: use the "%sEx" rule in the layout pattern --&gt;
 * &lt;Pattern&gt;%d{HH:mm:ss.SSS} %-5level %logger [%thread:%X{requestId:--}] - %msg%n%sEx&lt;/Pattern&gt;
 * &lt;/layout&gt;
 * &lt;!-- rest of your config ... --&gt;
 * &lt;/appender&gt;
 *
 * &lt;!-- rest of your config ... --&gt;
 * &lt;root level="INFO"&gt;
 * &lt;appender-ref ref="STDOUT" /&gt;
 * &lt;/root&gt;
 *
 * &lt;/configuration&gt;
 * </pre>
 *
 * <h3>using ThrowableConverterWithHash with logstash-logback-appender</h3>
 *
 * <pre style="font-size: medium">
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;configuration&gt;
 *
 * &lt;!-- using the ThrowableConverterWithHash with any appender from logstash-logback-appender (even simpler!) --&gt;
 * &lt;appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashSocketAppender"&gt;
 * &lt;throwableConverter class="ThrowableConverterWithHash" /&gt;
 * &lt;!-- rest of your config ... --&gt;
 * &lt;/appender&gt;
 *
 * &lt;!-- rest of your config ... --&gt;
 * &lt;root level="INFO"&gt;
 * &lt;appender-ref ref="LOGSTASH" /&gt;
 * &lt;/root&gt;
 *
 * &lt;/configuration&gt;
 * </pre>
 *
 * @author Pierre Smeyers
 * @see StackHasher
 */
public class ThrowableConverterWithHash extends ThrowableProxyConverter {

    @Override
    protected String throwableProxyToString(IThrowableProxy tp) {
        StringBuilder sb = new StringBuilder(BUILDER_CAPACITY);

        // compute stack trace hashes
        Deque<String> hashes = null;
        if (tp instanceof ThrowableProxy) {
            hashes = StackHasher.hexHashes(((ThrowableProxy) tp).getThrowable());
        }

        recursiveAppend(sb, null, ThrowableProxyUtil.REGULAR_EXCEPTION_INDENT, tp, hashes);

        return sb.toString();
    }

    private void recursiveAppend(StringBuilder sb, String prefix, int indent, IThrowableProxy tp, Deque<String> hashes) {
        if (tp == null)
            return;
        String hash = hashes == null || hashes.isEmpty() ? null : hashes.pop();
        subjoinFirstLine(sb, prefix, indent, tp, hash);
        sb.append(CoreConstants.LINE_SEPARATOR);
        subjoinSTEPArray(sb, indent, tp);
        IThrowableProxy[] suppressed = tp.getSuppressed();
        if (suppressed != null) {
            for (IThrowableProxy current : suppressed) {
                recursiveAppend(sb, CoreConstants.SUPPRESSED, indent + ThrowableProxyUtil.SUPPRESSED_EXCEPTION_INDENT, current, hashes);
            }
        }
        recursiveAppend(sb, CoreConstants.CAUSED_BY, indent, tp.getCause(), hashes);
    }

    private void subjoinFirstLine(StringBuilder buf, String prefix, int indent, IThrowableProxy tp, String hash) {
        ThrowableProxyUtil.indent(buf, indent - 1);
        if (prefix != null) {
            buf.append(prefix);
        }
        subjoinExceptionMessage(buf, tp, hash);
    }

    private void subjoinExceptionMessage(StringBuilder buf, IThrowableProxy tp, String hash) {
        if (hash != null) {
            buf.append("#" + hash + "> ");
        }
        buf.append(tp.getClassName()).append(": ").append(tp.getMessage());
    }
}