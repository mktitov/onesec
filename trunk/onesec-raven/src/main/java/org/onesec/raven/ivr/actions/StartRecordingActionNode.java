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
package org.onesec.raven.ivr.actions;

import java.util.Collection;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.cache.TemporaryFileManager;
import org.raven.cache.TemporaryFileManagerValueHandlerFactory;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class StartRecordingActionNode extends BaseNode implements IvrActionNode, DataSource {
    
    @NotNull @Parameter(valueHandlerType=TemporaryFileManagerValueHandlerFactory.TYPE)
    private TemporaryFileManager temporaryFileManager;
    @NotNull @Parameter(defaultValue="1")
    private Integer noiseLevel;
    @NotNull @Parameter(defaultValue="2.0")
    private Double maxGainCoef;

    public Integer getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(Integer noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public Double getMaxGainCoef() {
        return maxGainCoef;
    }

    public void setMaxGainCoef(Double maxGainCoef) {
        this.maxGainCoef = maxGainCoef;
    }

    public TemporaryFileManager getTemporaryFileManager() {
        return temporaryFileManager;
    }

    public void setTemporaryFileManager(TemporaryFileManager temporaryFileManager) {
        this.temporaryFileManager = temporaryFileManager;
    }

    public IvrAction createAction() {
        return new StartRecordingAction(this);
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("DataSource work in PUSH mode only");
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }
    
    void sendDataToConsumers(Object data, DataContext context) {
        DataSourceHelper.sendDataToConsumers(this, data, context);
    }
}
