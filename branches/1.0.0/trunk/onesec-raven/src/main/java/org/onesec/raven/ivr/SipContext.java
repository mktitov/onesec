/*
 * Copyright 2014 Mikhail Titov.
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

package org.onesec.raven.ivr;

import java.text.ParseException;
import javax.sip.SipProvider;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import org.raven.sched.ExecutorService;

/**
 *
 * @author Mikhail Titov
 */
public interface SipContext {
    public SipProvider getSipProvider();
    public SipURI createSipURI(String address) throws ParseException;
    public String getIp();
    public int getPort();
    public String getProxyHost();
    public String generateTag();
    public ExecutorService getExecutor();
    public MessageFactory getMessageFactory();
    public HeaderFactory getHeaderFactory();
    public AddressFactory getAddressFactory();
}
