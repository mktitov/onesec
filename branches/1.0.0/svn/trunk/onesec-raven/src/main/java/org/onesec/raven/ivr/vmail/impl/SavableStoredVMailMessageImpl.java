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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.onesec.raven.ivr.vmail.SavableStoredVMailMessage;

/**
 *
 * @author Mikhail Titov
 */
public class SavableStoredVMailMessageImpl extends StoredVMailMessageImpl implements SavableStoredVMailMessage {
    private final File savedMessagesDir;

    public SavableStoredVMailMessageImpl(VMailBoxNode vmailBox, File savedMessagesDir, File messageFile, 
            String senderPhoneNumber, Date messageDate) 
    {
        super(vmailBox, messageFile, senderPhoneNumber, messageDate);
        this.savedMessagesDir = savedMessagesDir;
    }

    public void save() throws Exception {
        boolean sboxWasEmpty = vmailBox.getSavedMessagesCount()==0;
        if (!messageFile.renameTo(new File(savedMessagesDir, messageFile.getName())))
            throw new IOException(String.format(
                "Error moving message (%s) to directory (%s)", messageFile, savedMessagesDir));
        if (vmailBox.getNewMessagesCount()==0)
            vmailBox.fireStatusEvent(VMailBoxStatusChannel.EventType.NBOX_BECAME_EMPTY);
        if (sboxWasEmpty)
            vmailBox.fireStatusEvent(VMailBoxStatusChannel.EventType.SBOX_BECAME_NON_EMPTY);
    }

    @Override
    protected void afterDelete() throws Exception {
        if (vmailBox.getNewMessagesCount()==0)
            vmailBox.fireStatusEvent(VMailBoxStatusChannel.EventType.NBOX_BECAME_EMPTY);
    }
    
}
