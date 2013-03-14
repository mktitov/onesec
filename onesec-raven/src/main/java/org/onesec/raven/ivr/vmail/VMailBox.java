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
package org.onesec.raven.ivr.vmail;

import java.util.List;

/**
 *
 * @author Mikhail Titov
 */
public interface VMailBox {
    /**
     * Returns count of new messages
     */
    public int getNewMessagesCount();
    /**
     * Returns list of new messages. Never returns null
     */
    public List<VMailMessage> getNewMessages();
    /**
     * Returns the count of saved messages
     */
    public int getSavedMessagesCount();
    /**
     * Returns list of new messages. Never returns null
     */
    public List<VMailMessage> getSavedMessages();
    /**
     * Adds new message to vmail box
     */
    public void addMessage(VMailMessage message);
    /**
     * Returns <b>true</b> if vmail box owned by phone number passed in the parameter
     */
    public boolean isVBoxPhoneNumber(String phoneNumber);
}
