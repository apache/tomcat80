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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCompressionMethodSelector {

    private CompressionMethodSelector compressionMethodSelector;
    private Request request;

    @Before
    public void setUp() throws Exception {
        compressionMethodSelector = new CompressionMethodSelector();
        request = new Request();
    }

    @Test
    public void testWhenAcceptEncodingIsMissing()
            throws Exception {

        CompressionMethod method;

        method = compressionMethodSelector
                .getCompressionMethod(request, CompressionLevel.ON);
        assertEquals(CompressionMethod.NONE, method);

        method = compressionMethodSelector
                .getCompressionMethod(request, CompressionLevel.FORCE);
        assertEquals(CompressionMethod.NONE, method);

        method = compressionMethodSelector
                .getCompressionMethod(request, CompressionLevel.OFF);
        assertEquals(CompressionMethod.NONE, method);
    }

    @Test
    public void testWhenAcceptEncodingHasValueGZIP()
            throws Exception {

        CompressionMethod method;
        request.getMimeHeaders()
                .addValue("Accept-Encoding")
                .setString(CompressionMethodSelector.GZIP_CONTENT_ENCODING);

        method = compressionMethodSelector
                .getCompressionMethod(request, CompressionLevel.ON);
        assertEquals(CompressionMethod.GZIP, method);

        method = compressionMethodSelector
                .getCompressionMethod(request, CompressionLevel.FORCE);
        assertEquals(CompressionMethod.GZIP, method);

        method = compressionMethodSelector
                .getCompressionMethod(request, CompressionLevel.OFF);
        assertEquals(CompressionMethod.GZIP, method);
    }
}