package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OView;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.parser.OUpdateItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by luigidellaquila on 09/08/16.
 */
public class UpdateSetStep extends AbstractExecutionStep {
  private final List<OUpdateItem> items;

  public UpdateSetStep(List<OUpdateItem> updateItems, OCommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.items = updateItems;
  }

  @Override
  public OResultSet syncPull(OCommandContext ctx, int nRecords) throws OTimeoutException {
    OResultSet upstream = getPrev().get().syncPull(ctx, nRecords);
    return new OResultSet() {
      @Override
      public boolean hasNext() {
        return upstream.hasNext();
      }

      @Override
      public OResult next() {
        OResult result = upstream.next();
        if (result instanceof OResultInternal) {
          for (OUpdateItem item : items) {
            OClass type = result.getElement().flatMap(x -> x.getSchemaType()).orElse(null);
            if (type == null) {
              Object clazz = result.getProperty("@view");
              if (clazz instanceof String) {
                type = ctx.getDatabase().getMetadata().getSchema().getView((String) clazz);
              }
            }
            if (type instanceof OView) {
              if (!((OView) type).isUpdatable()) {
                throw new OCommandExecutionException("View not updatable: " + type.getName());
              }
              String originField = ((OView) type).getOriginRidField();
              if (originField == null) {
                throw new OCommandExecutionException("No origin field set for view " + type.getName());
              }
              OIdentifiable elem = result.getProperty(originField);
              if (elem instanceof ORID) {
                elem = elem.getRecord();
              }
              OUpdatableResult linkedResult = new OUpdatableResult((OElement) elem);
              item.applyUpdate(linkedResult, ctx);
            }

            item.applyUpdate((OResultInternal) result, ctx);
          }
        }
        return result;
      }

      @Override
      public void close() {
        upstream.close();
      }

      @Override
      public Optional<OExecutionPlan> getExecutionPlan() {
        return null;
      }

      @Override
      public Map<String, Long> getQueryStats() {
        return null;
      }
    };
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ UPDATE SET");
    for (int i = 0; i < items.size(); i++) {
      OUpdateItem item = items.get(i);
      if (i < items.size()) {
        result.append("\n");
      }
      result.append(spaces);
      result.append("  ");
      result.append(item.toString());
    }
    return result.toString();
  }
}
