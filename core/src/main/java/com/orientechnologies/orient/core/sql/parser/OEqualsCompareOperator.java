/* Generated By:JJTree: Do not edit this line. OEqualsCompareOperator.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.sql.executor.metadata.OIndexFinder.Operation;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorEquals;
import java.util.Map;

public class OEqualsCompareOperator extends SimpleNode implements OBinaryCompareOperator {
  protected boolean doubleEquals = false;

  public OEqualsCompareOperator(int id) {
    super(id);
  }

  public OEqualsCompareOperator(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean execute(Object iLeft, Object iRight) {
    return OQueryOperatorEquals.equals(iLeft, iRight);
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  public Operation getOperation() {
    return Operation.Eq;
  }

  @Override
  public String toString() {
    return doubleEquals ? "==" : "=";
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(toString());
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append(toString());
  }

  @Override
  public OEqualsCompareOperator copy() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && obj.getClass().equals(this.getClass())
        && ((OEqualsCompareOperator) obj).doubleEquals == doubleEquals;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public boolean isInclude() {
    return false;
  }

  @Override
  public boolean isLess() {
    return false;
  }

  @Override
  public boolean isGreater() {
    return false;
  }
}
/* JavaCC - OriginalChecksum=bd2ec5d13a1d171779c2bdbc9d3a56bc (do not edit this line) */
