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

import org.onesec.raven.net.sip.SipAddressHeader;
import org.onesec.raven.net.sip.SipConstants;
import org.onesec.raven.net.sip.SipHeaderValue;
import org.onesec.raven.net.sip.SipHeaderValueParam;

/**
 *
 * @author Mikhail Titov
 */
public class AddressHeaderImpl extends SipHeaderImpl implements SipAddressHeader, SipConstants {
    private final String address;
    private final String tag;
    private final String displayName;

    public AddressHeaderImpl(String name, String values) throws Exception {
        super(name, values);
        SipHeaderValue value = getValue();
        if (value==null)
            throw new Exception(String.format("Invalid address header (%s: %s)", name, values));
        this.address = value.getValue();
        this.displayName = value.getDisplayName();
        SipHeaderValueParam tagParam = value.getParam(TAG_PARAM);
        this.tag = tagParam==null? null : tagParam.getValue();
    }

    public AddressHeaderImpl(String name, String displayName, String address, String tag) 
            throws Exception 
    {
        this(
            name, 
            (displayName==null?"":"\""+displayName+"\" ")+"<"+address+">"+(tag==null?"":";"+TAG_PARAM+"="+tag));
    }

    public String getAddress() {
        return address;
    }

    public String getTag() {
        return tag;
    }

    public String getDisplayName() {
        return displayName;
    }
}
