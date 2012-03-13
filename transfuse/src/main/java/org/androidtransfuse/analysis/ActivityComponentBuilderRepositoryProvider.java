package org.androidtransfuse.analysis;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.ListActivity;
import android.preference.PreferenceActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import com.sun.codemodel.JExpr;
import org.androidtransfuse.analysis.adapter.ASTClassFactory;
import org.androidtransfuse.analysis.adapter.ASTMethod;
import org.androidtransfuse.gen.componentBuilder.ComponentBuilderFactory;
import org.androidtransfuse.gen.componentBuilder.MethodCallbackGenerator;
import org.androidtransfuse.util.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author John Ericksen
 */
public class ActivityComponentBuilderRepositoryProvider implements Provider<ActivityComponentBuilderRepository> {

    private ComponentBuilderFactory componentBuilderFactory;
    private ASTClassFactory astClassFactory;
    private Logger log;

    @Inject
    public ActivityComponentBuilderRepositoryProvider(ASTClassFactory astClassFactory, ComponentBuilderFactory componentBuilderFactory, Logger log) {
        this.astClassFactory = astClassFactory;
        this.componentBuilderFactory = componentBuilderFactory;
        this.log = log;
    }

    @Override
    public ActivityComponentBuilderRepository get() {

        Map<String, Set<MethodCallbackGenerator>> methodCallbackGenerators = new HashMap<String, Set<MethodCallbackGenerator>>();

        Set<MethodCallbackGenerator> activityMethodGenerators = buildActivityMethodCallbackGenerators();
        methodCallbackGenerators.put(Activity.class.getName(), activityMethodGenerators);
        methodCallbackGenerators.put(ListActivity.class.getName(), buildListActivityMethodCallbackGenerators(activityMethodGenerators));
        methodCallbackGenerators.put(PreferenceActivity.class.getName(), activityMethodGenerators);
        methodCallbackGenerators.put(ActivityGroup.class.getName(), activityMethodGenerators);

        return new ActivityComponentBuilderRepository(componentBuilderFactory, methodCallbackGenerators, log, astClassFactory);
    }

    private Set<MethodCallbackGenerator> buildListActivityMethodCallbackGenerators(Set<MethodCallbackGenerator> activityMethodGenerators) {
        Set<MethodCallbackGenerator> listActivityCallbackGenerators = new HashSet<MethodCallbackGenerator>();
        listActivityCallbackGenerators.addAll(activityMethodGenerators);

        try {
            //onListItemClick(android.widget.ListView l, android.view.View v, int position, long id)
            ASTMethod onListItemClickMethod = astClassFactory.buildASTClassMethod(ListActivity.class.getDeclaredMethod("onListItemClick", ListView.class, View.class, Integer.TYPE, Long.TYPE));
            listActivityCallbackGenerators.add(
                    componentBuilderFactory.buildMethodCallbackGenerator("onListItemClick",
                            componentBuilderFactory.buildSimpleMethodGenerator(onListItemClickMethod, false)));
        } catch (NoSuchMethodException e) {
            throw new TransfuseAnalysisException("NoSucMethodException while trying to build event method", e);
        }

        return listActivityCallbackGenerators;
    }

    private Set<MethodCallbackGenerator> buildActivityMethodCallbackGenerators() {
        try {
            Set<MethodCallbackGenerator> activityCallbackGenerators = new HashSet<MethodCallbackGenerator>();
            // onDestroy
            activityCallbackGenerators.add(buildEventMethod("onDestroy"));
            // onPause
            activityCallbackGenerators.add(buildEventMethod("onPause"));
            // onRestart
            activityCallbackGenerators.add(buildEventMethod("onRestart"));
            // onResume
            activityCallbackGenerators.add(buildEventMethod("onResume"));
            // onStart
            activityCallbackGenerators.add(buildEventMethod("onStart"));
            // onStop
            activityCallbackGenerators.add(buildEventMethod("onStop"));
            //ontouch method

            ASTMethod onTouchMethod = astClassFactory.buildASTClassMethod(android.app.Activity.class.getDeclaredMethod("onTouchEvent", MotionEvent.class));
            activityCallbackGenerators.add(
                    componentBuilderFactory.buildMethodCallbackGenerator("onTouch",
                            componentBuilderFactory.buildReturningMethodGenerator(onTouchMethod, false, JExpr.TRUE)));

            return activityCallbackGenerators;
        } catch (NoSuchMethodException e) {
            throw new TransfuseAnalysisException("NoSucMethodException while trying to build event method", e);
        }
    }


    private MethodCallbackGenerator buildEventMethod(String name) throws NoSuchMethodException {
        return buildEventMethod(name, name);
    }

    private MethodCallbackGenerator buildEventMethod(String eventName, String methodName) throws NoSuchMethodException {

        ASTMethod method = astClassFactory.buildASTClassMethod(android.app.Activity.class.getDeclaredMethod(methodName));

        return componentBuilderFactory.buildMethodCallbackGenerator(eventName,
                componentBuilderFactory.buildSimpleMethodGenerator(method, true));
    }
}