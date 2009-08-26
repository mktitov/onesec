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

package org.onesec.server.helpers;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
//import org.onesec.core.services.ApplicationHome;
import org.apache.tapestry5.ioc.IOCUtilities;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;

/**
 *
 * @author Mikhail Titov
 */
public class StartServer {
    public static final String LIB_RELATIVE_PATH = "lib";
    
    private Registry registry;
    private Method shutdownRegistry;
//    private URLClassLoader classLoader;
    
    public void start() throws Exception 
    {
        RegistryBuilder builder = new RegistryBuilder();
        IOCUtilities.addDefaultModules(builder);
        
        registry = builder.build();
        
        registry.performRegistryStartup();
        
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        
    }
    
//    public void start() throws Exception {
//        
//        String home = detectHome();
//        System.setProperty("onesec.home", home);
////        String libdirname = 
////                System.getProperty(ApplicationHome.ONESEC_HOME_SYSTEM_PROPERTY_NAME) 
////                + File.separator + LIB_RELATIVE_PATH;
//        
//        File libdir = new File(home+File.separator+LIB_RELATIVE_PATH);
//        assert libdir.isDirectory();
//        File[] libs = libdir.listFiles();
//        assert libs!=null;
//        URL[] libURLs = new URL[libs.length];
//        for (int i=0; i<libs.length; ++i){
//            libURLs[i] = libs[i].toURI().toURL();
//        }
//        
//        URLClassLoader classLoader = new URLClassLoader(libURLs);
//        Class builderClass = classLoader.loadClass("org.apache.tapestry.ioc.RegistryBuilder");
//        Constructor initBuilder = builderClass.getConstructor(ClassLoader.class);
//        Method buildMethod = builderClass.getMethod("build");
//        Method addMethod = builderClass.getMethod("add", Class[].class);
//        
//        Class registryClass = classLoader.loadClass("org.apache.tapestry.ioc.Registry");
//        Method performRegistryStartup = registryClass.getMethod("performRegistryStartup");
//        shutdownRegistry = registryClass.getMethod("shutdown");
//        
//        Class iocUtilsClass = classLoader.loadClass("org.apache.tapestry.ioc.IOCUtilities");
//        
//        Method method = iocUtilsClass.getMethod("addDefaultModules", builderClass);
//        assert method!=null;
//        
//        Class coreModule = classLoader.loadClass("org.onesec.core.services.OnesecCoreModule");
//        Class serverModule = classLoader.loadClass("org.onesec.server.services.OnesecServerModule");
//        
//        Object builder = initBuilder.newInstance(classLoader);
//        method.invoke(null, builder);
//        addMethod.invoke(builder, coreModule, serverModule);
////        IOCUtilities.addDefaultModules(builder);
//        registry = buildMethod.invoke(builder);
//        
//        performRegistryStartup.invoke(registry);
//        
//        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
//    }
    
    public static void main(String[] args) throws Exception {
        StartServer starter = new StartServer();
        try{
            starter.start();
        }catch(Exception e){
            throw new Exception("FATAL ERROR. Error starting Server", e);
        }
    }

    private String detectHome() throws Exception {
        ClassLoader loader = this.getClass().getClassLoader();
        if (loader instanceof URLClassLoader) {
            URLClassLoader classLoader = (URLClassLoader)loader;
            for (URL url: (classLoader).getURLs()) {
                String strUrl = url.toString();
                if (strUrl.contains("onesec-server-")){
                    return strUrl.substring(strUrl.indexOf(':')+1, strUrl.lastIndexOf('/'));
                }
            }
            throw new Exception("Error find startup library.");
        }else
            throw new Exception(
                    "System error. " +
                    "Class loader is not instance of URLClassLoader");
    }
    
    private class ShutdownHook extends Thread {

        @Override
        public void run() {
            try{
                registry.shutdown();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        
    }
    
}
