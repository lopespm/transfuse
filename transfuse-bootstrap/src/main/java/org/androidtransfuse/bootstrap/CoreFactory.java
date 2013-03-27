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
package org.androidtransfuse.bootstrap;

import com.google.common.collect.ImmutableSet;
import com.sun.codemodel.*;
import org.androidtransfuse.adapter.*;
import org.androidtransfuse.adapter.classes.ASTClassFactory;
import org.androidtransfuse.adapter.classes.LazyClassParameterBuilder;
import org.androidtransfuse.adapter.element.*;
import org.androidtransfuse.analysis.AnalysisContext;
import org.androidtransfuse.analysis.AnalysisContextFactory;
import org.androidtransfuse.analysis.Analyzer;
import org.androidtransfuse.analysis.InjectionPointFactory;
import org.androidtransfuse.analysis.astAnalyzer.InjectionAnalyzer;
import org.androidtransfuse.analysis.astAnalyzer.ScopeAnalysis;
import org.androidtransfuse.analysis.module.*;
import org.androidtransfuse.analysis.repository.AnalysisRepository;
import org.androidtransfuse.analysis.repository.InjectionNodeBuilderRepository;
import org.androidtransfuse.gen.*;
import org.androidtransfuse.gen.componentBuilder.InjectionNodeImplFactory;
import org.androidtransfuse.gen.componentBuilder.MirroredMethodGeneratorFactory;
import org.androidtransfuse.gen.invocationBuilder.*;
import org.androidtransfuse.gen.proxy.AOPProxyGenerator;
import org.androidtransfuse.gen.proxy.VirtualProxyGenerator;
import org.androidtransfuse.gen.scopeBuilder.CustomScopeAspectFactoryFactory;
import org.androidtransfuse.gen.scopeBuilder.SingletonScopeAspectFactory;
import org.androidtransfuse.gen.scopeBuilder.SingletonScopeBuilder;
import org.androidtransfuse.gen.variableBuilder.*;
import org.androidtransfuse.gen.variableDecorator.*;
import org.androidtransfuse.model.InjectionNode;
import org.androidtransfuse.model.TypedExpression;
import org.androidtransfuse.scope.ConcurrentDoubleLockingScope;
import org.androidtransfuse.util.MessagerLogger;
import org.androidtransfuse.util.Providers;
import org.androidtransfuse.util.QualifierPredicate;
import org.androidtransfuse.util.ScopesPredicate;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * @author John Ericksen
 */
public class CoreFactory {

    private final Elements elements;
    private final JCodeModel codeModel = new JCodeModel();
    private final VirtualProxyGenerator.VirtualProxyGeneratorCache virtualProxyCache;
    private final ASTClassFactory astClassFactory = new ASTClassFactory(new ConcreteASTFactory());
    private final TypedExpressionFactory typedExpressionFactory = new TypedExpressionFactory(astClassFactory);
    private final UniqueVariableNamer namer = new UniqueVariableNamer();
    private final ClassGenerationUtil generationUtil;
    private final ProviderGenerator.ProviderCache providerCache = new ProviderGenerator.ProviderCache();
    private final Filer filer;
    private final ModuleRepositoryImpl moduleRepository = new ModuleRepositoryImpl();

    private BootstrapsInjectorGenerator bootstrapsInjectorGenerator = null;

    public CoreFactory(Elements elements, Messager messager, Filer filer) {
        this.elements = elements;
        this.filer = filer;
        this.generationUtil = new ClassGenerationUtil(codeModel, new MessagerLogger(messager), namer);
        this.virtualProxyCache = new VirtualProxyGenerator.VirtualProxyGeneratorCache(namer, generationUtil);
        this.moduleRepository.addModuleRepository(buildScopeRepository());
    }

    public ASTElementConverterFactory buildConverterFactory() {
        ConcreteASTFactory astFactory = new ConcreteASTFactory();
        ASTElementFactoryProvider astElementFactoryProvider = new ASTElementFactoryProvider();
        ASTTypeBuilderVisitor astTypeBuilderVisitor = new ASTTypeBuilderVisitor(astElementFactoryProvider);
        ElementConverterFactory elementConverterFactory =
                new ElementConverterFactory(astTypeBuilderVisitor, astElementFactoryProvider, astFactory);

        ASTElementConverterFactory astElementConverterFactory = new ASTElementConverterFactory(elementConverterFactory);

        //wire lazy injections
        astFactory.setElementConverterFactory(elementConverterFactory);
        astFactory.setAstElementFactoryProvider(astElementFactoryProvider);
        astElementFactoryProvider.setAstElementConverterFactory(astElementConverterFactory);
        astElementFactoryProvider.setAstFactory(astFactory);
        astElementFactoryProvider.setAstTypeBuilderVisitor(astTypeBuilderVisitor);

        return astElementConverterFactory;
    }

