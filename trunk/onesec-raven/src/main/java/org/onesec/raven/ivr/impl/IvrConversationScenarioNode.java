/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.ivr.impl;

import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.actions.PauseActionNode;
import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import org.onesec.raven.ivr.actions.StopConversationActionNode;
import org.raven.annotations.NodeClass;
import org.raven.conv.impl.ConversationScenarioNode;
import org.raven.conv.impl.ConversationScenarioPointNode;
import org.raven.conv.impl.GotoNode;
import org.raven.expr.impl.IfNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes={
    ConversationScenarioPointNode.class, IfNode.class, GotoNode.class,
    StopConversationActionNode.class, PlayAudioActionNode.class, PauseActionNode.class})
public class IvrConversationScenarioNode extends ConversationScenarioNode
        implements IvrConversationScenario
{
}
