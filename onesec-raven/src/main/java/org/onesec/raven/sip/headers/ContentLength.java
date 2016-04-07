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
import org.onesec.raven.sip.SipHeaders.Names;
import org.onesec.raven.sip.impl.AbstractSingleValueSipHeader;

/**
 *
 * @author Mikhail Titov
 */
public class ContentLength extends AbstractSingleValueSipHeader<Integer>{

    public ContentLength(List<String> values) {
        super(Names.Content_Length.headerName, values);
    }

    @Override
    protected Integer createValue(List<String> stringValues) {
        try {
            if (stringValues.isEmpty())
                throw new IllegalArgumentException("Empty value for Content-Length is not acceptable");
            int val = Integer.parseInt(stringValues.get(0));
            if (val<0)
                throw new IllegalArgumentException("Invalid value for Content-Length header: "+stringValues.get(0));
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for Content-Length header: "+stringValues.get(0));
        }
    }    
}
