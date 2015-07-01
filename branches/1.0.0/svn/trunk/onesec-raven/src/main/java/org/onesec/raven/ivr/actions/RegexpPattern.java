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
package org.onesec.raven.ivr.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.log.LogLevel;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=SayNumberActionNode.class)
public class RegexpPattern extends BaseNode implements Pattern {
    
    @NotNull @Parameter
    private String pattern;
    
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public Collection<String> matches(String str) {
        try {
            try {
                bindingSupport.enableScriptExecution();
                Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(str);
                if (!matcher.matches())
                    return null;
                if (matcher.groupCount()<1)
                    return Collections.EMPTY_LIST;
                ArrayList<String> groups = new ArrayList<String>(matcher.groupCount());
                for (int i=1; i<=matcher.groupCount(); ++i)
                    groups.add(matcher.group(i));
                return groups;
            } finally {
                bindingSupport.reset();
            }
        } catch (Exception e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("Pattern matches error", e);
            return null;
        }
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
