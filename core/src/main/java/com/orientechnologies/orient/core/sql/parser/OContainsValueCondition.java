/* Generated By:JJTree: Do not edit this line. OContainsValueCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.executor.OIndexSearchInfo;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.metadata.OIndexCandidate;
import com.orientechnologies.orient.core.sql.executor.metadata.OIndexFinder;
import com.orientechnologies.orient.core.sql.executor.metadata.OPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OContainsValueCondition extends OBooleanExpression {
  protected OExpression left;
  protected OContainsValueOperator operator;
  protected OOrBlock condition;
  protected OExpression expression;

  public OContainsValueCondition(int id) {
    super(id);
  }

  public OContainsValueCondition(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(OIdentifiable currentRecord, OCommandContext ctx) {
    Object leftValue = left.execute(currentRecord, ctx);
    if (leftValue instanceof Map) {
      Map map = (Map) leftValue;
      if (condition != null) {
        for (Object o : map.values()) {
          if (condition.evaluate(o, ctx)) {
            return true;
          }
        }
        return false;
      } else {
        Object rightValue = expression.execute(currentRecord, ctx);
        return map.values().contains(rightValue); // TODO type conversions...?
      }
    }
    return false;
  }

  @Override
  public boolean evaluate(OResult currentRecord, OCommandContext ctx) {
    if (left.isFunctionAny()) {
      return evaluateAny(currentRecord, ctx);
    }

    if (left.isFunctionAll()) {
      return evaluateAllFunction(currentRecord, ctx);
    }

    Object leftValue = left.execute(currentRecord, ctx);
    if (leftValue instanceof Map) {
      Map map = (Map) leftValue;
      if (condition != null) {
        for (Object o : map.values()) {
          if (condition.evaluate(o, ctx)) {
            return true;
          }
        }
        return false;
      } else {
        Object rightValue = expression.execute(currentRecord, ctx);
        return map.values().contains(rightValue); // TODO type conversions...?
      }
    }
    return false;
  }

  private boolean evaluateAllFunction(OResult currentRecord, OCommandContext ctx) {
    for (String propertyName : currentRecord.getPropertyNames()) {
      Object leftValue = currentRecord.getProperty(propertyName);
      if (leftValue instanceof Map) {
        Map map = (Map) leftValue;
        if (condition != null) {
          boolean found = false;
          for (Object o : map.values()) {
            if (condition.evaluate(o, ctx)) {
              found = true;
              break;
            }
          }
          if (!found) {
            return false;
          }
        } else {
          Object rightValue = expression.execute(currentRecord, ctx);
          if (!map.values().contains(rightValue)) {
            return false;
          }
        }
      } else {
        return false;
      }
    }
    return true;
  }

  private boolean evaluateAny(OResult currentRecord, OCommandContext ctx) {
    for (String propertyName : currentRecord.getPropertyNames()) {
      Object leftValue = currentRecord.getProperty(propertyName);
      if (leftValue instanceof Map) {
        Map map = (Map) leftValue;
        if (condition != null) {
          for (Object o : map.values()) {
            if (condition.evaluate(o, ctx)) {
              return true;
            }
          }
        } else {
          Object rightValue = expression.execute(currentRecord, ctx);
          if (map.values().contains(rightValue)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {

    left.toString(params, builder);
    builder.append(" CONTAINSVALUE ");
    if (condition != null) {
      builder.append("(");
      condition.toString(params, builder);
      builder.append(")");
    } else {
      expression.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    left.toGenericStatement(builder);
    builder.append(" CONTAINSVALUE ");
    if (condition != null) {
      builder.append("(");
      condition.toGenericStatement(builder);
      builder.append(")");
    } else {
      expression.toGenericStatement(builder);
    }
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    if (condition == null) {
      return 0;
    }
    return condition.getNumberOfExternalCalculations();
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    if (condition == null) {
      return Collections.EMPTY_LIST;
    }
    return condition.getExternalCalculationConditions();
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    if (left != null && left.needsAliases(aliases)) {
      return true;
    }
    if (condition != null && condition.needsAliases(aliases)) {
      return true;
    }
    if (expression != null && expression.needsAliases(aliases)) {
      return true;
    }

    return false;
  }

  @Override
  public OContainsValueCondition copy() {
    OContainsValueCondition result = new OContainsValueCondition(-1);
    result.left = left.copy();
    result.operator = operator;
    result.condition = condition == null ? null : condition.copy();
    result.expression = expression == null ? null : expression.copy();
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    left.extractSubQueries(collector);
    if (condition != null) {
      condition.extractSubQueries(collector);
    }
    if (expression != null) {
      expression.extractSubQueries(collector);
    }
  }

  @Override
  public boolean refersToParent() {
    if (left != null && left.refersToParent()) {
      return true;
    }
    if (condition != null && condition.refersToParent()) {
      return true;
    }
    if (expression != null && condition.refersToParent()) {
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OContainsValueCondition that = (OContainsValueCondition) o;

    if (left != null ? !left.equals(that.left) : that.left != null) return false;
    if (operator != null ? !operator.equals(that.operator) : that.operator != null) return false;
    if (condition != null ? !condition.equals(that.condition) : that.condition != null)
      return false;
    if (expression != null ? !expression.equals(that.expression) : that.expression != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (operator != null ? operator.hashCode() : 0);
    result = 31 * result + (condition != null ? condition.hashCode() : 0);
    result = 31 * result + (expression != null ? expression.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> leftX = left == null ? null : left.getMatchPatternInvolvedAliases();
    List<String> expressionX =
        expression == null ? null : expression.getMatchPatternInvolvedAliases();
    List<String> conditionX = condition == null ? null : condition.getMatchPatternInvolvedAliases();

    List<String> result = new ArrayList<String>();
    if (leftX != null) {
      result.addAll(leftX);
    }
    if (expressionX != null) {
      result.addAll(expressionX);
    }
    if (conditionX != null) {
      result.addAll(conditionX);
    }

    return result.size() == 0 ? null : result;
  }

  @Override
  public boolean isCacheable() {
    if (left != null && !left.isCacheable()) {
      return false;
    }
    if (condition != null && !condition.isCacheable()) {
      return false;
    }
    if (expression != null && !expression.isCacheable()) {
      return false;
    }

    return true;
  }

  public OExpression getExpression() {
    return expression;
  }

  public OExpression getLeft() {
    return left;
  }

  public OContainsValueOperator getOperator() {
    return operator;
  }

  public Optional<OIndexCandidate> findIndex(OIndexFinder info, OCommandContext ctx) {
    Optional<OPath> path = left.getPath();
    if (path.isPresent()) {
      if (expression != null && expression.isEarlyCalculated(ctx)) {
        Object value = expression.execute((OResult) null, ctx);
        return info.findByValueIndex(path.get(), value, ctx);
      }
    }

    return Optional.empty();
  }

  public boolean isIndexAware(OIndexSearchInfo info, OCommandContext ctx) {
    if (left.isBaseIdentifier()) {
      if (info.getField().equals(left.getDefaultAlias().getStringValue())) {
        if (expression != null
            && expression.isEarlyCalculated(info.getCtx())
            && info.isMap()
            && info.isIndexByValue()) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public OExpression resolveKeyFrom(OBinaryCondition additional) {
    return getExpression();
  }

  @Override
  public OExpression resolveKeyTo(OBinaryCondition additional) {
    return getExpression();
  }

  @Override
  public boolean isKeyFromIncluded(OBinaryCondition additional) {
    OBinaryCompareOperator operator = getOperator();
    if (operator.isGreater()) {
      return operator.isInclude();
    } else {
      if (additional != null && additional.getOperator() != null) {
        return additional.getOperator().isGreaterInclude();
      } else {
        return true;
      }
    }
  }

  @Override
  public boolean isKeyToIncluded(OBinaryCondition additional) {
    OBinaryCompareOperator operator = getOperator();
    if (operator.isLess()) {
      return operator.isInclude();
    } else {
      if (additional != null && additional.getOperator() != null) {
        return additional.getOperator().isLessInclude();
      } else {
        return true;
      }
    }
  }
}
/* JavaCC - OriginalChecksum=6fda752f10c8d8731f43efa706e39459 (do not edit this line) */
