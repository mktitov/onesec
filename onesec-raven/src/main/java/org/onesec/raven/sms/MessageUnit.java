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

package org.onesec.raven.sms;

import com.logica.smpp.pdu.SubmitSM;

/**
 *
 * @author Mikhail Titov
 */
public interface MessageUnit {
//    public boolean isLastSeg();
//    public void ready();
    public void submitted();
    public void fatal();
    public void confirmed();
    public void delay(long interval);
    public SubmitSM getPdu();
    public String getDst();
    public MessageUnitStatus getStatus();
//    public long getMessageId();
    public long getXTime();
    public int getAttempts();
    public long getFd();
    public long getConfirmTime();
    public int getSequenceNumber();
    public MessageUnitStatus checkStatus();
    public MessageUnit addListener(MessageUnitListener listener);
}
