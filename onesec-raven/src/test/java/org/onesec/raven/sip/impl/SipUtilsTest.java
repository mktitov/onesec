/*
 * Copyright 2016 Mikhail Titov.
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
package org.onesec.raven.sip.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.AppendableCharSequence;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.sip.SipMessageDecoderException;
import org.raven.Pair;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class SipUtilsTest {
    private final Logger logger = LoggerFactory.getLogger(SipMessageDecoderTest.class.getName());
    private final LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "Decoder", "Decoder. ", logger);
    
    
    @Test
    public void splitTest() throws Exception {
        String[] arr = new String[2];
        int cnt;
        cnt = SipUtils.split("1:2", ':', 0, -1, false, arr);
        assertEquals(cnt, 2);
        assertArrayEquals(new String[]{"1", "2"}, arr);
        
        arr = new String[2];
        cnt = SipUtils.split("12", ':', 0, -1, false, arr);
        assertEquals(cnt, 1);
        assertArrayEquals(new String[]{"12", null}, arr);
        
        arr = new String[2];
        cnt = SipUtils.split("", ':', 0, -1, false, arr);
        assertEquals(cnt, 1);
        assertArrayEquals(new String[]{"", null}, arr);
        
        arr = new String[2];
        cnt = SipUtils.split(":", ':', 0, -1, false, arr);
        assertEquals(cnt, 2);
        assertArrayEquals(new String[]{"", ""}, arr);
        
        arr = new String[2];
        cnt = SipUtils.split("12:", ':', 0, -1, false, arr);
        assertEquals(2, cnt);
        assertArrayEquals(new String[]{"12", ""}, arr);
        
        arr = new String[3];
        cnt = SipUtils.split(":12:", ':', 0, -1, false, arr);
        assertEquals(cnt, 3);
        assertArrayEquals(new String[]{"", "12", ""}, arr);
        
        arr = new String[3];
        cnt = SipUtils.split(":12:", ':', 1, 1, false, arr);
        assertEquals(1, cnt);
        assertArrayEquals(new String[]{"12", null, null}, arr);
        
        arr = new String[3];
        cnt = SipUtils.split(":12:", ':', 1, 1, true, arr);
        assertEquals(1, cnt);
        assertArrayEquals(new String[]{"12:", null, null}, arr);
        
        arr = new String[1];
        cnt = SipUtils.split(":12:", ':', 1, -1, true, arr);
        assertEquals(1, cnt);
        assertArrayEquals(new String[]{"12:"}, arr);
        
        arr = new String[3];
        cnt = SipUtils.split(":12:", ':', 0, 2, true, arr);
        assertEquals(2, cnt);
        assertArrayEquals(new String[]{"", "12:", null}, arr);
    }    
    
    @Test
    public void splitExactByWsTest() {
        splitExactByWs("v1", new String[]{"v1"}, true, 0);
        splitExactByWs("v1 ", new String[]{"v1"}, true, 0);
        splitExactByWs("v1 v2", new String[]{"v1", "v2"}, true, 0);
        splitExactByWs("v1 v2 ", new String[]{"v1", "v2"}, true, 0);
        splitExactByWs("v1  \tv2", new String[]{"v1", "v2"}, true, 0);
        splitExactByWs("v1 v2", new String[]{"v1", "v2", null}, false, 0);
        splitExactByWs("v1 v2", new String[]{"v1"}, false, 0);        
        splitExactByWs("", new String[]{""}, true, 0);
        splitExactByWs("p=v1 v2", new String[]{"v1","v2"}, true, 2);
    }
    
    @Test
    public void toHeaderNameTest() {
        assertEquals("Test-Name", SipUtils.toHeaderName("  test-name "));
    }
    
    @Test
    public void splitHeaderValues() {
        assertEquals("value", SipUtils.splitHeaderValues("value", ',', '"'));
        assertEquals("value", SipUtils.splitHeaderValues("  value", ',', '"'));
        assertEquals("value", SipUtils.splitHeaderValues("value  ", ',', '"'));
        assertEquals("value\"  \"", SipUtils.splitHeaderValues("value\"  \"  ", ',', '"'));
        assertEquals("\"  \"value", SipUtils.splitHeaderValues("\"  \"value", ',', '"'));
        assertEquals("va\",  \"lue", SipUtils.splitHeaderValues("va\",  \"lue", ',', '"'));
        //
        checkArr(new Object[]{"value1","value2"}, "value1,value2");
        checkArr(new Object[]{"value1","value2"}, "value1, value2");
        checkArr(new Object[]{"value1","value2"}, "value1 ,value2");
        checkArr(new Object[]{"value1","value2"}, "  value1   ,  value2  ");
        checkArr(new Object[]{"value1\", \"","value2"}, "  value1\", \"   ,  value2  ");
        checkArr(new Object[]{}, ",");
        checkArr(new Object[]{}, " ,");
        checkArr(new Object[]{}, " , ");
        checkArr(new Object[]{}, ",,");
        checkArr(new Object[]{}, " , , ");
        checkArr(new Object[]{"value1","value2"}, "value1,,value2");
        checkArr(new Object[]{"value1","value2"}, ",value1,,value2,");        
    }
    
    @Test
    public void splitHeaderValuesByWS() {
        assertEquals("value", SipUtils.splitHeaderValuesByWS("value", '"'));
        assertEquals("value1\" \"value2", SipUtils.splitHeaderValuesByWS("value1\" \"value2", '"'));
        checkArr2(new Object[]{"value1","value2"}, "value1 value2");        
        checkArr2(new Object[]{"value1","value2"}, " \tvalue1  value2\t");        
        
    }
    
    @Test
    public void splitHeaderValueAndParam() {
        checkValueParamPair(null, null, SipUtils.slitHeaderValueAndParams(""));
        checkValueParamPair("name", null, SipUtils.slitHeaderValueAndParams("name"));
        checkValueParamPair("name", null, SipUtils.slitHeaderValueAndParams("name;"));
        checkValueParamPair("name", new String[][]{new String[]{"param", null}}, SipUtils.slitHeaderValueAndParams("name;param"));
        checkValueParamPair("name", new String[][]{new String[]{"param", null}}, SipUtils.slitHeaderValueAndParams("name;param="));
        checkValueParamPair("name", new String[][]{new String[]{"param", null}}, SipUtils.slitHeaderValueAndParams("name;param=\"\""));
        checkValueParamPair("name", new String[][]{
                new String[]{"param", null},
                new String[]{"param2", "v"}
            }, 
            SipUtils.slitHeaderValueAndParams("name;param;param2=v")
        );
        checkValueParamPair("value", new String[][]{
                new String[]{"p1", "v1"},
                new String[]{"p2", "v2"},
                new String[]{"p3", "V3"},
            }, 
            SipUtils.slitHeaderValueAndParams("value;P1=V1;p2=v2;p3=\"V3\"")
        );
    }
    
    @Test
    public void readLineWithAllDataInBufTest() throws SipMessageDecoderException {
        AppendableCharSequence seq = new AppendableCharSequence(32);
        ByteBuf buf = Unpooled.buffer(1024);
        String str;
        //
        str = "test";
        buf.writeBytes(SipUtils.toBytes(str));
        assertTrue(SipUtils.readLine(buf, seq, 32, true, logger));
        assertEquals("test", seq.toString());
        //parse two lines
        buf.clear(); seq.reset();
        str = "line1\n\rline2";
        buf.writeBytes(SipUtils.toBytes(str));
        assertTrue(SipUtils.readLine(buf, seq, 5, true, logger));
        assertEquals("line1", seq.toString());
        seq.reset();
        assertTrue(SipUtils.readLine(buf, seq, 32, true, logger));
        assertEquals("line2", seq.toString());
        //parse two lines
        buf.clear(); seq.reset();
        str = "line1\nline2";
        buf.writeBytes(SipUtils.toBytes(str));
        assertTrue(SipUtils.readLine(buf, seq, 32, true, logger));
        assertEquals("line1", seq.toString());
        seq.reset();
        assertTrue(SipUtils.readLine(buf, seq, 32, true, logger));
        assertEquals("line2", seq.toString());
    }
    
    @Test (expected = SipMessageDecoderException.class)
    public void readLineMaxSizeTest() throws SipMessageDecoderException {
        AppendableCharSequence seq = new AppendableCharSequence(32);
        ByteBuf buf = Unpooled.buffer(1024).writeBytes(SipUtils.toBytes("12345"));
        SipUtils.readLine(buf, seq, 4, false, logger);
    }
    
    @Test
    public void readLineWithPartialDataInBuffer() throws Exception {
        AppendableCharSequence seq = new AppendableCharSequence(32);
        ByteBuf buf = Unpooled.buffer(1024);
        String str;
        //
        str = "test";
        buf.writeBytes(SipUtils.toBytes(str));
        assertFalse(SipUtils.readLine(buf, seq, 32, false, logger));
        assertEquals("test", seq.toString());
        buf.writeBytes(SipUtils.toBytes("\n\r"));
        assertTrue(SipUtils.readLine(buf, seq, 32, false, logger));
        assertEquals("test", seq.toString());
        buf.writeBytes(SipUtils.toBytes("test2\n\r"));
        seq.reset();
        assertTrue(SipUtils.readLine(buf, seq, 32, false, logger));
        assertEquals("test2", seq.toString());
    }
    
//    private static String doReadLine(String str, ByteBuf buf, AppendableCharSequence seq) {
//        buf.writeBytes(SipUtils.toBytes(str));
//        
//    }
    
    private void checkValueParamPair(String expectParamValue, String[][] expectParams, Pair<String, Map<String, String>> pair) {
        if (expectParamValue==null)
            assertNull(pair);
        else {
            assertEquals(expectParamValue, pair.getKey());
            if (expectParams==null)
                assertNull(pair.getValue());
            else {
                Map<String, String> expectParamsMap = new HashMap<>();
                for (String[] param: expectParams)
                    expectParamsMap.put(param[0], param[1]);
                assertEquals(expectParamsMap, pair.getValue());
            }
        }
    }
    
    private void checkArr(Object[] expect, String strToSplit) {
        Object res = SipUtils.splitHeaderValues(strToSplit, ',', '"');
        assertTrue(res instanceof List);
        assertArrayEquals(expect, ((List)res).toArray());
    }
    
    private void checkArr2(Object[] expect, String strToSplit) {
        Object res = SipUtils.splitHeaderValuesByWS(strToSplit, '"');
        assertTrue(res instanceof List);
        assertArrayEquals(expect, ((List)res).toArray());
    }
    
    private void splitExactByWs(String line, String[] expectArr, boolean expectRes, int startFrom) {
        AppendableCharSequence cs = new AppendableCharSequence(128);
        cs.append(line);
        String[] resArr = new String[expectArr.length];
        boolean splitRes = SipUtils.splitExactByWs(resArr, cs, startFrom);
        assertEquals(expectRes, splitRes);
        if (splitRes)
            assertArrayEquals(expectArr, resArr);
    }
    
}
