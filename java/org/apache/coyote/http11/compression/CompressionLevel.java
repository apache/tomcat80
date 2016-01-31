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

import org.apache.catalina.tribes.util.Arrays;

public enum CompressionLevel {
    OFF(0, 0),
    ON(1, 5),
    FORCE(2, 11);

    public int gzipLevel;
    public int brotliQuality;

    CompressionLevel(int gzipLevel, int brotliQuality) {
        this.gzipLevel = gzipLevel;
        this.brotliQuality = brotliQuality;
    }

    public String asString() {
        switch (this) {
            case ON:
                return "on";
            case FORCE:
                return "force";
            default:
                return "off";
        }
    }

    public static CompressionLevel fromStringOrNull(String name) {
        if ("on".equalsIgnoreCase(name)) {
            return ON;
        } else if ("force".equalsIgnoreCase(name)) {
            return FORCE;
        } else if ("off".equalsIgnoreCase(name)) {
            return OFF;
        }
        throw new IllegalArgumentException("Invalid compression level '" + name + "'. Allowed values: " + Arrays.toString(CompressionLevel.values()));
    }
}