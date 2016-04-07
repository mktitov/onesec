/*
 * Copyright 2016 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.sip.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.onesec.raven.sip.SipHeaders;
import org.onesec.raven.sip.SipMessage;
import org.onesec.raven.sip.SipHeaders.Names;
import org.onesec.raven.sip.headers.ContentLength;
import org.onesec.raven.sip.headers.ContentType;

/**
 *
 * @author Mikhail Titov
 */
public class HeadersParser {
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    
    public void add(String name, String value) {
        name = SipUtils.toHeaderName(name);
        List<String> vals = headers.get(name);
        if (vals==null) {
            vals = new ArrayList<>(1);
            headers.put(name, vals);
        }
        SipUtils.splitHeaderValues(value, ',', '"', false, vals);
    }
    
    public void addHeadersToMessage(final SipMessage message) {        
        final SipHeaders sipHeaders = message.headers();
        for (Map.Entry<String, List<String>> headerDef: headers.entrySet()) {
            switch(Names.getByHeaderName(headerDef.getKey())) {
                case Content_Length:
                    sipHeaders.add(new ContentLength(headerDef.getValue()));
                    break;
                case Content_Type:
                    sipHeaders.add(new ContentType(headerDef.getValue()));
                    break;
                default:
                    sipHeaders.add(new DefaultSipHeader(headerDef.getKey(), headerDef.getValue()));
            }
        }
    }
    
    public Map<String, List<String>> headers() {
        return headers;
    }
    
    public void reset() {
        headers.clear();
    }
}
