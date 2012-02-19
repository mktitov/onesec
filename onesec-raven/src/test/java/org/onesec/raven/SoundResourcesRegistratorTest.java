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
import org.raven.tree.DataFileException;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public class SoundResourcesRegistratorTest extends OnesecRavenTestCase implements Constants {
    
    @Test
    public void test() throws DataFileException {
        ResourceManager resourceManager = registry.getService(ResourceManager.class);
        assertNotNull(resourceManager);
        AudioFileNode hello = (AudioFileNode) resourceManager.getResource(
                SOUNDS_RESOURCES_BASE+"/hello", new Locale("ru"));
        assertNotNull(hello);
        DataFile file = hello.getAudioFile();
        assertNotNull(file);
        assertNotNull("hello_ru.wav", file.getFilename());
        assertNotNull(file.getFileSize());
        assertTrue(file.getFileSize()>0);
    }
}