    public InjectionPointFactory buildInjectionPointFactory() {
        QualifierPredicate qualifierPredicate = new QualifierPredicate(astClassFactory);

        return new InjectionPointFactory(astClassFactory, qualifierPredicate,
                new VariableInjectionNodeBuilder(buildAnalyser(),
                buildVariableInjectionBuilder()),
                new GeneratedProviderInjectionNodeBuilderProvider());
    }

    private final class GeneratedProviderInjectionNodeBuilderProvider implements Provider<GeneratedProviderInjectionNodeBuilder>{

        @Override
        public GeneratedProviderInjectionNodeBuilder get() {
            return buildGeneratedProviderInjectionNodeBuilder();
        }
    }

    private VariableInjectionBuilder buildVariableInjectionBuilder(){
        AOPProxyGenerator aopProxyGenerator = new AOPProxyGenerator(codeModel, namer, generationUtil);
        InjectionExpressionBuilder injectionExpressionBuilder = new InjectionExpressionBuilder();
        injectionExpressionBuilder.setExpressionDecorator(new ExpressionDecoratorFactory(new ConcreteVariableExpressionBuilderFactory()).get());
        ExceptionWrapper exceptionWrapper = new ExceptionWrapper(codeModel);
        GeneratorFactory2 generatorFactory = new GeneratorFactory2(Providers.of(new TypeInvocationHelper(codeModel, astClassFactory)));

        return new VariableInjectionBuilder(
                        codeModel,
                        namer,
                        buildInvocationBuilder(),
                        aopProxyGenerator,
                        injectionExpressionBuilder,
                        typedExpressionFactory,
                        exceptionWrapper,
                        generatorFactory);
    }

    private Analyzer buildAnalyser(){
        Analyzer analyzer = new Analyzer();
        analyzer.setVariableInjectionBuilderProvider(Providers.of(buildVariableInjectionBuilder()));

        return analyzer;
    }

    public AnalysisContext buildAnalysisContext() {
        return new AnalysisContext(buildInjectionNodeRepository(), buildAnalysisRepository());
    }

    private Provider<InjectionNodeBuilderRepository> buildInjectionNodeRepositoryProvider(){
        return new Provider<InjectionNodeBuilderRepository>() {
            @Override
            public InjectionNodeBuilderRepository get() {
                return buildInjectionNodeRepository();
            }
        };
    }

    private InjectionNodeBuilderRepository buildInjectionNodeRepository(){
        return moduleRepository.buildModuleConfiguration();
    }

    private GeneratedProviderInjectionNodeBuilder buildGeneratedProviderInjectionNodeBuilder(){

        Provider<ProviderGenerator> providerGeneratorProvider = new Provider<ProviderGenerator>() {
            @Override
            public ProviderGenerator get() {
                return buildProviderGenerator();
            }
        };

        GeneratedProviderBuilderFactory generatedProviderBuilderFactory = new GeneratedProviderBuilderFactory(providerGeneratorProvider,
                Providers.of(namer), Providers.of(typedExpressionFactory));

        return new GeneratedProviderInjectionNodeBuilder(generatedProviderBuilderFactory, buildInjectionPointFactory(), buildAnalyser());
    }

    private ProviderGenerator buildProviderGenerator(){
        return new ProviderGenerator(providerCache, codeModel, buildInjectionGenerator(), generationUtil, namer);
    }

    private AnalysisRepository buildAnalysisRepository(){
        AnalysisRepository analysisRepository = new AnalysisRepository();

        analysisRepository.addAnalysis(new InjectionAnalyzer(buildInjectionPointFactory()));
        analysisRepository.addAnalysis(new ScopeAnalysis());

        return analysisRepository;
    }

