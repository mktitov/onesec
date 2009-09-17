/*
 *  Copyright 2009 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.core.impl;

/**
 *
 * @author Mikhail Titov
 */
public class OnesecUtils
{
    private OnesecUtils()
    {
    }

    /**
     * Converts the array of events to string.
     * @param events the array of events
     */
    public final static String eventsToString(Object[] events)
    {
        if (events==null || events.length==0)
            return "";
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<events.length; ++i)
            buf.append((i>0? ", ":"")+events[i].toString());
        return buf.toString();
    }
}
