/*
 *  Copyright 2007 Mikhail Titov.
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

package org.onesec.core.services;

import org.onesec.core.call.AddressMonitor;
import org.onesec.core.call.CallState;

/**
 *
 * @author Mikhail Titov
 */
public interface Operator
{
    /**
     * Выполняет вызов с номера А на номер Б
     * @param numA номер А
     * @param numB номер Б
     * @return Текущее состояние вызова
     */
    public CallState call(String numA, String numB);

    /**
     * Returns true if the number passed in the parameter can be used by this operator
     * @param num the number
     */
    public boolean isOperatorNumber(String num);
    /**
     * Returns new address monitor. After usage monitor must be 
     * {@link AddressMonitor#releaseMonitor() realesed}.
     */
    public AddressMonitor createAddressMonitor(String address) throws Exception;
}
