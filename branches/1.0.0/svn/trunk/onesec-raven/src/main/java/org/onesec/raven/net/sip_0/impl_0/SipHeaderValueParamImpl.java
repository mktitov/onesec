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

import org.onesec.raven.net.sip_0.SipHeaderValueParam;
import org.raven.RavenUtils;

/**
 *
 * @author Mikhail Titov
 */
public class SipHeaderValueParamImpl implements SipHeaderValueParam {
    private final String value;
    private final String name;

    public SipHeaderValueParamImpl(String param) throws Exception {
        String[] toks = RavenUtils.split(param, "=", false);
        if (toks.length!=2)
            throw new Exception(String.format(
                    "Error decoding value parameter (%s). Not found equals sign", param));
        name = toks[0].toLowerCase();
        if (name.isEmpty())
            throw new Exception(String.format(
                    "Error decoding value parameter (%s). Empty parameter name", param));
        value = toks[1];
    }
    
    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
