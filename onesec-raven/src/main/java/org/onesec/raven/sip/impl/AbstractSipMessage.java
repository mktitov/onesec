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

import org.onesec.raven.sip.SipMessage;
import org.onesec.raven.sip.headers.*;
import static org.onesec.raven.sip.SipHeaders.Names.*;
/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractSipMessage implements SipMessage {
    private Object content;
    
    @Override
    public Integer getContentLength() {
        ContentLength header = headers().get(Content_Length.getHeaderName());
        return header==null? null : header.getFirstValue();
    }

    @Override
    public String getContentType() {
        ContentType header = headers().get(Content_Type.headerName);
        return header==null? null : header.getFirstValue();
    }
    
    public  static Method parseMethod(String method) {
        try {
            return Method.valueOf(method);
        } catch (IllegalArgumentException e) {
            return Method.UNKNOWN;
        }
    }

    @Override
    public void setContent(Object body) {
        this.content = body;
    }
}
