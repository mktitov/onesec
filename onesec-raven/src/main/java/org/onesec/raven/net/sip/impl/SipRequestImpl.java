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
import org.onesec.raven.net.sip.SipRequest;

/**
 *
 * @author Mikhail Titov
 */
public class SipRequestImpl extends AbstractSipMessage implements SipRequest {
    private final String requestUri;
    private final String method;

    public SipRequestImpl(String method, String requestUri) {
        this.requestUri = requestUri;
        this.method = method;
    }

  public SipAddressHeader getFrom() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public SipAddressHeader getTo() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void getVia() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void getMaxForwards() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public SipAddressHeader getContact() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
    
}
