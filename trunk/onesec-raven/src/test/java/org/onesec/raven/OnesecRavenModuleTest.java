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
package org.onesec.raven;

import org.junit.Test;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class OnesecRavenModuleTest extends OnesecRavenTestCase {
    
    @Test
    public void vmailTemplatesTest() throws Exception {
        Node vmailTemplates = tree.getRootNode().getNodeByPath("Templates/IVR/VMail");
        assertNotNull(vmailTemplates);
        assertNotNull(vmailTemplates.getNode("Recording scenario"));
        assertNotNull(vmailTemplates.getNode("Listening scenario"));
    }
}
