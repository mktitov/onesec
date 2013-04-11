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
import java.util.Date;
import javax.activation.FileDataSource;
import org.apache.commons.io.FileUtils;
import org.onesec.raven.ivr.vmail.StoredVMailMessage;

/**
 *
 * @author Mikhail Titov
 */
public class StoredVMailMessageImpl extends VMailMessageImpl implements StoredVMailMessage {
    
    protected final File messageFile;
    protected final VMailBoxNode vmailBox;

    public StoredVMailMessageImpl(VMailBoxNode vmailBox, File messageFile, String senderPhoneNumber, Date messageDate) {
        super(senderPhoneNumber, messageDate, new FileDataSource(messageFile));
        this.vmailBox = vmailBox;
        this.messageFile = messageFile;
    }

    public void delete() throws Exception {
        FileUtils.forceDelete(messageFile);
        afterDelete();
    }
    
    protected void afterDelete() throws Exception {
        if (vmailBox.getSavedMessagesCount()==0)
            vmailBox.fireStatusEvent(VMailBoxStatusChannel.EventType.SBOX_BECAME_EMPTY);
    }
}
