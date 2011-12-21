package org.androidrobotics.gen;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import org.androidrobotics.analysis.AnalysisContext;
import org.androidrobotics.analysis.Analyzer;
import org.androidrobotics.analysis.adapter.ASTClassFactory;
import org.androidrobotics.analysis.adapter.ASTType;
import org.androidrobotics.analysis.astAnalyzer.ASTInjectionAspect;
import org.androidrobotics.analysis.astAnalyzer.ProxyAspect;
import org.androidrobotics.config.RoboticsGenerationGuiceModule;
import org.androidrobotics.gen.target.*;
import org.androidrobotics.gen.variableBuilder.ProviderAspect;
import org.androidrobotics.gen.variableBuilder.ProviderVariableBuilderFactory;
import org.androidrobotics.gen.variableBuilder.VariableBuilder;
import org.androidrobotics.model.*;
import org.androidrobotics.util.JavaUtilLogger;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Provider;

import static junit.framework.Assert.*;

/**
 * @author John Ericksen
 */
public class InjectionFragmentGeneratorTest {

    @Inject
    private InjectionFragmentGeneratorHarness fragmentGeneratorHarness;
    @Inject
    private CodeGenerationUtil codeGenerationUtil;
    @Inject
    private JCodeModel codeModel;
    private VariableBuilderRepository variableBuilderRepository;
    @Inject
    private ProviderVariableBuilderFactory providerVariableBuilderFactory;
    @Inject
    private ASTClassFactory astClassFactory;
    @Inject
    private Analyzer analyzer;

    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new RoboticsGenerationGuiceModule(new JavaUtilLogger(this)));
        injector.injectMembers(this);

        VariableBuilderRepositoryFactory variableBuilderRepositoryFactory = injector.getInstance(VariableBuilderRepositoryFactory.class);

        variableBuilderRepository = variableBuilderRepositoryFactory.buildRepository();
    }

    @Test
    public void testConstrictorInjection() throws Exception {
        InjectionNode injectionNode = new InjectionNode(astClassFactory.buildASTClassType(ConstructorInjectable.class));
        injectionNode.addAspect(VariableBuilder.class, variableBuilderRepository.get(ConstructorInjectable.class.getName()));
        //setup constructor injection
        ConstructorInjectionPoint constructorInjectionPoint = new ConstructorInjectionPoint();
        constructorInjectionPoint.addInjectionNode(buildInjectionNode(InjectionTarget.class));
        getInjectionAspect(injectionNode).add(constructorInjectionPoint);

        ConstructorInjectable constructorInjectable = buildInstance(ConstructorInjectable.class, injectionNode);

        assertNotNull(constructorInjectable.getInjectionTarget());
    }

    @Test
    public void testMethodInjection() throws Exception {
        InjectionNode injectionNode = buildInjectionNode(MethodInjectable.class);

        MethodInjectionPoint methodInjectionPoint = new MethodInjectionPoint("setInjectionTarget");
        methodInjectionPoint.addInjectionNode(buildInjectionNode(InjectionTarget.class));
        getInjectionAspect(injectionNode).add(methodInjectionPoint);

        MethodInjectable methodInjectable = buildInstance(MethodInjectable.class, injectionNode);

        assertNotNull(methodInjectable.getInjectionTarget());
    }

    @Test
    public void testFieldInjection() throws Exception {
        InjectionNode injectionNode = buildInjectionNode(FieldInjectable.class);

        FieldInjectionPoint fieldInjectionPoint = new FieldInjectionPoint("injectionTarget", buildInjectionNode(InjectionTarget.class));
        getInjectionAspect(injectionNode).add(fieldInjectionPoint);

        FieldInjectable fieldInjectable = buildInstance(FieldInjectable.class, injectionNode);

        assertNotNull(fieldInjectable.getInjectionTarget());
    }

    @Test
    public void testDelayedProxyInjection() throws Exception {
        InjectionNode injectionNode = new InjectionNode(astClassFactory.buildASTClassType(DelayedProxyTarget.class));
        injectionNode.addAspect(VariableBuilder.class, variableBuilderRepository.get(DelayedProxyTarget.class.getName()));

        ProxyAspect proxyAspect = new ProxyAspect();
        proxyAspect.setProxyRequired(true);
        proxyAspect.getProxyInterfaces().add(astClassFactory.buildASTClassType(DelayedProxy.class));

        injectionNode.addAspect(proxyAspect);

        //setup constructor injection
        ConstructorInjectionPoint constructorInjectionPoint = new ConstructorInjectionPoint();
        InjectionNode dependencyInjectionNode = new InjectionNode(astClassFactory.buildASTClassType(DelayedProxyDependency.class));
        dependencyInjectionNode.addAspect(VariableBuilder.class, variableBuilderRepository.get(DelayedProxyTarget.class.getName()));
        constructorInjectionPoint.addInjectionNode(dependencyInjectionNode);
        getInjectionAspect(injectionNode).add(constructorInjectionPoint);

        //reference circle
        ConstructorInjectionPoint dependencyConstructorInjectionPoint = new ConstructorInjectionPoint();
        dependencyConstructorInjectionPoint.addInjectionNode(injectionNode);
        getInjectionAspect(dependencyInjectionNode).add(dependencyConstructorInjectionPoint);

        DelayedProxyTarget proxyTarget = buildInstance(DelayedProxyTarget.class, injectionNode);

        assertNotNull(proxyTarget.getDelayedProxyDependency());
        assertNotNull(proxyTarget.getDelayedProxyDependency().getDelayedProxy());
        //assertEquals(proxyTarget, proxyTarget.getDelayedProxyDependency().getDelayedProxy());
        assertTrue(proxyTarget.getDelayedProxyDependency().getDelayedProxy() instanceof DelayedProxy);
    }

    @Test
    public void testVariableBuilder() throws Exception {
        InjectionNode injectionNode = buildInjectionNode(VariableBuilderInjectable.class);

        FieldInjectionPoint fieldInjectionPoint = new FieldInjectionPoint("target", buildInjectionNode(VariableTarget.class));
        getInjectionAspect(injectionNode).add(fieldInjectionPoint);

        variableBuilderRepository.put(VariableTarget.class.getName(), new VariableBuilder() {
            @Override
            public JExpression buildVariable(InjectionBuilderContext injectionBuilderContext, InjectionNode injectionNode) {
                return JExpr._new(codeModel.ref(VariableTarget.class));
            }

            @Override
            public InjectionNode buildInjectionNode(ASTType astType, AnalysisContext context) {
                return analyzer.analyze(astType, astType, context);
            }
        });

        VariableBuilderInjectable vbInjectable = buildInstance(VariableBuilderInjectable.class, injectionNode);

        assertNotNull(vbInjectable.getTarget());
    }

    @Test
    public void testProviderBuilder() throws Exception {

        variableBuilderRepository.put(VariableTarget.class.getName(), providerVariableBuilderFactory.buildProviderVariableBuilder(VariableTargetProvider.class));

        InjectionNode injectionNode = buildInjectionNode(VariableTarget.class);
        injectionNode.addAspect(new ProviderAspect(buildInjectionNode(VariableTargetProvider.class)));
        injectionNode.addAspect(VariableBuilder.class, providerVariableBuilderFactory.buildProviderVariableBuilder(VariableTargetProvider.class));

        VariableTarget target = buildInstance(VariableTarget.class, injectionNode);

        assertNotNull(target);
    }

    private InjectionNode buildInjectionNode(Class<?> instanceClass) {
        InjectionNode injectionNode = new InjectionNode(astClassFactory.buildASTClassType(instanceClass));
        injectionNode.addAspect(VariableBuilder.class, variableBuilderRepository.get(instanceClass.getName()));

        ConstructorInjectionPoint noArgConstructorInjectionPoint = new ConstructorInjectionPoint();
        getInjectionAspect(injectionNode).add(noArgConstructorInjectionPoint);

        return injectionNode;
    }

    private <T> T buildInstance(Class<T> instanceClass, InjectionNode injectionNode) throws Exception {
        PackageClass providerPackageClass = new PackageClass(instanceClass).add("Provider");

        fragmentGeneratorHarness.buildProvider(injectionNode, providerPackageClass);

        ClassLoader classLoader = codeGenerationUtil.build(false);
        Class<Provider> generatedFactoryClass = (Class<Provider>) classLoader.loadClass(providerPackageClass.getFullyQualifiedName());

        assertNotNull(generatedFactoryClass);
        Provider provider = generatedFactoryClass.newInstance();
        Object result = provider.get();
        assertEquals(instanceClass, result.getClass());

        return (T) result;
    }

    private ASTInjectionAspect getInjectionAspect(InjectionNode injectionNode) {
        if (!injectionNode.containsAspect(ASTInjectionAspect.class)) {
            injectionNode.addAspect(new ASTInjectionAspect());
        }
        return injectionNode.getAspect(ASTInjectionAspect.class);
    }
}
