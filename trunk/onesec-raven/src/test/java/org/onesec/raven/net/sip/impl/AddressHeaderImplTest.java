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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class AddressHeaderImplTest {
    
    @Test
    public void decodeTest() throws Exception {
        AddressHeaderImpl addr = new AddressHeaderImpl("to", "\"Display name\" <addr>;tag=tagVal");
        assertNotNull(addr);
        assertEquals("Display name", addr.getDisplayName());
        assertEquals("addr", addr.getAddress());
        assertEquals("tagVal", addr.getTag());
    }
    
    @Test
    public void createTest() throws Exception {
        AddressHeaderImpl addr = new AddressHeaderImpl("to", "Display name", "addr", "tagVal");
        assertNotNull(addr);
        assertEquals("to", addr.getName());
        assertEquals("Display name", addr.getDisplayName());
        assertEquals("addr", addr.getAddress());
        assertEquals("tagVal", addr.getTag());
    }
}
