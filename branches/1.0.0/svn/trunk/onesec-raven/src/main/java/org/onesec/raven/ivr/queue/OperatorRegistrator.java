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
package org.onesec.raven.ivr.queue;

/**
 *
 * @author Mikhail Titov
 */
public interface OperatorRegistrator {
    /**
     * Returns the information about current registered operator or null
     */
    public OperatorDesc getCurrentOperator(String operatorNumber);
    /**
     * Authenticate operator. 
     * @param operatorNumber the operator's phone number
     * @param operatorCode the operator's code
     * @return Returns user description on success authorization or null
     */
    public OperatorDesc register(String operatorNumber, String operatorCode);
    /**
     * Unbinds operator from phone
     * @param operatorNumber the phone number
     */
    public void unregister(String operatorNumber);
}
