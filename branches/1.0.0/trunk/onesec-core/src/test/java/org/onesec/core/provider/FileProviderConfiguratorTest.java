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

package org.onesec.core.provider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import org.onesec.core.StateWaitResult;
import org.onesec.core.impl.ProviderConfiguratorListenersImpl;
import org.onesec.core.provider.impl.FileProviderConfigurator;
import org.onesec.core.provider.impl.ProviderConfigurationImpl;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;
import static org.onesec.core.provider.ProviderConfiguratorState.*;

/**
 *
 * @author Mikhail Titov
 */
@Test(sequential=true)
public class FileProviderConfiguratorTest 
{

    public void test_simpleRead() throws Throwable 
    {
        ProviderConfiguratorListener listener = createMocksFor_test_simpleRead();
        
        FileProviderConfigurator configurator = new FileProviderConfigurator(
                "providers1.cfg"
                , new File("src/test/conf")
                , new ProviderConfiguratorListenersImpl(Arrays.asList(listener))
                , LoggerFactory.getLogger(this.getClass()));
        try{
            ProviderConfiguratorState state = configurator.getState();

            StateWaitResult<ProviderConfiguratorState> waitResult = 
                    state.waitForState(new int[]{CONFIGURATION_UPDATED, ERROR}, 2000);

            assertFalse(waitResult.isWaitInterrupted());

            if (state.getId()==ERROR)
                throw state.getErrorException();

            verify(listener);
        }finally{
            configurator.shutdown();
        }
    }
    
    public void test_idUnique() {
        ProviderConfiguratorListener listener = createMock(ProviderConfiguratorListener.class);
        replay(listener);
        
        FileProviderConfigurator configurator = new FileProviderConfigurator(
                "providers3.cfg"
                , new File("src/test/conf")
                , new ProviderConfiguratorListenersImpl(Arrays.asList(listener))
                , LoggerFactory.getLogger(this.getClass()));
        try{

            ProviderConfiguratorState state = configurator.getState();

            StateWaitResult<ProviderConfiguratorState> waitResult = 
                    state.waitForState(new int[]{ERROR}, 500);

            assertFalse(waitResult.isWaitInterrupted());

            assertNotNull(state.getErrorException());
            assertNotNull(state.getErrorMessage());
            assertTrue(state.getErrorMessage().contains(
                        "Invalid provider id at line 4. Provider with id (1) is already exists"));

            verify(listener);
        }finally{
            configurator.shutdown();
        }
    }
    
    public void test_numberRangeIntersection() 
    {
        ProviderConfiguratorListener listener = createMock(ProviderConfiguratorListener.class);
        replay(listener);
        
        FileProviderConfigurator configurator = new FileProviderConfigurator(
                "providers2.cfg"
                , new File("src/test/conf")
                , new ProviderConfiguratorListenersImpl(Arrays.asList(listener))
                , LoggerFactory.getLogger(this.getClass()));
        try{
            ProviderConfiguratorState state = configurator.getState();

            StateWaitResult<ProviderConfiguratorState> waitResult = 
                    state.waitForState(new int[]{ERROR}, 500);

            assertFalse(waitResult.isWaitInterrupted());

            assertNotNull(state.getErrorException());
            assertNotNull(state.getErrorMessage());
            assertTrue(state.getErrorMessage()
                    .contains("Addresses range intersection detected at line (2)"));

            verify(listener);
        }finally{
            configurator.shutdown();
        }
    }
    
    public void test_addNewLineToConfig() throws Exception, Throwable {
        ProviderConfiguratorListener listener = createMockFor_test_addNewLineToConfig();
        String filename = "src/test/conf/providers4.cfg";
        File file = new File(filename);
        appendToFile(file.getAbsolutePath(), "1 name1 1  10 user1 pwd1 host1", false);
        
        FileProviderConfigurator configurator = new FileProviderConfigurator(
                "providers4.cfg"
                , new File("src/test/conf")
                , new ProviderConfiguratorListenersImpl(Arrays.asList(listener))
                , LoggerFactory.getLogger(this.getClass()));        
        
        try{

            ProviderConfiguratorState state = configurator.getState();

            StateWaitResult<ProviderConfiguratorState> waitResult = 
                    state.waitForState(new int[]{CONFIGURATION_UPDATED}, 500);

            assertFalse(waitResult.isWaitInterrupted());

            appendToFile(file.getAbsolutePath(), "2 name2 11 20 user2 pwd2 host2", true);

            waitResult = state.waitForNewState(
                    waitResult.getState(), FileProviderConfigurator.DEFAULT_CONFIG_CHECK_TIME+500);
            
            assertFalse(waitResult.isWaitInterrupted());

            verify(listener);
        }finally{
            configurator.shutdown();
        }
    }
    
