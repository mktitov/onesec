/*
 *  Copyright 2007 Mikhail Titov.
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

package org.onesec.core;

import org.onesec.core.services.ApplicationHome;
import java.io.File;
import org.onesec.core.impl.ApplicationHomeImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author Mikhail Titov
 */
@Test
public class ApplicationHomeTest extends Assert 
{
    public void test() {
        System.setProperty(ApplicationHome.ONESEC_HOME_SYSTEM_PROPERTY_NAME, "home_path");
        ApplicationHome home = new ApplicationHomeImpl();
        assertEquals(home.getHome(), new File("home_path"));
    }
}
