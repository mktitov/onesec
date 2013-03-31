/*
 *  Copyright 2011 Mikhail Titov.
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

package org.onesec.raven.ivr.queue.impl;

import org.raven.tree.Node;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.RavenUtils;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.DatabaseRecordExtension;
import org.raven.ds.impl.DatabaseRecordFieldExtension;
import org.raven.ds.impl.IdRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import static org.onesec.raven.ivr.queue.impl.CallQueueCdrRecordSchemaNode.*;
/**
 *
 * @author Mikhail Titov
 */
public class CallQueueCdrRecordSchemaNodeTest extends OnesecRavenTestCase
{
    @Test
    public void test()
    {
        CallQueueCdrRecordSchemaNode schema = new CallQueueCdrRecordSchemaNode();
        schema.setName("schema");
        testsNode.addAndSaveChildren(schema);

        checkRecordDbExtension(schema);
        checkField(schema, ID, RecordSchemaFieldType.LONG);
        checkField(schema, TARGET_QUEUE, RecordSchemaFieldType.STRING);
        checkField(schema, HANDLED_BY_QUEUE, RecordSchemaFieldType.STRING);
        checkField(schema, PRIORITY, RecordSchemaFieldType.INTEGER);
        checkField(schema, CALLING_NUMBER, RecordSchemaFieldType.STRING);
        checkField(schema, OPERATOR_ID, RecordSchemaFieldType.STRING);
        checkField(schema, OPERATOR_NUMBER, RecordSchemaFieldType.STRING);
        checkField(schema, OPERATOR_PERSON_ID, RecordSchemaFieldType.STRING);
        checkField(schema, OPERATOR_PERSON_DESC, RecordSchemaFieldType.STRING);
        checkField(schema, OPERATOR_BUSY_TIMER, RecordSchemaFieldType.INTEGER);
        checkField(schema, LOG, RecordSchemaFieldType.STRING);
        checkField(schema, QUEUED_TIME, RecordSchemaFieldType.TIMESTAMP);
        checkField(schema, REJECTED_TIME, RecordSchemaFieldType.TIMESTAMP);
        checkField(schema, READY_TO_COMMUTATE_TIME, RecordSchemaFieldType.TIMESTAMP);
        checkField(schema, COMMUTATED_TIME, RecordSchemaFieldType.TIMESTAMP);
        checkField(schema, DISCONNECTED_TIME, RecordSchemaFieldType.TIMESTAMP);
        checkField(schema, CONVERSATION_START_TIME, RecordSchemaFieldType.TIMESTAMP);
        checkField(schema, CONVERSATION_DURATION, RecordSchemaFieldType.INTEGER);
        checkField(schema, TRANSFERED, RecordSchemaFieldType.STRING);
    }

    private void checkRecordDbExtension(CallQueueCdrRecordSchemaNode schema)
    {
        DatabaseRecordExtension dbExt = schema.getRecordExtension(
                DatabaseRecordExtension.class, "dbTable");
        assertNotNull(dbExt);
        assertEquals(CallQueueCdrRecordSchemaNode.DATABASE_TABLE_NAME, dbExt.getTableName());
    }

    private void checkField(RecordSchemaNode schema, String fieldName, RecordSchemaFieldType type)
    {
        RecordSchemaFieldNode field = (RecordSchemaFieldNode) schema.getChildren(fieldName);
        assertNotNull(field);
        assertEquals(type, field.getFieldType());

        if (ID.equals(fieldName)) {
            Node node = field.getNode("id");
            assertNotNull(node);
            assertTrue(node instanceof IdRecordFieldExtension);
        }
        DatabaseRecordFieldExtension dbExt =
                (DatabaseRecordFieldExtension) field.getNode("dbColumn");
        assertNotNull(dbExt);
        assertEquals(RavenUtils.nameToDbName(fieldName), dbExt.getColumnName());
    }
}