/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven.impl;

import com.cisco.jtapi.extensions.CiscoJtapiException;

/**
 *
 * @author Mikhail Titov
 */
public class CCMUtils {

    private CCMUtils() { }
    
    public static String ccmExLog(String mess, Throwable e) {
        return e instanceof CiscoJtapiException? 
                mess+String.format(" Error: code (%s); name (%s); description (%s)", 
                    ((CiscoJtapiException)e).getErrorCode(), ((CiscoJtapiException)e).getErrorName(), 
                    ((CiscoJtapiException)e).getErrorDescription())
                : mess;
    }
}
