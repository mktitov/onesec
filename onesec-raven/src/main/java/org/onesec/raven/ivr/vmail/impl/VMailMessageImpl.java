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
package org.onesec.raven.ivr.vmail.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.activation.DataSource;
import org.onesec.raven.ivr.vmail.VMailMessage;

/**
 *
 * @author Mikhail Titov
 */
public class VMailMessageImpl implements VMailMessage {
    private final String senderPhoneNumber;
    private final Date messageDate;
    private final DataSource audioSource;

    public VMailMessageImpl(String senderPhoneNumber, Date messageDate, DataSource audioSource) {
        this.senderPhoneNumber = senderPhoneNumber;
        this.messageDate = messageDate;
        this.audioSource = audioSource;
    }

    public String getSenderPhoneNumber() {
        return senderPhoneNumber;
    }

    public Date getMessageDate() {
        return messageDate;
    }

    public DataSource getAudioSource() {
        return audioSource;
    }

    @Override
    public String toString() {
        return "VoiceMessage: senderPhoneNumber="+senderPhoneNumber+
                "; messageDate="+new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    }
}