    private InjectionNodeBuilderRepository buildScopeRepository(){

        InjectionNodeBuilderRepository scopeRepository = new InjectionNodeBuilderRepository(astClassFactory);

        SingletonScopeBuilder singletonScopeBuilder = new SingletonScopeBuilder(codeModel, new ProviderGenerator(providerCache, codeModel, buildInjectionGenerator(), generationUtil, namer), typedExpressionFactory, namer);
        scopeRepository.putScopeAspectFactory(astClassFactory.getType(Singleton.class), astClassFactory.getType(ConcurrentDoubleLockingScope.class), new SingletonScopeAspectFactory(Providers.of(singletonScopeBuilder)));
        scopeRepository.putScopeAspectFactory(astClassFactory.getType(BootstrapModule.class), astClassFactory.getType(ConcurrentDoubleLockingScope.class), new SingletonScopeAspectFactory(Providers.of(singletonScopeBuilder)));

        return scopeRepository;
    }

    private InvocationBuilder buildInvocationBuilder(){
        return new InvocationBuilder(new InvocationBuilderStrategy() {
            @Override
            public ModifierInjectionBuilder getInjectionBuilder(ASTAccessModifier modifier) {
                if(modifier.equals(ASTAccessModifier.PUBLIC)){
                    return new PublicInjectionBuilder(new TypeInvocationHelper(codeModel, astClassFactory));
                }
                return new PrivateInjectionBuilder(codeModel);
            }
        });
    }

    public JCodeModel getCodeModel() {
        return codeModel;
    }

    public CodeWriter buildCodeWriter() {
        return new FilerSourceCodeWriter(filer);
    }

    public CodeWriter buildResourceWriter(){
        return new FilerResourceWriter(filer);
    }

    private InjectionFragmentGenerator buildInjectionGenerator(){
        InjectionBuilderContextFactory injectionBuilderContextFactory = new InjectionBuilderContextFactoryImpl();
        InjectionExpressionBuilder injectionExpressionBuilder = new InjectionExpressionBuilder();
        injectionExpressionBuilder.setExpressionDecorator(new ExpressionDecoratorFactory(new ConcreteVariableExpressionBuilderFactory()).get());
        VirtualProxyGenerator virtualProxyGenerator = new VirtualProxyGenerator(codeModel, namer, astClassFactory, generationUtil, virtualProxyCache);

        return new InjectionFragmentGenerator(injectionBuilderContextFactory, injectionExpressionBuilder, virtualProxyGenerator);
    }

    public synchronized BootstrapsInjectorGenerator buildBootstrapsInjectorGenerator() {
        if(bootstrapsInjectorGenerator == null){
            InjectionExpressionBuilder injectionExpressionBuilder = new InjectionExpressionBuilder();
            injectionExpressionBuilder.setExpressionDecorator(new ExpressionDecoratorFactory(new ConcreteVariableExpressionBuilderFactory()).get());
            ExistingVariableInjectionBuilderFactory variableBuilderFactory = new ExistingVariableInjectionBuilderFactory(
                    buildInvocationBuilder(),
                    injectionExpressionBuilder,
                    typedExpressionFactory,
                    new ExceptionWrapper(codeModel),
                    new GeneratorFactory2(Providers.of(new TypeInvocationHelper(codeModel, astClassFactory))));
            this.bootstrapsInjectorGenerator = new BootstrapsInjectorGenerator(codeModel, generationUtil, namer, buildInjectionGenerator(), variableBuilderFactory, buildInjectionNodeRepository().getScopeAnnotations());
        }
        return bootstrapsInjectorGenerator;
    }

