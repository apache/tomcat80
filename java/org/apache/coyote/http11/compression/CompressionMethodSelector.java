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

package org.apache.coyote.http11.compression;

import org.apache.coyote.Request;
import org.apache.tomcat.util.buf.MessageBytes;

import java.util.regex.Pattern;

import static org.apache.coyote.http11.compression.CompressionLevel.FORCE;
import static org.apache.coyote.http11.compression.CompressionMethod.GZIP;
import static org.apache.coyote.http11.compression.CompressionMethod.NONE;

public class CompressionMethodSelector {

    public static final String GZIP_CONTENT_ENCODING = "gzip";

    /**
     * Regular expression that defines the user agents to not use gzip with
     */
    private Pattern noCompressionUserAgents = null;

    public CompressionMethod getCompressionMethod(Request request,
                                                  CompressionLevel compressionLevel) {
        if (useCompression(request, compressionLevel, GZIP_CONTENT_ENCODING)) return GZIP;
        return NONE;
    }

    /**
     * Check if compression should be used for this resource. Already checked
     * that the resource could be compressed if the client supports it.
     * @param request           request
     * @param compressionLevel  compressionLevel
     * @param acceptHeaderValue acceptHeaderValue
     */
    private boolean useCompression(Request request, CompressionLevel compressionLevel,
                                   String acceptHeaderValue) {

        // Check if browser support gzip encoding
        MessageBytes acceptEncodingMB =
                request.getMimeHeaders().getValue("Accept-Encoding");

        if ((acceptEncodingMB == null)
                || (acceptEncodingMB.indexOf(acceptHeaderValue) == -1)) {
            return false;
        }

        // If force mode, always compress (test purposes only)
        if (compressionLevel == FORCE) {
            return true;
        }

        // Check for incompatible Browser
        if (noCompressionUserAgents != null) {
            MessageBytes userAgentValueMB =
                    request.getMimeHeaders().getValue("User-Agent");
            if(userAgentValueMB != null) {
                String userAgentValue = userAgentValueMB.toString();

                if (userAgentValue != null &&
                        noCompressionUserAgents.matcher(userAgentValue).matches()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Set no compression user agent pattern. Regular expression as supported
     * by {@link Pattern}.
     *
     * ie: "gorilla|desesplorer|tigrus"
     */
    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        if (noCompressionUserAgents == null || noCompressionUserAgents.length() == 0) {
            this.noCompressionUserAgents = null;
        } else {
            this.noCompressionUserAgents =
                    Pattern.compile(noCompressionUserAgents);
        }
    }
}