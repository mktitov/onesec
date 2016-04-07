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
import org.onesec.raven.sip.SipConstants;
import org.onesec.raven.sip.SipHeaders;
import org.onesec.raven.sip.SipMessage;
import org.onesec.raven.sip.headers.ContentType;

/**
 *
 * @author Mikhail Titov
 */
public class ContentParser {
        
    public void parse(final SipMessage message, final ByteBuf buf) {
        ContentType contentTypeHeader = message.headers().get(SipHeaders.Names.Content_Type.headerName);
        switch (contentTypeHeader.getFirstValue()) {
            case SipConstants.SDP_CONTENT_TYPE:
                
            default:
                throw new IllegalArgumentException("Invalid sip message Content-Type");
        }
    }
}
