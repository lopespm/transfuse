/**
 * Copyright 2013 John Ericksen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.androidtransfuse.gen;

import org.androidtransfuse.analysis.astAnalyzer.ASTInjectionAspect;
import org.androidtransfuse.gen.variableDecorator.VariableExpressionBuilder;
import org.androidtransfuse.model.FieldInjectionPoint;
import org.androidtransfuse.model.InjectionNode;
import org.androidtransfuse.model.MethodInjectionPoint;
import org.androidtransfuse.model.TypedExpression;

import javax.inject.Inject;

/**
 * @author John Ericksen
 */
public class InjectionExpressionBuilder {

    private VariableExpressionBuilder expressionDecorator;

    public TypedExpression buildVariable(InjectionBuilderContext injectionBuilderContext, InjectionNode injectionNode) {
        return expressionDecorator.buildVariableExpression(injectionBuilderContext, injectionNode);
    }

    public void setupInjectionRequirements(InjectionBuilderContext injectionBuilderContext, InjectionNode injectionNode) {
        ASTInjectionAspect injectionAspect = injectionNode.getAspect(ASTInjectionAspect.class);
        if (injectionAspect != null) {
            //constructor injection
            if(injectionAspect.getConstructorInjectionPoint() != null){
                for (InjectionNode constructorNode : injectionAspect.getConstructorInjectionPoint().getInjectionNodes()) {
                    buildVariable(injectionBuilderContext, constructorNode);
                }
            }
            for (ASTInjectionAspect.InjectionGroup injectionGroup : injectionAspect.getGroups()) {
                //field injection
                for (FieldInjectionPoint fieldInjectionPoint : injectionGroup.getFieldInjectionPoints()) {
                    buildVariable(injectionBuilderContext, fieldInjectionPoint.getInjectionNode());
                }
                //method injection
                for (MethodInjectionPoint methodInjectionPoint : injectionGroup.getMethodInjectionPoints()) {
                    for (InjectionNode methodNode : methodInjectionPoint.getInjectionNodes()) {
                        buildVariable(injectionBuilderContext, methodNode);
                    }
                }
            }
        }
    }

    @Inject
    public void setExpressionDecorator(VariableExpressionBuilder expressionDecorator) {
        this.expressionDecorator = expressionDecorator;
    }
}
