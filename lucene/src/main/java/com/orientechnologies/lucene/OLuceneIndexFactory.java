/*
 * Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orientechnologies.lucene;

import static com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE.FULLTEXT;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.log.OLogger;
import com.orientechnologies.lucene.engine.OLuceneFullTextIndexEngine;
import com.orientechnologies.lucene.index.OLuceneFullTextIndex;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.IndexEngineData;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseInternal;
import com.orientechnologies.orient.core.db.ODatabaseLifecycleListener;
import com.orientechnologies.orient.core.exception.OConfigurationException;
import com.orientechnologies.orient.core.index.OIndexFactory;
import com.orientechnologies.orient.core.index.OIndexInternal;
import com.orientechnologies.orient.core.index.OIndexMetadata;
import com.orientechnologies.orient.core.index.engine.OBaseIndexEngine;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.OStorage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class OLuceneIndexFactory implements OIndexFactory, ODatabaseLifecycleListener {
  private static final OLogger logger = OLogManager.instance().logger(OLuceneIndexFactory.class);
  public static final String LUCENE_ALGORITHM = "LUCENE";

  private static final Set<String> TYPES;
  private static final Set<String> ALGORITHMS;

  static {
    final Set<String> types = new HashSet<String>();
    types.add(FULLTEXT.toString());
    TYPES = Collections.unmodifiableSet(types);
  }

  static {
    final Set<String> algorithms = new HashSet<String>();
    algorithms.add(LUCENE_ALGORITHM);
    ALGORITHMS = Collections.unmodifiableSet(algorithms);
  }

  public OLuceneIndexFactory() {
    this(false);
  }

  public OLuceneIndexFactory(boolean manual) {
    if (!manual) Orient.instance().addDbLifecycleListener(this);
  }

  @Override
  public int getLastVersion(final String algorithm) {
    return 0;
  }

  @Override
  public Set<String> getTypes() {
    return TYPES;
  }

  @Override
  public Set<String> getAlgorithms() {
    return ALGORITHMS;
  }

  @Override
  public OIndexInternal createIndex(OStorage storage, OIndexMetadata im)
      throws OConfigurationException {
    ODocument metadata = im.getMetadata();
    final String indexType = im.getType();
    final String algorithm = im.getAlgorithm();

    if (metadata == null) {
      metadata = new ODocument().field("analyzer", StandardAnalyzer.class.getName());
      im.setMetadata(metadata);
    }

    if (FULLTEXT.toString().equalsIgnoreCase(indexType)) {
      return new OLuceneFullTextIndex(im, storage);
    }
    throw new OConfigurationException("Unsupported type : " + algorithm);
  }

  @Override
  public OBaseIndexEngine createIndexEngine(OStorage storage, IndexEngineData data) {
    return new OLuceneFullTextIndexEngine(storage, data.getName(), data.getIndexId());
  }

  @Override
  public PRIORITY getPriority() {
    return PRIORITY.REGULAR;
  }

  @Override
  public void onCreate(ODatabaseInternal db) {
    logger.debug("onCreate");
  }

  @Override
  public void onOpen(ODatabaseInternal db) {
    logger.debug("onOpen");
  }

  @Override
  public void onClose(ODatabaseInternal db) {
    logger.debug("onClose");
  }

  @Override
  public void onDrop(final ODatabaseInternal db) {
    try {
      if (db.isClosed()) return;

      logger.debug("Dropping Lucene indexes...");

      final ODatabaseDocumentInternal internal = (ODatabaseDocumentInternal) db;
      internal.getMetadata().getIndexManagerInternal().getIndexes(internal).stream()
          .filter(idx -> idx.getInternal() instanceof OLuceneFullTextIndex)
          .peek(idx -> logger.debug("deleting index %s", idx.getName()))
          .forEach(idx -> idx.delete());

    } catch (Exception e) {
      logger.warn("Error on dropping Lucene indexes", e);
    }
  }

  @Override
  public void onLocalNodeConfigurationRequest(ODocument iConfiguration) {}
}
