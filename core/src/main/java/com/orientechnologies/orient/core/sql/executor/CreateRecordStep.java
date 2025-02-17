package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.Optional;

/** @author Luigi Dell'Aquila (l.dellaquila-(at)-orientdb.com) */
public class CreateRecordStep extends AbstractExecutionStep {

  private int total = 0;
  private Optional<String> cl;

  public CreateRecordStep(
      OCommandContext ctx, int total, boolean profilingEnabled, Optional<String> cl) {
    super(ctx, profilingEnabled);
    this.total = total;
    this.cl = cl;
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws OTimeoutException {
    getPrev().ifPresent(x -> x.start(ctx).close(ctx));
    return OExecutionStream.produce(this::produce).limit(total);
  }

  private OResult produce(OCommandContext ctx) {
    if (cl.isPresent()) {
      return new OUpdatableResult((ODocument) ctx.getDatabase().newInstance(cl.get()));
    }
    return new OUpdatableResult((ODocument) ctx.getDatabase().newInstance());
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ CREATE EMPTY RECORDS");
    if (profilingEnabled) {
      result.append(" (" + getCostFormatted() + ")");
    }
    result.append("\n");
    result.append(spaces);
    if (total == 1) {
      result.append("  1 record");
    } else {
      result.append("  " + total + " record");
    }
    return result.toString();
  }
}