    public void test_removeLineFromConfig() throws Exception {
        ProviderConfiguratorListener listener = createMockFor_test_removeLineFromConfig();
        String filename = "src/test/conf/providers4.cfg";
        appendToFile(filename, "1 name1 1  10 user1 pwd1 host1", false);
        appendToFile(filename, "2 name2 11 20 user2 pwd2 host2", true);
        
        FileProviderConfigurator configurator = new FileProviderConfigurator(
                "providers4.cfg"
                , new File("src/test/conf")
                , new ProviderConfiguratorListenersImpl(Arrays.asList(listener))
                , LoggerFactory.getLogger(this.getClass()));        
        
        try{

            ProviderConfiguratorState state = configurator.getState();

            StateWaitResult<ProviderConfiguratorState> waitResult = 
                    state.waitForState(new int[]{CONFIGURATION_UPDATED}, 500);

            assertFalse(waitResult.isWaitInterrupted());

            appendToFile(filename, "1 name1 1  10 user1 pwd1 host1", false);

            waitResult = state.waitForNewState(
                    waitResult.getState(), FileProviderConfigurator.DEFAULT_CONFIG_CHECK_TIME+500);
            
            assertFalse(waitResult.isWaitInterrupted());

            verify(listener);
        }finally{
            configurator.shutdown();
        }
    }
    
    public void test_updateLineInConfig() throws Exception{
        ProviderConfiguratorListener listener = createMockFor_test_updateLineInConfig();
        String filename = "src/test/conf/providers4.cfg";
        appendToFile(filename, "1 name1 1  10 user1 pwd1 host1", false);
        appendToFile(filename, "2 name2 11 20 user2 pwd2 host2", true);
        
        FileProviderConfigurator configurator = new FileProviderConfigurator(
                "providers4.cfg"
                , new File("src/test/conf")
                , new ProviderConfiguratorListenersImpl(Arrays.asList(listener))
                , LoggerFactory.getLogger(this.getClass()));        
        
        try{

            ProviderConfiguratorState state = configurator.getState();

            StateWaitResult<ProviderConfiguratorState> waitResult = 
                    state.waitForState(new int[]{CONFIGURATION_UPDATED}, 500);

            assertFalse(waitResult.isWaitInterrupted());

            appendToFile(filename, "1 name1 1  10 user1 pwd1 host1", false);
            appendToFile(filename, "2 name2 11 20 user2 pwd2 host3", true);

            waitResult = state.waitForNewState(
                    waitResult.getState(), FileProviderConfigurator.DEFAULT_CONFIG_CHECK_TIME+500);
            
            assertFalse(waitResult.isWaitInterrupted());

            verify(listener);
        }finally{
            configurator.shutdown();
        }
    }
    
    private void appendToFile(String fileName, String content, boolean append) throws IOException {        
        PrintWriter writer = new PrintWriter(new FileOutputStream(fileName, append));
        writer.println(content);
        writer.close();
    }

    private ProviderConfiguratorListener createMockFor_test_addNewLineToConfig() {
        return createMocksFor_test_simpleRead();
        
    }

    private ProviderConfiguratorListener createMockFor_test_removeLineFromConfig() {
        ProviderConfiguratorListener listener = createMock(ProviderConfiguratorListener.class);
        
        listener.providerAdded((ProviderConfiguration)anyObject());
        expectLastCall().times(2);
        
        ProviderConfiguration conf = new ProviderConfigurationImpl(
                2, "name2", 11, 20, "user2", "pwd2", "host2");
        listener.providerRemoved(conf);
        
        replay(listener);
        
        return listener;
    }

    private ProviderConfiguratorListener createMockFor_test_updateLineInConfig() {
        ProviderConfiguratorListener listener = createMock(ProviderConfiguratorListener.class);
        
        listener.providerAdded((ProviderConfiguration)anyObject());
        expectLastCall().times(2);
        
        ProviderConfiguration conf = new ProviderConfigurationImpl(
                2, "name2", 11, 20, "user2", "pwd2", "host3");
        listener.providerUpdated(conf);
        
        replay(listener);
        
        return listener;
    }
    
    private ProviderConfiguratorListener createMocksFor_test_simpleRead() 
    {
        ProviderConfiguratorListener listener = createMock(ProviderConfiguratorListener.class);
        
        ProviderConfiguration conf = 
                new ProviderConfigurationImpl(1, "name1", 1, 10, "user1", "pwd1", "host1");
        listener.providerAdded(conf);
        
        conf = new ProviderConfigurationImpl(2, "name2", 11, 20, "user2", "pwd2", "host2");
        listener.providerAdded(conf);
        
        replay(listener);
        
        return listener;
    }
    
}
