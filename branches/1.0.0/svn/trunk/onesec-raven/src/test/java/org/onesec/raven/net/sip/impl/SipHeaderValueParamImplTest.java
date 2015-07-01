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

import org.onesec.raven.net.sip_0.impl_0.SipHeaderValueParamImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class SipHeaderValueParamImplTest extends Assert {
    
    @Test(expected=Exception.class)
    public void paramStrWithoutEqualsSign() throws Exception{
        new SipHeaderValueParamImpl("assdf");
    }
    
    @Test(expected=Exception.class)
    public void emptyParamString() throws Exception{
        new SipHeaderValueParamImpl("");
    }
    
    @Test(expected=Exception.class)
    public void emptyParamNameString() throws Exception{
        new SipHeaderValueParamImpl("=val");
    }
    
    @Test
    public void nameWithoutValueTest() throws Exception {
        SipHeaderValueParamImpl param = new SipHeaderValueParamImpl("p=");
        assertEquals("p", param.getName());
        assertEquals("", param.getValue());
    }
}
