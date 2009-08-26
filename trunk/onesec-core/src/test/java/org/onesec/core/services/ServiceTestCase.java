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

package org.onesec.core.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 *
 * @author Mikhail Titov
 */
public abstract class ServiceTestCase {
    protected Registry registry;
    protected String home;
        
    private String providersConfigFileName;

    public String getProvidersConfigFileName() {
        return providersConfigFileName;
    }

    public void setProvidersConfigFileName(String providersConfigFileName) {
        this.providersConfigFileName = providersConfigFileName;
    }

    @BeforeClass()
    protected void initRegistry() throws IOException
    {
        home = System.getProperty("user.home")+"/.onesec/";
        
        String[][] files = initConfigurationFiles();
        
        if (files!=null)
            for (String[] filesLink: files)
                FileUtils.copyFile(new File(home+filesLink[0]), new File(home+filesLink[1]));
        
        System.setProperty(ApplicationHome.ONESEC_HOME_SYSTEM_PROPERTY_NAME, home);
        
        RegistryBuilder builder = new RegistryBuilder();
        builder.add(OnesecCoreModule.class);
        
        registry = builder.build();
        registry.performRegistryStartup();
    }
    
    /**Метод вызывается перед созданием реестр
     * 
     * @param home каталог который будет возвращать сервис {@link ApplicationHome}
     */
    protected abstract String[][] initConfigurationFiles();
    
    @AfterClass
    protected void shutdownRegistry() {
        registry.shutdown();
    }
    
    protected Properties getProperties(String fileName) throws FileNotFoundException, IOException{
        Properties props = new Properties();
        FileInputStream is = new FileInputStream(home+fileName);
        props.load(is);
        return props;
    }

}
