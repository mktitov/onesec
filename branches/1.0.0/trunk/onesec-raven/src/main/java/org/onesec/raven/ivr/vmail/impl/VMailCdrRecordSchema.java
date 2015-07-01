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

import org.raven.annotations.NodeClass;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.DatabaseRecordExtension;
import org.raven.ds.impl.DatabaseRecordFieldExtension;
import org.raven.ds.impl.IdRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemasNode;
import org.raven.tree.Node;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titotv
 */
@NodeClass(parentNode=RecordSchemasNode.class)
public class VMailCdrRecordSchema extends RecordSchemaNode {
    public final static String ID = "id";
    public final static String VMAIL_BOX_ID = "vmailBox";
    public final static String VMAIL_BOX_NUMBER = "vmailBoxNumber";
    public final static String SENDER_PHONE_NUMBER = "senderPhoneNumber";
    public final static String MESSAGE_DATE = "messageDate";
    
    public final static String DATABASE_TABLE_EXTENSION_NAME = "dbTable";
    public final static String DATABASE_TABLE_NAME = "RAVEN_VMAIL_CDR";
    public final static String DATABASE_COLUMN_EXTENSION_NAME = "dbColumn";
    
    @Message private static String idDisplayName;
    @Message private static String vmailBoxIdDisplayName;
    @Message private static String vmailBoxNumberDisplayName;
    @Message private static String senderPhoneNumberDisplayName;
    @Message private static String messageDateDisplayName;
    @Message private static String datePattern;
    
    @Override
    protected void doInit() throws Exception {
        super.doInit();
        generateFields();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        generateFields();
    }
    
    private void generateFields() {
        Node node = getRecordExtensionsNode().getNode(DATABASE_TABLE_EXTENSION_NAME);
        if (node==null) {
            DatabaseRecordExtension dbExtension = new DatabaseRecordExtension();
            dbExtension.setName(DATABASE_TABLE_EXTENSION_NAME);
            getRecordExtensionsNode().addAndSaveChildren(dbExtension);
            dbExtension.setTableName(DATABASE_TABLE_NAME);
            dbExtension.start();
        }
        createField(ID, idDisplayName, RecordSchemaFieldType.LONG);
        createField(VMAIL_BOX_ID, vmailBoxIdDisplayName, RecordSchemaFieldType.LONG);
        createField(VMAIL_BOX_NUMBER, vmailBoxNumberDisplayName, RecordSchemaFieldType.STRING);
        createField(SENDER_PHONE_NUMBER, senderPhoneNumberDisplayName, RecordSchemaFieldType.STRING);
        createField(MESSAGE_DATE, messageDateDisplayName, RecordSchemaFieldType.DATE);
    }
    
    protected void createField(String name, String displayName, RecordSchemaFieldType fieldType) {
        if (getNode(name)!=null)
            return;
        String format = RecordSchemaFieldType.TIMESTAMP.equals(fieldType)? datePattern : null;
        RecordSchemaFieldNode field = super.createField(name, fieldType, format);
        field.setDisplayName(displayName);
        DatabaseRecordFieldExtension.create(field, DATABASE_COLUMN_EXTENSION_NAME, null);
        if (ID.equals(name))
            IdRecordFieldExtension.create(field, "id");
        if (RecordSchemaFieldType.TIMESTAMP.equals(fieldType))
            field.setPattern(datePattern);
    }
}
