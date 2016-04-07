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
package org.onesec.raven.sdp.impl;

import io.netty.util.internal.AppendableCharSequence;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.onesec.raven.sdp.SdpAttrs;
import org.onesec.raven.sdp.SdpParseException;
import org.onesec.raven.sip.impl.SipUtils;

/**
 *
 * @author Mikhail Titov
 */
public class SdpAttrsParser {
    private final Map<String, List<String>> attrs = new LinkedHashMap<>();
    private Set<String> flags;
    
    public void parseAttr(AppendableCharSequence seq) throws SdpParseException {
        final int len = seq.length();
        if (len<=2)
            throw new SdpParseException("Attribute (a) can not be empty: "+seq);        
        final int pos = SipUtils.indexOf(':', seq);
        if (pos==-1) {
            //parsing flag
            if (flags==null)
                flags = new HashSet<>();
            flags.add(seq.subStringUnsafe(2, len));
        } else {
            //parsing attr and value
            final String name = seq.subStringUnsafe(2, pos);
            final String value = pos+1>=len? null : seq.subStringUnsafe(pos+1, len);
            List<String> vals = attrs.get(name);
            if (vals==null) {
                vals = new ArrayList<>(1);
                attrs.put(name, vals);
            }
            vals.add(value);
        }
    }
    
    Map<String, List<String>> attrs() {
        return attrs;
    }
    
    Set<String> flags() {
        return flags;
    }

    public void addToToAttrs(final SdpAttrs sdpAttrs) {
        for (Map.Entry<String, List<String>> attrEntry: attrs.entrySet()) {
            switch(attrEntry.getKey()) {
                case SdpAttrs.RTPMAP_ATTR: 
                    break;
                default: break;    
            }
        }
    }
}
