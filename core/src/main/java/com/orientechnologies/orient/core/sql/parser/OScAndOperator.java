/* Generated By:JJTree: Do not edit this line. OScAndOperator.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.operator.OQueryOperator;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorFactory;
import java.util.Iterator;
import java.util.Map;

public class OScAndOperator extends SimpleNode implements OBinaryCompareOperator {

  protected OQueryOperator lowLevelOperator = null;

  public OScAndOperator(int id) {
    super(id);
    initOperator();
  }

  public OScAndOperator(OrientSql p, int id) {
    super(p, id);
    initOperator();
  }

  protected void initOperator() {
    Iterator<OQueryOperatorFactory> factories = OSQLEngine.getOperatorFactories();
    while (factories.hasNext()) {
      OQueryOperatorFactory factory = factories.next();
      for (OQueryOperator op : factory.getOperators()) {
        if ("&&".equals(op.getKeyword())) {
          lowLevelOperator = op;
        }
      }
    }
  }

  @Override
  public boolean execute(Object iLeft, Object iRight) {
    if (lowLevelOperator == null) {
      // TODO implement this!
    }
    if (lowLevelOperator == null) {
      throw new UnsupportedOperationException();
    }
    return lowLevelOperator.evaluate(iLeft, iRight);
  }

  @Override
  public String toString() {
    return "&&";
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("&&");
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("&&");
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  public OScAndOperator copy() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && obj.getClass().equals(this.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
/* JavaCC - OriginalChecksum=12592a24f576571470ce760aff503b30 (do not edit this line) */