    public ModuleProcessor buildModuleProcessor() {

        VariableASTImplementationFactory variableASTImplementationFactory = new VariableASTImplementationFactory(buildAnalyser(), Providers.of(buildVariableInjectionBuilder()));

        InjectionExpressionBuilder injectionExpressionBuilder = new InjectionExpressionBuilder();
        injectionExpressionBuilder.setExpressionDecorator(new ExpressionDecoratorFactory(new ConcreteVariableExpressionBuilderFactory()).get());

        ProvidesVariableBuilderFactory providesVariableBuilderFactory = new ProvidesVariableBuilderFactory(injectionExpressionBuilder, typedExpressionFactory, buildInvocationBuilder());
        ProvidesInjectionNodeBuilderFactory providesInjectionNodeBuilderFactory = new ProvidesInjectionNodeBuilderFactory(providesVariableBuilderFactory, buildInjectionPointFactory(), buildAnalyser());

        ProviderVariableBuilderFactory providerVariableBuilderFactory = new ProviderVariableBuilderFactory(injectionExpressionBuilder, typedExpressionFactory);
        ProviderInjectionNodeBuilderFactory providerInjectionNodeBuilderFactory = new ProviderInjectionNodeBuilderFactory(buildAnalyser(), providerVariableBuilderFactory);

        BindProcessor bindProcessor = new BindProcessor(variableASTImplementationFactory);
        BindProviderProcessor bindProviderProcessor = new BindProviderProcessor(providerInjectionNodeBuilderFactory);
        BindingConfigurationFactory bindingConfigurationFactory = new BindingConfigurationFactory();
        ProvidesProcessor providesProcessor = new ProvidesProcessor(providesInjectionNodeBuilderFactory, new QualifierPredicate(astClassFactory), new ScopesPredicate(astClassFactory), astClassFactory, buildGeneratedProviderInjectionNodeBuilder());

        ScopeReferenceInjectionFactory scopeInjectionFactory = new ScopeReferenceInjectionFactory(typedExpressionFactory, codeModel, buildAnalyser());

        CustomScopeAspectFactoryFactory scopeAspectFactoryFactory = new CustomScopeAspectFactoryFactory(codeModel, buildProviderGenerator(), typedExpressionFactory, namer);

        DefineScopeProcessor defineScopeProcessor = new DefineScopeProcessor(astClassFactory, scopeInjectionFactory, scopeAspectFactoryFactory);

        InstallProcessor installProcessor = new InstallProcessor(moduleRepository);

        return new ModuleProcessor(bindProcessor, bindProviderProcessor,  bindingConfigurationFactory, providesProcessor, astClassFactory, defineScopeProcessor, installProcessor, moduleRepository, buildInjectionNodeRepositoryProvider());
    }

    public FactoryGenerator buildFactoryGenerator() {

        return new FactoryGenerator(codeModel,
                buildInjectionGenerator(),
                new AnalysisContextFactory(buildAnalysisRepository()),
                buildInjectionNodeRepositoryProvider(),
                moduleRepository,
                new InjectionNodeImplFactory(buildInjectionPointFactory(), new VariableFactoryBuilderFactory2(typedExpressionFactory, codeModel, buildAnalyser()), new QualifierPredicate(astClassFactory)),
                new MirroredMethodGeneratorFactory(namer, codeModel),
                generationUtil, namer);
    }

    public FactoriesGenerator buildFactoriesGenerator() {
        return new FactoriesGenerator(codeModel, generationUtil, namer);
    }

    public VirtualProxyGenerator buildVirtualProxyGenerator(){
        return new VirtualProxyGenerator(codeModel, namer, astClassFactory, generationUtil, virtualProxyCache);
    }

    public void registerFactories(Collection<? extends ASTType> factories) {
        //register factory configuration
        InjectionNodeBuilderRepository repository = new InjectionNodeBuilderRepository(astClassFactory);
        for (ASTType factoryType : factories) {
            repository.putType(factoryType,
                    new FactoryNodeBuilder(factoryType, new VariableFactoryBuilderFactory2(typedExpressionFactory, codeModel, buildAnalyser()), buildAnalyser()));
        }

        moduleRepository.addModuleRepository(repository);
    }

    public ModuleRepository getModuleRepository() {
        return moduleRepository;
    }

    private static class InjectionBuilderContextFactoryImpl implements InjectionBuilderContextFactory {
        @Override
        public InjectionBuilderContext buildContext(JBlock block, JDefinedClass definedClass, JExpression scopeVar, Map<InjectionNode, TypedExpression> expressionMap) {
            return new InjectionBuilderContext(block, definedClass, scopeVar, expressionMap);
        }
    }

    private final class ConcreteASTFactory implements ASTFactory {

        private ElementConverterFactory elementConverterFactory;
        private Provider<ASTElementFactory> astElementFactoryProvider;

        public void setElementConverterFactory(ElementConverterFactory elementConverterFactory){
            this.elementConverterFactory = elementConverterFactory;
        }

        public void setAstElementFactoryProvider(Provider<ASTElementFactory> astElementFactoryProvider) {
            this.astElementFactoryProvider = astElementFactoryProvider;
        }

        @Override
        public ASTElementAnnotation buildASTElementAnnotation(AnnotationMirror annotationMirror, ASTType type) {
            return new ASTElementAnnotation(annotationMirror, type, elementConverterFactory);
        }

