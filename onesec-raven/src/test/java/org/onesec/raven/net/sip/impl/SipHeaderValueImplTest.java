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

import org.onesec.raven.net.sip_0.impl_0.SipHeaderValueImpl;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class SipHeaderValueImplTest extends Assert {
    
    @Test
    public void emptyValueString() throws Exception {
        SipHeaderValueImpl val = new SipHeaderValueImpl("");
        assertNotNull(val);
        assertNull(val.getDisplayName());
        assertEquals("", val.getValue());
    }
    
    @Test
    public void simpleValueString() throws Exception {
        SipHeaderValueImpl val = new SipHeaderValueImpl("value1");
        assertNotNull(val);
        assertNull(val.getDisplayName());
        assertEquals("value1", val.getValue());
    }
    
    @Test
    public void valueWithDisplayName() throws Exception {
        SipHeaderValueImpl val = new SipHeaderValueImpl("\"Display <> name\" <value>");
        assertNotNull(val);
        assertEquals("Display <> name", val.getDisplayName());
        assertEquals("value", val.getValue());
    }
    
    @Test
    public void valueWithParams() throws Exception {
        SipHeaderValueImpl val = new SipHeaderValueImpl("\"Display <> name\" <value>;p1=v1;p2=v2");
        assertNotNull(val);
        assertEquals("Display <> name", val.getDisplayName());
        assertEquals("value", val.getValue());
        assertEquals(2, val.getParams().size());
        assertEquals("v1", val.getParam("p1").getValue());
        assertEquals("v2", val.getParam("p2").getValue());
    }
}
