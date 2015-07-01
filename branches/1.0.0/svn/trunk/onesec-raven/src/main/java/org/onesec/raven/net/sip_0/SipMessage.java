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
package org.onesec.raven.net.sip_0;

import org.onesec.raven.net.sip_0.impl_0.FromHeaderImpl;

/**
 *
 * @author Mikhail Titov
 */
public interface SipMessage {
    public void addHeader(SipHeader header) throws Exception;
    public void setContent(String content);
    public SipAddressHeader getFrom();
    public SipAddressHeader getTo();
    public void getVia();
    public void getMaxForwards();
    public SipAddressHeader getContact();
}