        @Override
        public LazyClassParameterBuilder builderParameterBuilder(ParameterizedType genericType) {
            return new LazyClassParameterBuilder(genericType, astClassFactory);
        }

        @Override
        public LazyElementParameterBuilder buildParameterBuilder(DeclaredType declaredType) {
            ASTTypeBuilderVisitor astTypeBuilderVisitor = new ASTTypeBuilderVisitor(astElementFactoryProvider);
            return new LazyElementParameterBuilder(declaredType, astTypeBuilderVisitor);
        }

        @Override
        public ASTGenericTypeWrapper buildGenericTypeWrapper(ASTType astType, LazyTypeParameterBuilder lazyTypeParameterBuilder) {
            return new ASTGenericTypeWrapper(astType, lazyTypeParameterBuilder);
        }
    }

    private final class ASTElementFactoryProvider implements Provider<ASTElementFactory>{

        private ASTFactory astFactory;
        private ASTTypeBuilderVisitor astTypeBuilderVisitor;
        private ASTElementConverterFactory astElementConverterFactory;
        private ASTElementFactory astElementFactory = null;

        @Override
        public synchronized ASTElementFactory get() {
            if(astElementFactory == null){
                astElementFactory = new ASTElementFactory(elements, astFactory, astTypeBuilderVisitor, astElementConverterFactory);
            }
            return astElementFactory;
        }

        public void setAstFactory(ASTFactory astFactory) {
            this.astFactory = astFactory;
        }

        public void setAstTypeBuilderVisitor(ASTTypeBuilderVisitor astTypeBuilderVisitor) {
            this.astTypeBuilderVisitor = astTypeBuilderVisitor;
        }

        public void setAstElementConverterFactory(ASTElementConverterFactory astElementConverterFactory) {
            this.astElementConverterFactory = astElementConverterFactory;
        }
    }

    private final class ConcreteVariableExpressionBuilderFactory implements VariableExpressionBuilderFactory {

        @Override
        public CachedExpressionDecorator buildCachedExpressionDecorator(VariableExpressionBuilder decorator) {
            return new CachedExpressionDecorator(decorator);
        }

        @Override
        public ScopedExpressionDecorator buildScopedExpressionDecorator(VariableExpressionBuilder decorator) {
            return new ScopedExpressionDecorator(decorator);
        }

        @Override
        public VariableBuilderExpressionDecorator buildVariableBuilderExpressionDecorator() {
            return new VariableBuilderExpressionDecorator();
        }

        @Override
        public VirtualProxyExpressionDecorator buildVirtualProxyExpressionDecorator(VariableExpressionBuilder decorator) {

            ProxyVariableBuilder proxyVariableBuilder = new ProxyVariableBuilder(namer);
            VirtualProxyGenerator virtualProxyGenerator = new VirtualProxyGenerator(codeModel, namer, astClassFactory, generationUtil, virtualProxyCache);

            return new VirtualProxyExpressionDecorator(decorator, proxyVariableBuilder, virtualProxyGenerator, typedExpressionFactory);
        }
    }

    private final class ModuleRepositoryImpl implements  ModuleRepository{

        private final Set<ASTType> installedComponents = new HashSet<ASTType>();
        private final InjectionNodeBuilderRepository moduleInjectionNodeBuilderRepository = new InjectionNodeBuilderRepository(astClassFactory);

        public InjectionNodeBuilderRepository buildModuleConfiguration() {
            InjectionNodeBuilderRepository repository = new InjectionNodeBuilderRepository(astClassFactory);
            repository.addRepository(moduleInjectionNodeBuilderRepository);
            return repository;
        }

        @Override
        public Collection<ASTType> getInstalledAnnotatedWith(Class<? extends Annotation> annotation) {
            ImmutableSet.Builder<ASTType> installedBuilder = ImmutableSet.builder();

            for (ASTType installedComponent : installedComponents) {
                if(installedComponent.isAnnotated(annotation)){
                    installedBuilder.add(installedComponent);
                }
            }

            return installedBuilder.build();
        }

        @Override
        public void addInstalledComponents(ASTType[] astType) {
            installedComponents.addAll(Arrays.asList(astType));
        }

        @Override
        public void addModuleRepository(InjectionNodeBuilderRepository repository) {
            this.moduleInjectionNodeBuilderRepository.addRepository(repository);
        }
    }
}
