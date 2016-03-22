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
package org.onesec.raven.sip.headers;

import java.util.List;
import java.util.Map;
import org.onesec.raven.sip.SipHeaders;
import org.onesec.raven.sip.impl.AbstractSingleValueSipHeader;
import org.onesec.raven.sip.impl.SipUtils;
import org.raven.Pair;

/**
 *
 * @author Mikhail Titov
 */
public class ContentType extends AbstractSingleValueSipHeader<String> {
    private Map<String, String> params;

    public ContentType(List<String> stringValues) {
        super(SipHeaders.Names.Content_Type.headerName, stringValues);
    }

    @Override
    protected String createValue(List<String> stringValues) {
        if (stringValues.isEmpty())
            return null;
        else {
            final Pair<String, Map<String,String>> pair = SipUtils.slitHeaderValueAndParams(stringValues.get(0));
            params = pair.getValue();
            return pair.getKey();
        }
    }
    
    public String getParamValue(String name) {
        return params==null? null : params.get(name);
    }
}
