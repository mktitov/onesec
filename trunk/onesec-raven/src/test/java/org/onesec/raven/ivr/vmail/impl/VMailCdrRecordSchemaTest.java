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

import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.RavenUtils;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.DatabaseRecordExtension;
import org.raven.ds.impl.DatabaseRecordFieldExtension;
import org.raven.ds.impl.IdRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.tree.Node;
import static org.onesec.raven.ivr.vmail.impl.VMailCdrRecordSchema.*;
/**
 *
 * @author Mikhail Titov
 */
public class VMailCdrRecordSchemaTest extends OnesecRavenTestCase {
    
    @Test
    public void test() {
        VMailCdrRecordSchema schema = new VMailCdrRecordSchema();
        schema.setName("schema");
        testsNode.addAndSaveChildren(schema);
        checkRecordDbExtension(schema);
        checkField(schema, ID, RecordSchemaFieldType.LONG);
        checkField(schema, VMAIL_BOX_ID, RecordSchemaFieldType.LONG);
        checkField(schema, VMAIL_BOX_NUMBER, RecordSchemaFieldType.STRING);
        checkField(schema, SENDER_PHONE_NUMBER, RecordSchemaFieldType.STRING);
        checkField(schema, MESSAGE_DATE, RecordSchemaFieldType.DATE);
    }
    
    private void checkRecordDbExtension(VMailCdrRecordSchema schema) {
        DatabaseRecordExtension dbExt = schema.getRecordExtension(
                DatabaseRecordExtension.class, "dbTable");
        assertNotNull(dbExt);
        assertEquals(VMailCdrRecordSchema.DATABASE_TABLE_NAME, dbExt.getTableName());
    }

    private void checkField(RecordSchemaNode schema, String fieldName, RecordSchemaFieldType type) {
        RecordSchemaFieldNode field = (RecordSchemaFieldNode) schema.getNode(fieldName);
        assertNotNull(field);
        assertEquals(type, field.getFieldType());

        if (ID.equals(fieldName)) {
            Node node = field.getNode("id");
            assertNotNull(node);
            assertTrue(node instanceof IdRecordFieldExtension);
        }
        DatabaseRecordFieldExtension dbExt = (DatabaseRecordFieldExtension) field.getNode("dbColumn");
        assertNotNull(dbExt);
        assertEquals(RavenUtils.nameToDbName(fieldName), dbExt.getColumnName());
    }
}
