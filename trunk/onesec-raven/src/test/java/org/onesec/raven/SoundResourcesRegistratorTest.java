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
package org.onesec.raven;

import java.util.Locale;
import org.junit.Test;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public class SoundResourcesRegistratorTest extends OnesecRavenTestCase implements Constants {
    
    @Test
    public void test() throws Exception {
        ResourceManager resourceManager = registry.getService(ResourceManager.class);
        assertNotNull(resourceManager);
        checkRes(resourceManager, "/hello", "hello_ru.wav");
        checkRes(resourceManager, "/numbers/female/1", "1_ru.wav");
        checkRes(resourceManager, "/numbers/male2/1", "1_ru.wav");
        checkRes(resourceManager, "/numbers/male/миллион", "миллион_ru.wav");
        checkRes(resourceManager, "/numbers/male/миллиона", "миллиона_ru.wav");
        checkRes(resourceManager, "/numbers/male/миллионов", "миллионов_ru.wav");
        checkRes(resourceManager, "/numbers/male2/миллион", "миллион_ru.wav");
        checkRes(resourceManager, "/numbers/male2/миллиона", "миллиона_ru.wav");
        checkRes(resourceManager, "/numbers/male2/миллионов", "миллионов_ru.wav");
    }
    
//    @Test
    public void numbersResourceTest() throws Exception {
        ResourceManager resourceManager = registry.getService(ResourceManager.class);
        Node resource = resourceManager.getResource(NUMBERS_FEMALE_RESOURCE, null);
        assertNotNull(resource);
    }
    
    public void checkRes(ResourceManager resourceManager, String path, String filename) throws Exception {
        AudioFileNode hello = (AudioFileNode) resourceManager.getResource(
                SOUNDS_RESOURCES_BASE+path, new Locale("ru"));
        assertNotNull(hello);
        DataFile file = hello.getAudioFile();
        assertNotNull(file);
        assertEquals(filename, file.getFilename());
        assertNotNull(file.getFileSize());
        assertTrue(file.getFileSize()>0);
    }
}
