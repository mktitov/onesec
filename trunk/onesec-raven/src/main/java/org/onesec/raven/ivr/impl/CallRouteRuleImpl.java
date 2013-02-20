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
package org.onesec.raven.ivr.impl;

import java.util.Arrays;
import org.onesec.raven.ivr.CallRouteRule;

/**
 *
 * @author Mikhail Titov
 */
public class CallRouteRuleImpl implements CallRouteRule {
    private final String originalCallingNumber;
    private final String[] destinations;
    private final String[] callingNumbers;
    private final boolean permanent;
    private final int priority;

    public CallRouteRuleImpl(String originalCallingNumber, String[] destinations, String[] callingNumbers, 
            boolean permanent, int priority) 
    {
        this.originalCallingNumber = originalCallingNumber;
        this.destinations = destinations;
        this.callingNumbers = callingNumbers;
        this.permanent = permanent;
        this.priority = priority;
    }
    
    public CallRouteRuleImpl(String originalCallingNumber, String destination, String callingNumber, 
            boolean permanent, int priority) 
    {
        this.originalCallingNumber = originalCallingNumber;
        this.destinations = new String[]{destination};
        this.callingNumbers = new String[]{callingNumber};
        this.permanent = permanent;
        this.priority = priority;
    }
    
    public boolean accept(String callingNumber) {
        return originalCallingNumber.equals(callingNumber);
    }

    public boolean isPermanent() {
        return permanent;
    }
    
    public int getPriority() {
        return priority;
    }

    public String[] getDestinations() {
        return destinations;
    }

    public String[] getCallingNumbers() {
        return callingNumbers;
    }

    public String getRuleKey() {
        return originalCallingNumber;
    }

    @Override
    public String toString() {
        return "originalCallingNumber: "+originalCallingNumber+"; permanent route: "+permanent+
                "; destinations: "+Arrays.toString(destinations)+
                "; callingNumbers: "+Arrays.toString(callingNumbers);
    }
}
