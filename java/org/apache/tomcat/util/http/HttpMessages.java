/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.http;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tomcat.util.res.StringManager;

/**
 * Handle (internationalized) HTTP messages.
 *
 * @author James Duncan Davidson [duncan@eng.sun.com]
 * @author James Todd [gonzo@eng.sun.com]
 * @author Jason Hunter [jch@eng.sun.com]
 * @author Harish Prabandham
 * @author costin@eng.sun.com
 */
public class HttpMessages {

    private static final Map<Locale,HttpMessages> instances =
            new ConcurrentHashMap<>();

    // Keep this in separate package from standard i18n messages
    private static final HttpMessages DEFAULT = new HttpMessages(
            StringManager.getManager("org.apache.tomcat.util.http.res",
                    Locale.getDefault()));


    private final StringManager sm;

    private final String st_200;
    private final String st_302;
    private final String st_400;
    private final String st_404;
    private final String st_500;

    private HttpMessages(StringManager sm) {
        // There is a performance tradeoff here. This implementation incurs
        // ~160ns (40ns per StringManager) lookup delay on first access but all
        // subsequent lookups take ~0.25ns.
        // The alternative approach (lazy init of each cached String) delays the
        // StringManager lookup until required but increases the time for
        // subsequent lookups to ~0.5ns.
        // These times will be in the noise for most requests. This
        // implementation was chosen because:
        // - Over anything more than a few hundred requests it is faster.
        // - The code is a lot simpler. Thread safe lazy init needs care to get
        //   right. See http://markmail.org/thread/wjp3oejdyxcrz7do
        this.sm = sm;
        st_200 = sm.getString("sc.200");
        st_302 = sm.getString("sc.302");
        st_400 = sm.getString("sc.400");
        st_404 = sm.getString("sc.404");
        st_500 = sm.getString("sc.500");
    }


    /**
     * Get the status string associated with a status code. Common messages are
     * cached.
     *
     * @param status The HTTP status code to retrieve the message for
     *
     * @return The HTTP status string that conforms to the requirements of the
     *         HTTP specification
     */
    public String getMessage(int status) {
        switch (status) {
        case 200:
            return st_200;
        case 302:
            return st_302;
        case 400:
            return st_400;
        case 404:
            return st_404;
        case 500:
            return st_500;
        default:
            return sm.getString("sc."+ status);
        }
    }


    public static HttpMessages getInstance(Locale locale) {
        HttpMessages result = instances.get(locale);
        if (result == null) {
            StringManager sm = StringManager.getManager(
                    "org.apache.tomcat.util.http.res", locale);
            if (Locale.getDefault().equals(sm.getLocale())) {
                result = DEFAULT;
            } else {
                result = new HttpMessages(sm);
            }
            instances.put(locale, result);
        }
        return result;
    }


    /**
     * Filter the specified message string for characters that are sensitive
     * in HTML.  This avoids potential attacks caused by including JavaScript
     * codes in the request URL that is often reported in error messages.
     *
     * @param message The message string to be filtered
     */
    public static String filter(String message) {

        if (message == null) {
            return (null);
        }

        char content[] = new char[message.length()];
        message.getChars(0, message.length(), content, 0);
        StringBuilder result = new StringBuilder(content.length + 50);
        for (int i = 0; i < content.length; i++) {
            switch (content[i]) {
            case '<':
                result.append("&lt;");
                break;
            case '>':
                result.append("&gt;");
                break;
            case '&':
                result.append("&amp;");
                break;
            case '"':
                result.append("&quot;");
                break;
            default:
                result.append(content[i]);
            }
        }
        return (result.toString());
    }

    /**
     * Is the provided message safe to use in an HTTP header. Safe messages must
     * meet the requirements of RFC2616 - i.e. must consist only of TEXT.
     *
     * @param msg   The message to test
     * @return      <code>true</code> if the message is safe to use in an HTTP
     *              header else <code>false</code>
     */
    public static boolean isSafeInHttpHeader(String msg) {
        // Nulls are fine. It is up to the calling code to address any NPE
        // concerns
        if (msg == null) {
            return true;
        }

        // Reason-Phrase is defined as *<TEXT, excluding CR, LF>
        // TEXT is defined as any OCTET except CTLs, but including LWS
        // OCTET is defined as an 8-bit sequence of data
        // CTL is defined as octets 0-31 and 127
        // LWS, if we exclude CR LF pairs, is defined as SP or HT (32, 9)
        final int len = msg.length();
        for (int i = 0; i < len; i++) {
            char c = msg.charAt(i);
            if (32 <= c && c <= 126 || 128 <= c && c <= 255 || c == 9) {
                continue;
            }
            return false;
        }

        return true;
    }
}
