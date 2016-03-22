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

import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.Map;
import org.onesec.raven.sip.SipConstants;
import org.onesec.raven.sip.SipHeader;
import org.onesec.raven.sip.SipHeaders;


/**
 *
 * @author Mikhail Titov
 */
public class SipHeadersImpl implements SipHeaders {
    
    private final Map<String, SipHeader> headers = new LinkedHashMap<>(8);

//    @Override
//    public Set<String> getHeaderNames() {
//        return headers.keySet();
//    }
//
//    @Override
//    public String getFirstValue(String headerName) {
//        Object value = headers.get(headerName);
//        if (value==null)
//            return null;
//        else if (value instanceof List)
//            return ((List)value).isEmpty()? null : ((List)value).get(0).toString();
//        else
//            return value.toString();
//    }
//
//    @Override
//    public List<String> getAllValues(String headerName) {
//        Object val = headers.get(headerName);
//        if (val instanceof List) {
//            List list = (List)val;
//            if (list.isEmpty())
//                return Collections.EMPTY_LIST;
//            List<String> newList = new ArrayList<>(list.size());
//            for (Object item: list) {
//                newList.add(item.toString());
//            }
//            return newList;
//        }
//        return Arrays.asList(val.toString());
//    }        
//
//    @Override
//    public void add(String name, String value) {
//        name = SipUtils.toHeaderName(name);
//        Object _value = parseHeaderValue(name, value);
//        Object curValue = headers.get(name);
//        if (curValue instanceof List) {
//            if (_value instanceof List)
//                ((List)curValue).addAll(((List)_value));
//            else
//                ((List)curValue).add(_value);
//        } if (curValue==null) {
//            headers.put(name, _value);
//        } else {
//            if (_value instanceof List)
//                ((List)_value).add(0, curValue);
//            else
//                headers.put(name, Arrays.asList(curValue, _value));
//        }
//    }
//    
//    private Object parseHeaderValue(String name, String value) {
//        return defaultValueParser(value);
//    }
//    
//    private Object defaultValueParser(String value) {
//        return SipUtils.splitHeaderValues(value, ',', '"');
//    }
//    
//    @Override
//    public ByteBuf writeTo(ByteBuf buf) {
//        for (Map.Entry<String, Object> header: headers) {
//            Object value = header.getValue();
//        }
//    }
//    
//    private void writeHeaderTo(final ByteBuf buf, final String name, final Object value) {
//        buf.writeBytes(SipUtils.toBytes(name)).writeByte(':').writeByte(' ');
//        if (value instanceof ByteBufWriteable)
//            ((ByteBufWriteable)value).writeTo(buf);
//        else
//            buf.writeBytes(toBytes(value.toString()));
//    }

    @Override
    public <T extends SipHeader> T get(String name) {
        return (T) headers.get(name);
    }

    @Override
    public void add(SipHeader header) {
        headers.put(header.getName(), header);
    }

    @Override
    public ByteBuf writeTo(ByteBuf buf) {
        for (SipHeader header: headers.values())
            header.writeTo(buf).writeBytes(SipConstants.CRLF);
        return buf;
    }
}
