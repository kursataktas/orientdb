/* Generated By:JJTree: Do not edit this line. OBaseIdentifier.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.collate.OCollate;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.executor.AggregationContext;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class OBaseIdentifier extends SimpleNode {

  protected OLevelZeroIdentifier levelZero;

  protected OSuffixIdentifier suffix;

  public OBaseIdentifier(int id) {
    super(id);
  }

  public OBaseIdentifier(OrientSql p, int id) {
    super(p, id);
  }

  public OBaseIdentifier(OIdentifier identifier) {
    this.suffix = new OSuffixIdentifier(identifier);
  }

  public OBaseIdentifier(ORecordAttribute attr) {
    this.suffix = new OSuffixIdentifier(attr);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (levelZero != null) {
      levelZero.toString(params, builder);
    } else if (suffix != null) {
      suffix.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    if (levelZero != null) {
      levelZero.toGenericStatement(builder);
    } else if (suffix != null) {
      suffix.toGenericStatement(builder);
    }
  }

  public Object execute(OIdentifiable iCurrentRecord, OCommandContext ctx) {
    if (levelZero != null) {
      return levelZero.execute(iCurrentRecord, ctx);
    }
    if (suffix != null) {
      return suffix.execute(iCurrentRecord, ctx);
    }
    return null;
  }

  public Object execute(OResult iCurrentRecord, OCommandContext ctx) {
    if (levelZero != null) {
      return levelZero.execute(iCurrentRecord, ctx);
    }
    if (suffix != null) {
      return suffix.execute(iCurrentRecord, ctx);
    }
    return null;
  }

  public boolean isFunctionAny() {
    if (levelZero != null) {
      return levelZero.isFunctionAny();
    }
    return false;
  }

  public boolean isFunctionAll() {
    if (levelZero != null) {
      return levelZero.isFunctionAll();
    }
    return false;
  }

  public boolean isIndexedFunctionCall() {
    if (levelZero != null) {
      return levelZero.isIndexedFunctionCall();
    }
    return false;
  }

  public long estimateIndexedFunction(
      OFromClause target, OCommandContext context, OBinaryCompareOperator operator, Object right) {
    if (levelZero != null) {
      return levelZero.estimateIndexedFunction(target, context, operator, right);
    }

    return -1;
  }

  public Iterable<OIdentifiable> executeIndexedFunction(
      OFromClause target, OCommandContext context, OBinaryCompareOperator operator, Object right) {
    if (levelZero != null) {
      return levelZero.executeIndexedFunction(target, context, operator, right);
    }

    return null;
  }

  /**
   * tests if current expression is an indexed function AND that function can also be executed
   * without using the index
   *
   * @param target the query target
   * @param context the execution context
   * @param operator
   * @param right
   * @return true if current expression is an indexed function AND that function can also be
   *     executed without using the index, false otherwise
   */
  public boolean canExecuteIndexedFunctionWithoutIndex(
      OFromClause target, OCommandContext context, OBinaryCompareOperator operator, Object right) {
    if (this.levelZero == null) {
      return false;
    }
    return levelZero.canExecuteIndexedFunctionWithoutIndex(target, context, operator, right);
  }

  /**
   * tests if current expression is an indexed function AND that function can be used on this target
   *
   * @param target the query target
   * @param context the execution context
   * @param operator
   * @param right
   * @return true if current expression involves an indexed function AND that function can be used
   *     on this target, false otherwise
   */
  public boolean allowsIndexedFunctionExecutionOnTarget(
      OFromClause target, OCommandContext context, OBinaryCompareOperator operator, Object right) {
    if (this.levelZero == null) {
      return false;
    }
    return levelZero.allowsIndexedFunctionExecutionOnTarget(target, context, operator, right);
  }

  /**
   * tests if current expression is an indexed function AND the function has also to be executed
   * after the index search. In some cases, the index search is accurate, so this condition can be
   * excluded from further evaluation. In other cases the result from the index is a superset of the
   * expected result, so the function has to be executed anyway for further filtering
   *
   * @param target the query target
   * @param context the execution context
   * @return true if current expression is an indexed function AND the function has also to be
   *     executed after the index search.
   */
  public boolean executeIndexedFunctionAfterIndexSearch(
      OFromClause target, OCommandContext context, OBinaryCompareOperator operator, Object right) {
    if (this.levelZero == null) {
      return false;
    }
    return levelZero.executeIndexedFunctionAfterIndexSearch(target, context, operator, right);
  }

  public boolean isBaseIdentifier() {
    return suffix != null && suffix.isBaseIdentifier();
  }

  public boolean isExpand() {
    if (levelZero != null) {
      return levelZero.isExpand();
    }
    return false;
  }

  public OExpression getExpandContent() {
    return levelZero.getExpandContent();
  }

  public boolean needsAliases(Set<String> aliases) {
    if (levelZero != null && levelZero.needsAliases(aliases)) {
      return true;
    }
    if (suffix != null && suffix.needsAliases(aliases)) {
      return true;
    }
    return false;
  }

  public boolean isAggregate() {
    if (levelZero != null && levelZero.isAggregate()) {
      return true;
    }
    if (suffix != null && suffix.isAggregate()) {
      return true;
    }
    return false;
  }

  public boolean isCount() {
    if (levelZero != null && levelZero.isCount()) {
      return true;
    }
    if (suffix != null && suffix.isCount()) {
      return true;
    }
    return false;
  }

  public boolean isEarlyCalculated(OCommandContext ctx) {
    if (levelZero != null && levelZero.isEarlyCalculated(ctx)) {
      return true;
    }
    if (suffix != null && suffix.isEarlyCalculated(ctx)) {
      return true;
    }
    return false;
  }

  public SimpleNode splitForAggregation(
      AggregateProjectionSplit aggregateProj, OCommandContext ctx) {
    if (isAggregate()) {
      OBaseIdentifier result = new OBaseIdentifier(-1);
      if (levelZero != null) {
        SimpleNode splitResult = levelZero.splitForAggregation(aggregateProj, ctx);
        if (splitResult instanceof OLevelZeroIdentifier) {
          result.levelZero = (OLevelZeroIdentifier) splitResult;
        } else {
          return splitResult;
        }
      } else if (suffix != null) {
        result.suffix = suffix.splitForAggregation(aggregateProj);
      } else {
        throw new IllegalStateException();
      }
      return result;
    } else {
      return this;
    }
  }

  public AggregationContext getAggregationContext(OCommandContext ctx) {
    if (isAggregate()) {

      if (levelZero != null) {
        return levelZero.getAggregationContext(ctx);
      } else if (suffix != null) {
        return suffix.getAggregationContext(ctx);
      } else {
        throw new OCommandExecutionException("cannot aggregate on " + toString());
      }
    } else {
      throw new OCommandExecutionException("cannot aggregate on " + toString());
    }
  }

  public void setLevelZero(OLevelZeroIdentifier levelZero) {
    this.levelZero = levelZero;
  }

  public OBaseIdentifier copy() {
    OBaseIdentifier result = new OBaseIdentifier(-1);
    result.levelZero = levelZero == null ? null : levelZero.copy();
    result.suffix = suffix == null ? null : suffix.copy();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OBaseIdentifier that = (OBaseIdentifier) o;

    if (levelZero != null ? !levelZero.equals(that.levelZero) : that.levelZero != null)
      return false;
    if (suffix != null ? !suffix.equals(that.suffix) : that.suffix != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = levelZero != null ? levelZero.hashCode() : 0;
    result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
    return result;
  }

  public boolean refersToParent() {
    if (levelZero != null && levelZero.refersToParent()) {
      return true;
    }
    if (suffix != null && suffix.refersToParent()) {
      return true;
    }
    return false;
  }

  public OSuffixIdentifier getSuffix() {
    return suffix;
  }

  public OLevelZeroIdentifier getLevelZero() {

    return levelZero;
  }

  public void applyRemove(OResultInternal result, OCommandContext ctx) {
    if (suffix != null) {
      suffix.applyRemove(result, ctx);
    } else {
      throw new OCommandExecutionException("cannot apply REMOVE " + toString());
    }
  }

  public OResult serialize() {
    OResultInternal result = new OResultInternal();
    if (levelZero != null) {
      result.setProperty("levelZero", levelZero.serialize());
    }
    if (suffix != null) {
      result.setProperty("suffix", suffix.serialize());
    }
    return result;
  }

  public void deserialize(OResult fromResult) {
    if (fromResult.getProperty("levelZero") != null) {
      levelZero = new OLevelZeroIdentifier(-1);
      levelZero.deserialize(fromResult.getProperty("levelZero"));
    }
    if (fromResult.getProperty("suffix") != null) {
      suffix = new OSuffixIdentifier(-1);
      suffix.deserialize(fromResult.getProperty("suffix"));
    }
  }

  public boolean isDefinedFor(OResult currentRecord) {
    if (suffix != null) {
      return suffix.isDefinedFor(currentRecord);
    }
    return true;
  }

  public boolean isDefinedFor(OElement currentRecord) {
    if (suffix != null) {
      return suffix.isDefinedFor(currentRecord);
    }
    return true;
  }

  public void extractSubQueries(OIdentifier letAlias, SubQueryCollector collector) {
    if (this.levelZero != null) {
      this.levelZero.extractSubQueries(letAlias, collector);
    }
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (this.levelZero != null) {
      this.levelZero.extractSubQueries(collector);
    }
  }

  public OCollate getCollate(OResult currentRecord, OCommandContext ctx) {
    return suffix == null ? null : suffix.getCollate(currentRecord, ctx);
  }

  public boolean isCacheable() {
    if (levelZero != null) {
      return levelZero.isCacheable();
    }

    if (suffix != null) {
      return suffix.isCacheable();
    }

    return true;
  }

  public boolean isIndexChain(OCommandContext ctx, OClass clazz) {
    if (suffix != null && suffix.isBaseIdentifier()) {
      final String fieldName = suffix.getIdentifier().getStringValue();
      OProperty prop = clazz.getProperty(fieldName);
      if (prop == null) {
        return false;
      }
      Collection<OIndex> allIndexes = prop.getAllIndexes();

      return allIndexes != null
          && allIndexes.stream()
              .anyMatch(
                  idx -> {
                    if (idx.supportsOrderedIterations()) {
                      return idx.getDefinition().getFields().get(0).equals(fieldName);
                    } else {
                      return idx.getDefinition().getFields().size() == 1;
                    }
                  });
    }
    return false;
  }
}
/* JavaCC - OriginalChecksum=ed89af10d8be41a83428c5608a4834f6 (do not edit this line) */
