package org.androidrobotics.analysis.adapter;

import java.util.List;

/**
 * Abstract Syntax Tree Method node
 *
 * @author John Ericksen
 */
public interface ASTMethod extends ASTBase {

    /**
     * Supplies all parameters of this method
     *
     * @return method parameters
     */
    List<ASTParameter> getParameters();

    /**
     * Supplies the return type of this method
     *
     * @return return type
     */
    ASTType getReturnType();

    ASTAccessModifier getAccessModifier();
}
