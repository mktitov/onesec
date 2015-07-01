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

package org.onesec.core.provider.impl;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.tapestry5.ioc.services.RegistryShutdownListener;
import org.onesec.core.provider.ProviderConfiguration;
import org.onesec.core.provider.ProviderConfiguratorListener;
import org.onesec.core.provider.ProviderConfiguratorState;
import org.onesec.core.services.ProviderConfigurator;
import org.onesec.core.services.ProviderConfiguratorListeners;
import org.slf4j.Logger;
import static org.onesec.core.provider.ProviderConfiguratorState.*;
/**
 *
 * @author Mikhail Titov
 */
public class FileProviderConfigurator implements ProviderConfigurator, RegistryShutdownListener
{
    public final static String OBJECT_NAME = "Provider configurator";
    public final static String OBJECT_DESCRIPTION = "";
    
    public final static long DEFAULT_CONFIG_CHECK_TIME = 5000;
    
    public final static String CONFIG_FILE_NAME="providers.cfg";

    public enum EventType {ADD, REMOVE, UPDATE};
    
    private File configFile;
    private Collection<ProviderConfiguratorListener> listeners;
    private FileWatchdog fileWatchdog;
    
    private final Logger logger;
    
    private final ProviderConfiguratorStateImpl state;
    
    private Map<Integer, ProviderConfiguration> configurations =
            new ConcurrentHashMap<Integer, ProviderConfiguration>();
    
    private ExecutorService executor = Executors.newFixedThreadPool(1);

    public FileProviderConfigurator(
            String configFileName, File configFileDir
            , ProviderConfiguratorListeners providerConfiguratorListeners
            , Logger logger) 
    {
        this.logger = logger;
        configFile = new File(configFileDir.getAbsolutePath()+File.separator+configFileName);
        this.listeners = providerConfiguratorListeners.getListeners();
        
        state = new ProviderConfiguratorStateImpl(this);
        state.setState(READY);
        
        fileWatchdog = new FileWatchdog(configFile, DEFAULT_CONFIG_CHECK_TIME);
        executor.execute(fileWatchdog);
    }
    
    public FileProviderConfigurator(
            File configFileDir, ProviderConfiguratorListeners providerConfiguratorListeners
            , Logger logger)
    {
        this(CONFIG_FILE_NAME, configFileDir, providerConfiguratorListeners, logger);
    }

    public void start() {
    }
    
    public Collection<ProviderConfiguration> getAll() {
        return configurations.values();
    }

    public ProviderConfiguratorState getState() {
        return state;
    }
    
    public void add(ProviderConfiguration configuration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void remove(ProviderConfiguration configuration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void update(ProviderConfiguration configuration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private void rereadConfiguration() {
        try {
            state.setState(CONFIGURATION_UPDATING);

            List<String> lines = FileUtils.readLines(configFile);

            Map<Integer, ProviderConfiguration> newConfigurations =
                    new HashMap<Integer, ProviderConfiguration>();

            int i=0;
            for (String line: lines) {
                ++i;
                String elems[] = line.split("\\s+");

                if (line.trim().length()==0 || elems[0].startsWith("#"))
                    continue;

                if (elems.length!=7)
                    throw new Exception(String.format(
                            "Invalid number of columns (%d) at line (%d)", elems.length, i));

                int id = decodeId(elems[0], i);
                if (newConfigurations.containsKey(id))
                    throw new Exception(String.format(
                            "Invalid provider id at line %d. " +
                            "Provider with id (%d) is already exists"
                            , i, id));
                String name = elems[1];
                int fromNumber = decodeInt(elems[2], i, "first");
                int toNumber = decodeInt(elems[3],i, "last");
                for (ProviderConfiguration conf: newConfigurations.values())
                    if (   (fromNumber>=conf.getFromNumber() && fromNumber<=conf.getToNumber())
                        || (toNumber>=conf.getFromNumber() && toNumber<=conf.getToNumber())) 
                    {
                        throw new Exception(String.format(
                                "Addresses range intersection detected at line (%d). " +
                                "Counter part configuration is '%s'"
                                , i, conf.toString()));
                    }
                String user = elems[4];
                String password = elems[5];
                String host = elems[6];

                ProviderConfigurationImpl config = new ProviderConfigurationImpl(
                        id, name, fromNumber, toNumber, user, password, host);
                newConfigurations.put(config.getId(), config);

            }

            applyNewConfiguration(newConfigurations);
            
            state.setState(CONFIGURATION_UPDATED);
            
        } catch (Exception ex) {
            state.setState(
                    ERROR
                    , String.format(
                        "Error read config file (%s). %s"
                        , configFile.getAbsolutePath(), ex.getMessage())
                    , ex);
        }
    }

    private void applyNewConfiguration(Map<Integer, ProviderConfiguration> newConfigurations) {
        for (ProviderConfiguration conf: newConfigurations.values()) {
            ProviderConfiguration oldConf = configurations.get(conf.getId());
            if (oldConf!=null && !oldConf.equals(conf)){
                configurations.put(conf.getId(), conf);
                fireNewEvent(conf, EventType.UPDATE);
            }else if (oldConf==null) {
                configurations.put(conf.getId(), conf);
                fireNewEvent(conf, EventType.ADD);
            }
        }
        for (ProviderConfiguration conf: configurations.values())
            if (!newConfigurations.containsKey(conf.getId())) {
                configurations.remove(conf);
                fireNewEvent(conf, EventType.REMOVE);
            }
    }

    private void fireNewEvent(ProviderConfiguration conf, EventType type) {
        for (ProviderConfiguratorListener listener: listeners)
            switch(type) {
                case ADD : listener.providerAdded(conf); break;
                case REMOVE : listener.providerRemoved(conf); break;
                case UPDATE : listener.providerUpdated(conf); break;
            }
    }
    
    private int decodeId(String strId, int i) throws Exception
    {
        try {
            int id = Integer.valueOf(strId);
            return id;
        } catch (NumberFormatException e) {
            throw new Exception(String.format(
                    "Invalid provider id (%d) at line (%d)", strId, i));
        }
    }

    private int decodeInt(String strInt, int i, String message) throws Exception 
    {
        try{
            return Integer.valueOf(strInt);
        } catch (NumberFormatException e) {
            throw new Exception(String.format(
                    "Invalid %s address in addresses range id (%d) at line (%d)"
                    , message, strInt, i));
        }
    }
        
//    private class ConfigFileWatchdog extends FileWatchdog {

//        public ConfigFileWatchdog(String filename) {
//            super(filename);
//            setDelay(10);
//        }
//
//        @Override
//        protected void doOnChange() {
//            rereadConfiguration();
//        }

//    }
    private class FileWatchdog implements Runnable {
        private File file;
        private long delay;
        private long lastModified = 0;

        public FileWatchdog(File file, long delay) {
            this.file = file;
            this.delay = delay;
        }

        public void run() {
            try {
                while (true) {
                    if (file.exists() && file.lastModified() != lastModified) {
                        rereadConfiguration();
                    }
                    TimeUnit.MILLISECONDS.sleep(delay);
                }
            } catch (InterruptedException ex) {
                //
            }
        }
        
    }

    public void shutdown() {
        executor.shutdownNow();
        state.setState(ProviderConfiguratorState.STOPED);
    }

    public String getObjectName() {
        return OBJECT_NAME;
    }

    public String getObjectDescription() {
        return OBJECT_DESCRIPTION;
    }

    public void registryDidShutdown() {
        shutdown();
    }
    
    

}
