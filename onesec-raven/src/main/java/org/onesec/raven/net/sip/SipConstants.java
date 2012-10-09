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
package org.onesec.raven.net.sip;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Mikhail Titov
 */
public interface SipConstants {
    public final static String SIP_VERSION = "SIP/2.0";
    
    public final static String HEAD_ENCODING = "UTF-8";
    public final static String FROM_HEADER = "from";
    public final static String TO_HEADER = "to";
    public final static String VIA_HEADER = "via";
    public final static String MAX_FORWARDS_HEADER = "max-forwards";
    
    public final static String TAG_PARAM = "tag";
    
    public final static Set<String> NON_COMBINABLE_HEADERS = new HashSet<String>(
            Arrays.asList("www-authenticate", "authorization", "proxy-authenticate", "proxy-authorization"));
}
