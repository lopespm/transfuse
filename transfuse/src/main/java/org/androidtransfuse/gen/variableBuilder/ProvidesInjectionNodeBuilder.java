/**
 * Copyright 2012 John Ericksen
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
package org.androidtransfuse.gen.variableBuilder;

import com.google.inject.assistedinject.Assisted;
import org.androidtransfuse.analysis.AnalysisContext;
import org.androidtransfuse.analysis.Analyzer;
import org.androidtransfuse.analysis.adapter.ASTMethod;
import org.androidtransfuse.analysis.adapter.ASTParameter;
import org.androidtransfuse.analysis.adapter.ASTType;
import org.androidtransfuse.model.InjectionNode;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author John Ericksen
 */
public class ProvidesInjectionNodeBuilder extends InjectionNodeBuilderNoAnnotationAdapter {

    private final ASTType moduleType;
    private final ASTMethod providesMethod;
    private final Analyzer analyzer;
    private final VariableInjectionBuilderFactory variableInjectionBuilderFactory;

    @Inject
    public ProvidesInjectionNodeBuilder(@Assisted ASTType moduleType,
                                        @Assisted ASTMethod providesMethod,
                                        Analyzer analyzer,
                                        VariableInjectionBuilderFactory variableInjectionBuilderFactory) {
        this.moduleType = moduleType;
        this.providesMethod = providesMethod;
        this.analyzer = analyzer;
        this.variableInjectionBuilderFactory = variableInjectionBuilderFactory;
    }


    @Override
    public InjectionNode buildInjectionNode(ASTType astType, AnalysisContext context) {
        InjectionNode injectionNode = new InjectionNode(astType);

        InjectionNode module = analyzer.analyze(moduleType, moduleType, context);

        Map<ASTParameter, InjectionNode> dependencyAnalysis = new HashMap<ASTParameter, InjectionNode>();

        for (ASTParameter parameter : providesMethod.getParameters()) {
            InjectionNode parameterInjectionNode = analyzer.analyze(parameter.getASTType(), parameter.getASTType(), context);

            dependencyAnalysis.put(parameter, parameterInjectionNode);
        }

        injectionNode.addAspect(VariableBuilder.class, variableInjectionBuilderFactory.buildProvidesVariableBuilder(module, providesMethod, dependencyAnalysis));

        return injectionNode;
    }
}