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
package org.onesec.raven.sip_1;

import java.util.Collection;

/**
 *
 * @author Mikhail Titov
 */
public interface SipHeader {
    /**
     * Returns the header name
     */
    public String getName();
    /**
     * Returns the first value of the header
     */
    public SipHeaderValue getValue();
    /**
     * Returns all header values or empty collection if header does not contains values
     */
    public Collection<SipHeaderValue> getValues();
}
