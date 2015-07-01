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
package org.onesec.core.impl;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.onesec.core.services.ApplicationHome;

/**
 *
 * @author Mikhail Titov
 */
public class ApplicationHomeImpl implements ApplicationHome {

    private File home;

    public ApplicationHomeImpl() {
        String onesecHome = System.getProperty(ONESEC_HOME_SYSTEM_PROPERTY_NAME);
        if (onesecHome==null)
            onesecHome = ".";
        home = new File(onesecHome);
        /*
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            for (URL url: ((URLClassLoader)classLoader).getURLs()) {
                home = url.toString();
                
                home = new File(home.substring(home.indexOf(':') + 1, home.lastIndexOf('/')))
                        .getAbsolutePath();
            }
        }else {
            home = new File(".").getAbsolutePath();
        }
        */

    }

    public File getHome() {
        return home;
    }

}
