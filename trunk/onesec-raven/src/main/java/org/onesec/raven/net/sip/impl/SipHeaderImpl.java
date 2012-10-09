/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.net.sip.impl;

import java.util.Collection;
import java.util.LinkedList;
import org.onesec.raven.net.sip.SipHeader;
import org.onesec.raven.net.sip.SipHeaderValue;
import org.raven.RavenUtils;

/**
 *
 * @author Mikhail Titov
 */
public class SipHeaderImpl implements SipHeader {
    private final String name;
    private final LinkedList<SipHeaderValue> values = new LinkedList<SipHeaderValue>();
    private final String headerString;

    public SipHeaderImpl(String name, String values) throws Exception {
        try {
            this.name = name;
            decodeValues(values);
            headerString = name+": "+values;
        } catch (Exception e) {
            throw new Exception(String.format("Error decoding header (%s: %s). %s", name, values, e.getMessage()), e);
        }
    }
    
    public String getName() {
        return name;
    }

    public SipHeaderValue getValue() {
        return values.isEmpty()? null : values.getFirst();
    }

    public Collection<SipHeaderValue> getValues() {
        return values;
    }
    
    private void decodeValues(String valuesLine) throws Exception {
        String[] toks = RavenUtils.split(valuesLine, ",",  true);
        for (String tok: toks)
            values.add(new SipHeaderValueImpl(tok));
    }

    @Override
    public String toString() {
        return headerString;
    }
}
