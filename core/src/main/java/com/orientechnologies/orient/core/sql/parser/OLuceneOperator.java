/* Generated By:JJTree: Do not edit this line. OLuceneOperator.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.sql.executor.metadata.OIndexFinder.Operation;
import java.util.Map;

public class OLuceneOperator extends SimpleNode implements OBinaryCompareOperator {
  public OLuceneOperator(int id) {
    super(id);
  }

  public OLuceneOperator(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean execute(Object left, Object right) {
    throw new UnsupportedOperationException(
        toString() + " operator cannot be evaluated in this context");
  }

  @Override
  public String toString() {
    return "LUCENE";
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("LUCENE");
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("LUCENE");
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  public Operation getOperation() {
    return Operation.FuzzyEq;
  }

  @Override
  public OLuceneOperator copy() {
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
/* JavaCC - OriginalChecksum=bda1e010e6ba48c815829b22ce458b9d (do not edit this line) */
