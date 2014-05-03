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
package org.onesec.raven.net.sip_0.impl_0;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.onesec.raven.net.sip_0.SipHeaderValue;
import org.onesec.raven.net.sip_0.SipHeaderValueParam;
import org.raven.RavenUtils;

/**
 *
 * @author Mikhail Titov
 */
public class SipHeaderValueImpl implements SipHeaderValue {
    private final String displayName;
    private final String value;
    private final Map<String, SipHeaderValueParam> params;

    public SipHeaderValueImpl(String valueLine) throws Exception {
        try {
            String[] toks = RavenUtils.split(valueLine, ";", false);
            //decoding value and display name
            int ltPos = toks[0].lastIndexOf('<');
            if (ltPos>=0) {
                displayName = toks[0].substring(0, ltPos).trim();
                value = toks[0].substring(ltPos+1, toks[0].length()-1);
            } else {
                displayName = null;
                value = toks[0];
            }
            //decoding value params
            if (toks.length == 1)
                params = null;
            else {
                params = new HashMap<String, SipHeaderValueParam>();
                decodeParams(toks);
            }
        } catch (Exception e) {
            throw new Exception(String.format("Error decoding value (%s). "+e.getMessage()), e);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getValue() {
        return value;
    }

    public SipHeaderValueParam getParam(String name) {
        return params!=null? params.get(name) : null;
    }

    public Collection<SipHeaderValueParam> getParams() {
        return params==null? Collections.EMPTY_LIST : params.values();
    }
    
    private void decodeParams(String[] aparams) throws Exception {
        for (int i=1; i<aparams.length; ++i) {
            SipHeaderValueParamImpl param = new SipHeaderValueParamImpl(aparams[i]);
            params.put(param.getName(), param);
        }
    }
}
