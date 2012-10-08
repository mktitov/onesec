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

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.net.sip.SipHeaderValue;

/**
 *
 * @author Mikhail Titov
 */
public class SipHeaderImplTest extends Assert {
    
    @Test(expected=Exception.class)
    public void emptyHeader() throws Exception {
        SipHeaderImpl header = new SipHeaderImpl("");
    }
    
    @Test()
    public void emptyValuesPart() throws Exception {
        SipHeaderImpl header = new SipHeaderImpl("h:");
        assertNotNull(header);
        assertEquals(1, header.getValues().size());
        assertEquals("", header.getValue().getValue());
    }
    
    @Test()
    public void headerWithValuesPart() throws Exception {
        SipHeaderImpl header = new SipHeaderImpl("h: v1, v2");
        assertNotNull(header);
        assertEquals(2, header.getValues().size());
        assertEquals("v1", header.getValue().getValue());
        List<SipHeaderValue> values = new ArrayList<SipHeaderValue>(header.getValues());
        assertEquals("v2", values.get(1).getValue());
    }
    
}
