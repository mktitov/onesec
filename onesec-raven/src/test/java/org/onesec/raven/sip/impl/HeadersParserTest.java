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

import java.util.List;
import mockit.Mocked;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.sip.SipHeaders;
import org.onesec.raven.sip.SipMessage;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class HeadersParserTest {
    private final HeadersParser parser = new HeadersParser();
    
    @Test
    public void singleValueTest() {
        parser.add("test-name1", "1value1");
        parser.add("test-name2", "2value1");
        checkHeader("Test-Name1", new String[]{"1value1"});
        checkHeader("Test-Name2", new String[]{"2value1"});
    }
    
    @Test
    public void addMultiplyValuesTest() {
        parser.add("test-name1", "1value1");
        parser.add("test-name1", "1value2");
        checkHeader("Test-Name1", new String[]{"1value1", "1value2"});        
    }
    
    @Test
    public void addMultiplyValuesInOneString() {
        parser.add("test-name1", "1value1, 1value2");
        checkHeader("Test-Name1", new String[]{"1value1", "1value2"});        
    }
    
    @Test
    public void addToHeadersTest(
            @Mocked final SipMessage message,
            @Mocked final SipHeaders headers
    ) {
        parser.add("test-name1", "1value1");
        parser.add("test-name2", "2value1");
        parser.addHeadersToMessage(message);
        new VerificationsInOrder() {{
            DefaultSipHeader h1;
            DefaultSipHeader h2;
            headers.add(h1=withCapture()); 
            headers.add(h2=withCapture()); 
            //
            assertEquals("Test-Name1", h1.getName());
            assertEquals("1value1", h1.getFirstValue());
            assertEquals("Test-Name2", h2.getName());
            assertEquals("2value1", h2.getFirstValue());
            
        }};
    }
    
    private void checkHeader(String headerName, String[] values) {
        List<String> vals = parser.headers().get(headerName);
        assertNotNull(vals);
        assertArrayEquals(values, vals.toArray());
    }
}
