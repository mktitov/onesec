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
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.sdp.SdpParseException;

/**
 *
 * @author Mikhail Titov
 */
public class SdpAttrsParserTest extends Assert {
    final AppendableCharSequence seq = new AppendableCharSequence(512);
    final SdpAttrsParser parser = new SdpAttrsParser();
    
    @Test(expected = SdpParseException.class)
    public void exceptionTest() throws SdpParseException {
        seq.append("a=");
        parser.parseAttr(seq);
    }
    
    @Test
    public void falgsTest() throws SdpParseException {
        seq.append("a=f");
        parser.parseAttr(seq);
        seq.reset();
        seq.append("a=f2");
        parser.parseAttr(seq);
        assertTrue(parser.flags().size()==2);
        assertTrue(parser.flags().contains("f"));
        assertTrue(parser.flags().contains("f2"));
    }
    
    @Test
    public void attrsTest() throws SdpParseException {
        seq.append("a=t:");//null value test
        parser.parseAttr(seq);
        seq.reset();
        seq.append("a=t:v2");
        parser.parseAttr(seq);
        seq.reset();
        seq.append("a=b:v1");
        parser.parseAttr(seq);
        
        List<String> vals = parser.attrs().get("t");
        assertNotNull(vals);
        assertArrayEquals(new Object[]{null, "v2"}, vals.toArray());
        vals = parser.attrs().get("b");
        assertNotNull(vals);
        assertArrayEquals(new Object[]{"v1"}, vals.toArray());
    }
}
