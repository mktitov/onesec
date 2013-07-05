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
package org.onesec.raven.sms.impl;

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
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class, childNodes=RecordSchemaFieldNode.class)
public class SmsRecordSchemaNode extends RecordSchemaNode {
    public final static String ID = "id";
    public final static String MESSAGE = "message";
    public final static String ADDRESS = "address";
    public final static String COMPLETION_CODE = "completionCode";
    public final static String SEND_TIME = "sendTime";

    public final static String DATABASE_TABLE_EXTENSION_NAME = "dbTable";
    public final static String DATABASE_TABLE_NAME = "RAVEN_SMS_MESSAGES";
    public final static String DATABASE_COLUMN_EXTENSION_NAME = "dbColumn";
    
    @Message private static String datePattern;
    @Message private static String idDisplayName;
    @Message private static String messageDisplayName;
    @Message private static String addressDisplayName;
    @Message private static String completionCodeDisplayName;
    @Message private static String sendTimeDisplayName;
    
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
        createField(MESSAGE, messageDisplayName, RecordSchemaFieldType.STRING);
        createField(ADDRESS, addressDisplayName, RecordSchemaFieldType.STRING);
        createField(COMPLETION_CODE, completionCodeDisplayName, RecordSchemaFieldType.STRING);
        createField(SEND_TIME, sendTimeDisplayName, RecordSchemaFieldType.DATE);
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
