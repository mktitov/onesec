/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.ivr.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.RavenUtils;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.table.Table;
import org.raven.test.DataCollector;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class ExternalTextToSpeechEngineNodeTest extends OnesecRavenTestCase {
    
    private ExternalTextToSpeechEngineNode ttsNode;
    private File cacheDir;
    private DataCollector collector;
    
    @Before
    public void prepare() {
        ExecutorServiceNode executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(50);
        executor.setMaximumPoolSize(50);
        assertTrue(executor.start());
        
        ttsNode = new ExternalTextToSpeechEngineNode();
        ttsNode.setName("tts");
        tree.getRootNode().addAndSaveChildren(ttsNode);
        ttsNode.setExecutor(executor);
        ttsNode.setCacheDir("target/tts_cache");
        ttsNode.setLogLevel(LogLevel.TRACE);
        cacheDir = new File("target/tts_cache");
        
        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(ttsNode);
        
        try { FileUtils.forceDelete(cacheDir); } catch (Exception e) {}
    }
    
    @Test
    public void invalidCacheDir() throws IOException {
        assertFalse(ttsNode.start());
        FileUtils.forceMkdir(cacheDir);
        assertTrue(ttsNode.start());
    }

    @Test
    public void normalExecutionTestAndCacheTest() throws Exception {
        FileUtils.forceMkdir(cacheDir);
        ttsNode.setCommandLine("\"cp ${textFilename} ${wavFilename}\".toString()");
        assertTrue(ttsNode.start());
        
        NodeAttribute textAttr = collector.getNodeAttribute(ExternalTextToSpeechEngineNode.TEXT_ATTR);
        assertNotNull(textAttr);
        textAttr.setValue("test");
        assertTrue(collector.start());
        
        collector.refereshData(null);
        assertEquals(1, collector.getDataList().size());
        Object res = collector.getDataList().get(0);
        assertNotNull(collector.getDataList());
        assertTrue(res instanceof InputStreamSource);
        InputStreamSource source = (InputStreamSource) res;
        assertEquals("test", IOUtils.toString(source.getInputStream(), "utf-8"));
        Thread.sleep(1000);
        
        collector.getDataList().clear();
        collector.refereshData(null);
        assertEquals(1, collector.getDataList().size());
        assertSame(res, collector.getDataList().get(0));
    }
    
    @Test
    public void executionTimeoutTest() throws Exception {
        FileUtils.forceMkdir(cacheDir);
        ttsNode.setCommandLine("'sleep 2'");
        ttsNode.setCommandExecutionTimeout(1000l);
        assertTrue(ttsNode.start());
        
        NodeAttribute textAttr = collector.getNodeAttribute(ExternalTextToSpeechEngineNode.TEXT_ATTR);
        assertNotNull(textAttr);
        textAttr.setValue("test");
        assertTrue(collector.start());
        
        collector.refereshData(null);
        assertEquals(1, collector.getDataList().size());
        assertNull(collector.getDataList().get(0));
    }
    
    @Test
    public void readFromCacheTest() throws Exception {
        FileUtils.forceMkdir(cacheDir);
        String test1Base = cacheDir.getPath()+"/test1";
        String test2Base = cacheDir.getPath()+"/test2";
        File text1File = new File(test1Base+".txt");
        File wav1File = new File(test1Base+".wav");
        File text2File = new File(test2Base+".txt");
        FileUtils.writeStringToFile(text1File, "test");
        FileUtils.writeStringToFile(wav1File, "test wav file");
        FileUtils.writeStringToFile(text2File, "test2");
        
        assertTrue(text2File.exists());
        assertTrue(ttsNode.start());
        assertFalse(text2File.exists());
        NodeAttribute textAttr = collector.getNodeAttribute(ExternalTextToSpeechEngineNode.TEXT_ATTR);
        assertNotNull(textAttr);
        textAttr.setValue("test");
        assertTrue(collector.start());
        
        collector.refereshData(null);
        assertEquals(1, collector.getDataList().size());
        Object res = collector.getDataList().get(0);
        assertTrue(res instanceof InputStreamSource);
        InputStreamSource source = (InputStreamSource) res;
        assertEquals("test wav file", IOUtils.toString(source.getInputStream(), "utf-8"));
        Thread.sleep(1000);
    }
    
    @Test
    public void getViewableObjectsTest() throws Exception {
        FileUtils.forceMkdir(cacheDir);
        ttsNode.setCommandLine("\"cp ${textFilename} ${wavFilename}\".toString()");
        assertTrue(ttsNode.start());
        
        NodeAttribute textAttr = collector.getNodeAttribute(ExternalTextToSpeechEngineNode.TEXT_ATTR);
        assertNotNull(textAttr);
        textAttr.setValue("test");
        assertTrue(collector.start());
        
        List<ViewableObject> vos = ttsNode.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(2, vos.size());
        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vos.get(0).getMimeType());
        assertTrue(vos.get(0).getData() instanceof String);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vos.get(1).getMimeType());
        Object v2 = vos.get(1).getData();
        assertTrue(v2 instanceof Table);
        Table tab = (Table) v2;
        List<Object[]> rows = RavenUtils.tableAsList(tab);
        assertEquals(0, rows.size());
        
        collector.refereshData(null);
        vos = ttsNode.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(2, vos.size());
        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vos.get(0).getMimeType());
        assertTrue(vos.get(0).getData() instanceof String);
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vos.get(1).getMimeType());
        v2 = vos.get(1).getData();
        assertTrue(v2 instanceof Table);
        tab = (Table) v2;
        rows = RavenUtils.tableAsList(tab);
        assertEquals(1, rows.size());
        
        
    }
    
//    @Test
    public void execTest() throws Exception {
        Process proc = Runtime.getRuntime().exec("ls -l");
        printStream(proc.getInputStream());
        printStream(proc.getErrorStream());
    }

    private void printStream(InputStream stream) throws IOException {
        LineIterator it = IOUtils.lineIterator(stream, "utf-8");
        while (it.hasNext())
            System.out.println("LINE: "+it.nextLine());
    }
}
