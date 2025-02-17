/*
 *
 *  *  Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *  * For more information: http://orientdb.com
 *
 */

package com.orientechnologies.orient.core.storage.ridbag.sbtree;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperationsManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Persistent Set<OIdentifiable> implementation that uses the SBTree to handle entries in persistent
 * way.
 *
 * @author Artem Orobets (enisher-at-gmail.com)
 */
public class OIndexRIDContainer implements Set<OIdentifiable> {
  public static final String INDEX_FILE_EXTENSION = ".irs";

  private final long fileId;
  private Set<OIdentifiable> underlying;
  private boolean isEmbedded;
  private int topThreshold =
      OGlobalConfiguration.INDEX_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.getValueAsInteger();
  private final int bottomThreshold =
      OGlobalConfiguration.INDEX_SBTREEBONSAI_TO_EMBEDDED_THRESHOLD.getValueAsInteger();
  private final boolean durableNonTxMode;

  /** Should be called inside of lock to ensure uniqueness of entity on disk !!! */
  public OIndexRIDContainer(
      String name,
      boolean durableNonTxMode,
      AtomicLong bonsayFileId,
      final OAbstractPaginatedStorage storage) {
    long gotFileId = bonsayFileId.get();
    if (gotFileId == 0) {
      gotFileId = resolveFileIdByName(name + INDEX_FILE_EXTENSION, storage);
      bonsayFileId.set(gotFileId);
    }
    this.fileId = gotFileId;

    underlying = new HashSet<>();
    isEmbedded = true;
    this.durableNonTxMode = durableNonTxMode;
  }

  public OIndexRIDContainer(long fileId, Set<OIdentifiable> underlying, boolean durableNonTxMode) {
    this.fileId = fileId;
    this.underlying = underlying;
    isEmbedded = !(underlying instanceof OIndexRIDContainerSBTree);
    this.durableNonTxMode = durableNonTxMode;
  }

  public void setTopThreshold(int topThreshold) {
    this.topThreshold = topThreshold;
  }

  private static long resolveFileIdByName(
      String fileName, final OAbstractPaginatedStorage storage) {

    final OAtomicOperationsManager atomicOperationsManager = storage.getAtomicOperationsManager();
    final OAtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
    Objects.requireNonNull(atomicOperation);

    return atomicOperationsManager.calculateInsideComponentOperation(
        atomicOperation,
        fileName,
        (operation) -> {
          final long fileId;
          if (atomicOperation.isFileExists(fileName)) {
            fileId = atomicOperation.loadFile(fileName);
          } else {
            fileId = atomicOperation.addFile(fileName);
          }
          return fileId;
        });
  }

  public long getFileId() {
    return fileId;
  }

  @Override
  public int size() {
    return underlying.size();
  }

  @Override
  public boolean isEmpty() {
    return underlying.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return underlying.contains(o);
  }

  @Override
  public Iterator<OIdentifiable> iterator() {
    return underlying.iterator();
  }

  @Override
  public Object[] toArray() {
    return underlying.toArray();
  }

  @SuppressWarnings("SuspiciousToArrayCall")
  @Override
  public <T> T[] toArray(T[] a) {
    return underlying.toArray(a);
  }

  @Override
  public boolean add(OIdentifiable oIdentifiable) {
    final boolean res = underlying.add(oIdentifiable);
    checkTopThreshold();
    return res;
  }

  @Override
  public boolean remove(Object o) {
    final boolean res = underlying.remove(o);
    checkBottomThreshold();
    return res;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return underlying.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends OIdentifiable> c) {
    final boolean res = underlying.addAll(c);
    checkTopThreshold();
    return res;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return underlying.retainAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    final boolean res = underlying.removeAll(c);
    checkBottomThreshold();
    return res;
  }

  @Override
  public void clear() {
    if (isEmbedded) {
      underlying.clear();
    } else {
      final OIndexRIDContainerSBTree tree = (OIndexRIDContainerSBTree) underlying;
      tree.delete();
      underlying = new HashSet<>();
      isEmbedded = true;
    }
  }

  public boolean isEmbedded() {
    return isEmbedded;
  }

  public boolean isDurableNonTxMode() {
    return durableNonTxMode;
  }

  public Set<OIdentifiable> getUnderlying() {
    return underlying;
  }

  private void checkTopThreshold() {
    if (isEmbedded && topThreshold < underlying.size()) {
      convertToSbTree();
    }
  }

  private void checkBottomThreshold() {
    if (!isEmbedded && bottomThreshold > underlying.size()) {
      convertToEmbedded();
    }
  }

  private void convertToEmbedded() {
    final OIndexRIDContainerSBTree tree = (OIndexRIDContainerSBTree) underlying;

    final Set<OIdentifiable> set = new HashSet<>(tree);

    tree.delete();
    underlying = set;
    isEmbedded = true;
  }

  private void convertToSbTree() {
    final ODatabaseDocumentInternal db = ODatabaseRecordThreadLocal.instance().get();
    final OIndexRIDContainerSBTree tree =
        new OIndexRIDContainerSBTree(fileId, (OAbstractPaginatedStorage) db.getStorage());

    tree.addAll(underlying);

    underlying = tree;
    isEmbedded = false;
  }
}
