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

package org.onesec.raven.ivr.actions;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.SentenceResult;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.tree.DataFileException;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class SentenceSubactionTestHelper extends OnesecRavenTestCase {
    protected ResourceManager resourceManager;
    protected Map<String, String> params;
    
    @Override
    protected void configureRegistry(Set<Class> builder) {
        super.configureRegistry(builder); 
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
        params = new HashMap<String, String>();
    }
    
    @Before
    public void prepareHelper() {
        resourceManager = registry.getService(ResourceManager.class);
    }
    
    protected AudioFile createFile(Node owner, String name) throws DataFileException {
        AudioFileNode file = new AudioFileNode();
        file.setName(name);
        owner.addAndSaveChildren(file);
        file.getAudioFile().setDataStream(new ByteArrayInputStream(new byte[]{1,2,3}));
        file.getAudioFile().setFilename(name);
        assertTrue(file.start());
        return file;
    }
    
    protected Node createContainer(Node owner, String name) {
        ContainerNode container = new ContainerNode(name);
        owner.addAndSaveChildren(container);
        assertTrue(container.start());
        return container;
    }
    
    protected void createAttr(Node owner, String name, Node value) throws Exception {
        NodeAttributeImpl attr = new NodeAttributeImpl(name, Node.class, value, null);
        attr.setOwner(owner);
        attr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        attr.init();
        owner.addAttr(attr);
    }
    
    protected void checkSentences(String[][] words, List<SentenceResult> sentences) {
        assertEquals(words.length, sentences.size());
        for (int i=0; i<words.length; ++i) {
            List<String> names = new LinkedList<String>();
            for (AudioFile file: sentences.get(i).getWords())
                names.add(file.getName());
            assertArrayEquals(words[i], names.toArray());
        }
    }
    
    protected String[] arr(String... args) {
        return args;
    }
    
    protected String[][] arrOfArr(String[]... args) {
        return args;
    }
    
}
