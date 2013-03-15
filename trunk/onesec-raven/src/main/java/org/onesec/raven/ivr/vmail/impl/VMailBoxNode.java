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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.onesec.raven.ivr.vmail.NewVMailMessage;
import org.onesec.raven.ivr.vmail.StoredVMailMessage;
import org.onesec.raven.ivr.vmail.VMailBox;
import org.onesec.raven.ivr.vmail.VMailBoxDir;
import org.raven.annotations.NodeClass;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=VMailManagerNode.class)
public class VMailBoxNode extends BaseNode implements VMailBox {
    
    public final static String DATE_PATTERN = "yyyyMMdd_HHmmss_SSS";
    
    private VMailManagerNode getManager() {
        return (VMailManagerNode) getEffectiveParent();
    }
    
    private VMailBoxDir getDir() throws Exception {
        return getManager().getVMailBoxDir(this);
    }
    
    private int getMessagesCount(File dir) {
        return !isStarted()? 0 : dir.list().length;
    }

    public int getNewMessagesCount() throws Exception {
        return getMessagesCount(getDir().getNewMessagesDir());
    }

    public List<StoredVMailMessage> getNewMessages() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getSavedMessagesCount() throws Exception {
        return getMessagesCount(getDir().getSavedMessagesDir());
    }

    public List<StoredVMailMessage> getSavedMessages() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addMessage(NewVMailMessage message) throws Exception {
        String filename = new SimpleDateFormat(DATE_PATTERN).format(message.getMessageDate())+"-"
                          + message.getSenderPhoneNumber()+".wav";
        File messFile = new File(getDir().getTempDir(), filename);
        InputStream in = message.getAudioSource().getInputStream();
        try {
            FileOutputStream out = new FileOutputStream(messFile);
            try {
                IOUtils.copy(in, out);
                messFile.renameTo(new File(getDir().getNewMessagesDir(), filename));
            } finally {
                IOUtils.closeQuietly(out);
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
