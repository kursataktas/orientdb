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
package com.orientechnologies.orient.core.storage.index.engine;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.common.serialization.types.OBinarySerializer;
import com.orientechnologies.common.types.OModifiableBoolean;
import com.orientechnologies.common.util.OCommonConst;
import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.orient.core.config.IndexEngineData;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.encryption.OEncryption;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.index.OIndexKeyUpdater;
import com.orientechnologies.orient.core.index.OIndexMetadata;
import com.orientechnologies.orient.core.index.OIndexUpdateAction;
import com.orientechnologies.orient.core.index.engine.IndexEngineValidator;
import com.orientechnologies.orient.core.index.engine.IndexEngineValuesTransformer;
import com.orientechnologies.orient.core.index.engine.OIndexEngine;
import com.orientechnologies.orient.core.index.multivalue.OMultivalueEntityRemover;
import com.orientechnologies.orient.core.index.multivalue.OMultivalueIndexKeyUpdaterImpl;
import com.orientechnologies.orient.core.iterator.OEmptyIterator;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import com.orientechnologies.orient.core.storage.index.hashindex.local.OHashFunction;
import com.orientechnologies.orient.core.storage.index.hashindex.local.OHashTable;
import com.orientechnologies.orient.core.storage.index.hashindex.local.OMurmurHash3HashFunction;
import com.orientechnologies.orient.core.storage.index.hashindex.local.OSHA256HashFunction;
import com.orientechnologies.orient.core.storage.index.hashindex.local.v2.LocalHashTableV2;
import com.orientechnologies.orient.core.storage.index.hashindex.local.v3.OLocalHashTableV3;
import com.orientechnologies.orient.core.storage.index.versionmap.OVersionPositionMap;
import com.orientechnologies.orient.core.storage.index.versionmap.OVersionPositionMapV0;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Andrey Lomakin (a.lomakin-at-orientdb.com)
 * @since 15.07.13
 */
public final class OHashTableIndexEngine implements OIndexEngine {
  public static final int VERSION = 3;

  public static final String METADATA_FILE_EXTENSION = ".him";
  public static final String TREE_FILE_EXTENSION = ".hit";
  public static final String BUCKET_FILE_EXTENSION = ".hib";
  public static final String NULL_BUCKET_FILE_EXTENSION = ".hnb";

  private final OHashTable<Object, Object> hashTable;
  private final AtomicLong bonsayFileId = new AtomicLong(0);

  private final String name;

  private final int id;

  private final OVersionPositionMap versionPositionMap;

  private OAbstractPaginatedStorage storage;

  private String valueContainerAlgorithm;

  public OHashTableIndexEngine(
      String name, int id, OAbstractPaginatedStorage storage, int version) {
    this.storage = storage;
    this.id = id;
    if (version < 2) {
      throw new IllegalStateException("Unsupported version of hash index");
    } else if (version == 2) {
      hashTable =
          new LocalHashTableV2<>(
              name,
              METADATA_FILE_EXTENSION,
              TREE_FILE_EXTENSION,
              BUCKET_FILE_EXTENSION,
              NULL_BUCKET_FILE_EXTENSION,
              storage);
    } else if (version == 3) {
      hashTable =
          new OLocalHashTableV3<>(
              name,
              METADATA_FILE_EXTENSION,
              TREE_FILE_EXTENSION,
              BUCKET_FILE_EXTENSION,
              NULL_BUCKET_FILE_EXTENSION,
              storage);
    } else {
      throw new IllegalStateException("Invalid value of the index version , version = " + version);
    }
    versionPositionMap =
        new OVersionPositionMapV0(
            storage, name, name + TREE_FILE_EXTENSION, OVersionPositionMap.DEF_EXTENSION);
    this.name = name;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void init(OIndexMetadata metadata) {
    if (metadata.isMultivalue()) {
      this.valueContainerAlgorithm = metadata.getValueContainerAlgorithm();
    }
  }

  @Override
  public String getName() {
    return name;
  }

  public void create(OAtomicOperation atomicOperation, IndexEngineData data) throws IOException {

    OBinarySerializer valueSerializer =
        storage.resolveObjectSerializer(data.getValueSerializerId());
    OBinarySerializer keySerializer = storage.resolveObjectSerializer(data.getKeySerializedId());

    final OEncryption encryption =
        OAbstractPaginatedStorage.loadEncryption(data.getEncryption(), data.getEncryptionOptions());
    final OHashFunction<Object> hashFunction;

    if (encryption != null) {
      hashFunction = new OSHA256HashFunction<>(keySerializer);
    } else {
      hashFunction = new OMurmurHash3HashFunction<>(keySerializer);
    }

    hashTable.create(
        atomicOperation,
        keySerializer,
        valueSerializer,
        data.getKeyTypes(),
        encryption,
        hashFunction,
        data.isNullValuesSupport());
    versionPositionMap.create(atomicOperation);
  }

  @Override
  public void flush() {}

  @Override
  public String getIndexNameByKey(final Object key) {
    return name;
  }

  @Override
  public void updateUniqueIndexVersion(final Object key) {
    final int keyHash = versionPositionMap.getKeyHash(key);
    versionPositionMap.updateVersion(keyHash);
  }

  @Override
  public int getUniqueIndexVersion(final Object key) {
    final int keyHash = versionPositionMap.getKeyHash(key);
    return versionPositionMap.getVersion(keyHash);
  }

  @Override
  public void delete(OAtomicOperation atomicOperation) throws IOException {
    doClearTable(atomicOperation);

    hashTable.delete(atomicOperation);
    versionPositionMap.delete(atomicOperation);
  }

  private void doClearTable(OAtomicOperation atomicOperation) throws IOException {
    final OHashTable.Entry<Object, Object> firstEntry = hashTable.firstEntry();

    if (firstEntry != null) {
      OHashTable.Entry<Object, Object>[] entries = hashTable.ceilingEntries(firstEntry.key);
      while (entries.length > 0) {
        for (final OHashTable.Entry<Object, Object> entry : entries) {
          hashTable.remove(atomicOperation, entry.key);
        }

        entries = hashTable.higherEntries(entries[entries.length - 1].key);
      }
    }

    if (hashTable.isNullKeyIsSupported()) {
      hashTable.remove(atomicOperation, null);
    }
  }

  @Override
  public void load(IndexEngineData data) {
    OBinarySerializer keySerializer = storage.resolveObjectSerializer(data.getKeySerializedId());
    OBinarySerializer valueSerializer =
        storage.resolveObjectSerializer(data.getValueSerializerId());

    final OEncryption encryption =
        OAbstractPaginatedStorage.loadEncryption(data.getEncryption(), data.getEncryptionOptions());

    final OHashFunction<Object> hashFunction;

    if (encryption != null) {
      //noinspection unchecked
      hashFunction = new OSHA256HashFunction<>(keySerializer);
    } else {
      //noinspection unchecked
      hashFunction = new OMurmurHash3HashFunction<>(keySerializer);
    }
    //noinspection unchecked
    hashTable.load(
        data.getName(),
        data.getKeyTypes(),
        data.isNullValuesSupport(),
        encryption,
        hashFunction,
        keySerializer,
        valueSerializer);

    try {
      versionPositionMap.open();
    } catch (final IOException e) {
      throw OException.wrapException(
          new OIndexException("Error during VPM load of index " + data.getName()), e);
    }
  }

  @Override
  public boolean remove(OAtomicOperation atomicOperation, Object key) {
    try {
      return hashTable.remove(atomicOperation, key) != null;
    } catch (final IOException e) {
      throw OException.wrapException(
          new OIndexException("Error during key remove on index " + getName()), e);
    }
  }

  @Override
  public void clear(OAtomicOperation atomicOperation) throws IOException {
    doClearTable(atomicOperation);
  }

  @Override
  public void close() {
    hashTable.close();
  }

  @Override
  public Object get(Object key) {
    return hashTable.get(key);
  }

  @Override
  public void put(OAtomicOperation atomicOperation, Object key, ORID value) {
    if (valueContainerAlgorithm != null) {
      int binaryFormatVersion = storage.getConfiguration().getBinaryFormatVersion();
      final OIndexKeyUpdater<Object> creator =
          new OMultivalueIndexKeyUpdaterImpl(
              value, valueContainerAlgorithm, binaryFormatVersion, name, storage);

      update(atomicOperation, key, creator);
    } else {
      try {
        hashTable.put(atomicOperation, key, value);
      } catch (IOException e) {
        throw OException.wrapException(
            new OIndexException("Error during updating of key " + key + " in index " + name), e);
      }
    }
  }

  @Override
  public void put(OAtomicOperation atomicOperation, Object key, Object value) throws IOException {
    hashTable.put(atomicOperation, key, value);
  }

  @Override
  public boolean remove(OAtomicOperation atomicOperation, Object key, ORID value) {
    if (valueContainerAlgorithm != null) {

      Set<OIdentifiable> values = (Set<OIdentifiable>) get(key);

      if (values == null) {
        return false;
      }

      final OModifiableBoolean removed = new OModifiableBoolean(false);

      final OIndexKeyUpdater<Object> creator = new OMultivalueEntityRemover(value, removed);

      update(atomicOperation, key, creator);

      return removed.getValue();
    } else {
      return remove(atomicOperation, key);
    }
  }

  @Override
  public void update(
      OAtomicOperation atomicOperation, Object key, OIndexKeyUpdater<Object> updater) {
    try {
      Object value = get(key);
      OIndexUpdateAction<Object> updated = updater.update(value, bonsayFileId);
      if (updated.isChange()) {
        put(atomicOperation, key, updated.getValue());
      } else if (updated.isRemove()) {
        remove(atomicOperation, key);
      } else //noinspection StatementWithEmptyBody
      if (updated.isNothing()) {
        // Do nothing
      }
    } catch (IOException e) {
      throw OException.wrapException(
          new OIndexException("Error during updating of key " + key + " in index " + name), e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean validatedPut(
      OAtomicOperation atomicOperation,
      Object key,
      ORID value,
      IndexEngineValidator<Object, ORID> validator) {
    return hashTable.validatedPut(atomicOperation, key, value, (IndexEngineValidator) validator);
  }

  @Override
  public long size(IndexEngineValuesTransformer transformer) {
    if (transformer == null) {
      return hashTable.size();
    } else {
      long counter = 0;

      if (hashTable.isNullKeyIsSupported()) {
        final Object nullValue = hashTable.get(null);
        if (nullValue != null) {
          counter += transformer.transformFromValue(nullValue).size();
        }
      }

      OHashTable.Entry<Object, Object> firstEntry = hashTable.firstEntry();
      if (firstEntry == null) {
        return counter;
      }

      OHashTable.Entry<Object, Object>[] entries = hashTable.ceilingEntries(firstEntry.key);

      while (entries.length > 0) {
        for (OHashTable.Entry<Object, Object> entry : entries) {
          counter += transformer.transformFromValue(entry.value).size();
        }

        entries = hashTable.higherEntries(entries[entries.length - 1].key);
      }

      return counter;
    }
  }

  @Override
  public boolean hasRangeQuerySupport() {
    return false;
  }

  @Override
  public Stream<ORawPair<Object, ORID>> iterateEntriesBetween(
      Object rangeFrom,
      boolean fromInclusive,
      Object rangeTo,
      boolean toInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    throw new UnsupportedOperationException("iterateEntriesBetween");
  }

  @Override
  public Stream<ORawPair<Object, ORID>> iterateEntriesMajor(
      Object fromKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    throw new UnsupportedOperationException("iterateEntriesMajor");
  }

  @Override
  public Stream<ORawPair<Object, ORID>> iterateEntriesMinor(
      Object toKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    throw new UnsupportedOperationException("iterateEntriesMinor");
  }

  @Override
  public Stream<ORawPair<Object, ORID>> stream(
      final IndexEngineValuesTransformer valuesTransformer) {
    return StreamSupport.stream(
        new Spliterator<ORawPair<Object, ORID>>() {
          private int nextEntriesIndex;
          private OHashTable.Entry<Object, Object>[] entries;

          private Iterator<ORID> currentIterator = new OEmptyIterator<>();
          private Object currentKey;

          {
            OHashTable.Entry<Object, Object> firstEntry = hashTable.firstEntry();
            if (firstEntry == null) {
              //noinspection unchecked
              entries = OCommonConst.EMPTY_BUCKET_ENTRY_ARRAY;
            } else {
              entries = hashTable.ceilingEntries(firstEntry.key);
            }

            if (entries.length == 0) {
              currentIterator = null;
            }
          }

          @Override
          public boolean tryAdvance(Consumer<? super ORawPair<Object, ORID>> action) {
            if (currentIterator == null) {
              return false;
            }

            if (currentIterator.hasNext()) {
              final OIdentifiable identifiable = currentIterator.next();
              action.accept(new ORawPair<>(currentKey, identifiable.getIdentity()));
              return true;
            }

            while (currentIterator != null && !currentIterator.hasNext()) {
              if (entries.length == 0) {
                currentIterator = null;
                return false;
              }

              final OHashTable.Entry<Object, Object> bucketEntry = entries[nextEntriesIndex];

              currentKey = bucketEntry.key;

              Object value = bucketEntry.value;
              if (valuesTransformer != null) {
                currentIterator = valuesTransformer.transformFromValue(value).iterator();
              } else {
                currentIterator = Collections.singletonList((ORID) value).iterator();
              }

              nextEntriesIndex++;

              if (nextEntriesIndex >= entries.length) {
                entries = hashTable.higherEntries(entries[entries.length - 1].key);

                nextEntriesIndex = 0;
              }
            }

            if (currentIterator != null) {
              final OIdentifiable identifiable = currentIterator.next();
              action.accept(new ORawPair<>(currentKey, identifiable.getIdentity()));
              return true;
            }

            return false;
          }

          @Override
          public Spliterator<ORawPair<Object, ORID>> trySplit() {
            return null;
          }

          @Override
          public long estimateSize() {
            return Long.MAX_VALUE;
          }

          @Override
          public int characteristics() {
            return NONNULL;
          }
        },
        false);
  }

  @Override
  public Stream<ORawPair<Object, ORID>> descStream(
      final IndexEngineValuesTransformer valuesTransformer) {
    return StreamSupport.stream(
        new Spliterator<ORawPair<Object, ORID>>() {
          private int nextEntriesIndex;
          private OHashTable.Entry<Object, Object>[] entries;

          private Iterator<ORID> currentIterator = new OEmptyIterator<>();
          private Object currentKey;

          {
            OHashTable.Entry<Object, Object> lastEntry = hashTable.lastEntry();
            if (lastEntry == null) {
              //noinspection unchecked
              entries = OCommonConst.EMPTY_BUCKET_ENTRY_ARRAY;
            } else {
              entries = hashTable.floorEntries(lastEntry.key);
            }

            if (entries.length == 0) {
              currentIterator = null;
            }
          }

          @Override
          public boolean tryAdvance(Consumer<? super ORawPair<Object, ORID>> action) {
            if (currentIterator == null) {
              return false;
            }

            if (currentIterator.hasNext()) {
              final OIdentifiable identifiable = currentIterator.next();
              action.accept(new ORawPair<>(currentKey, identifiable.getIdentity()));
              return true;
            }

            while (currentIterator != null && !currentIterator.hasNext()) {
              if (entries.length == 0) {
                currentIterator = null;
                return false;
              }

              final OHashTable.Entry<Object, Object> bucketEntry = entries[nextEntriesIndex];

              currentKey = bucketEntry.key;

              Object value = bucketEntry.value;
              if (valuesTransformer != null) {
                currentIterator = valuesTransformer.transformFromValue(value).iterator();
              } else {
                currentIterator = Collections.singletonList((ORID) value).iterator();
              }

              nextEntriesIndex--;

              if (nextEntriesIndex < 0) {
                entries = hashTable.lowerEntries(entries[0].key);

                nextEntriesIndex = entries.length - 1;
              }
            }

            if (currentIterator != null) {
              final OIdentifiable identifiable = currentIterator.next();
              action.accept(new ORawPair<>(currentKey, identifiable.getIdentity()));
              return true;
            }

            return false;
          }

          @Override
          public Spliterator<ORawPair<Object, ORID>> trySplit() {
            return null;
          }

          @Override
          public long estimateSize() {
            return Long.MAX_VALUE;
          }

          @Override
          public int characteristics() {
            return NONNULL;
          }
        },
        false);
  }

  @Override
  public Stream<Object> keyStream() {
    return StreamSupport.stream(
        new Spliterator<Object>() {
          private int nextEntriesIndex;
          private OHashTable.Entry<Object, Object>[] entries;

          {
            OHashTable.Entry<Object, Object> firstEntry = hashTable.firstEntry();
            if (firstEntry == null) {
              //noinspection unchecked
              entries = OCommonConst.EMPTY_BUCKET_ENTRY_ARRAY;
            } else {
              entries = hashTable.ceilingEntries(firstEntry.key);
            }
          }

          @Override
          public boolean tryAdvance(Consumer<? super Object> action) {
            if (entries.length == 0) {
              return false;
            }

            final OHashTable.Entry<Object, Object> bucketEntry = entries[nextEntriesIndex];
            nextEntriesIndex++;
            if (nextEntriesIndex >= entries.length) {
              entries = hashTable.higherEntries(entries[entries.length - 1].key);

              nextEntriesIndex = 0;
            }

            action.accept(bucketEntry.key);
            return true;
          }

          @Override
          public Spliterator<Object> trySplit() {
            return null;
          }

          @Override
          public long estimateSize() {
            return Long.MAX_VALUE;
          }

          @Override
          public int characteristics() {
            return NONNULL;
          }
        },
        false);
  }

  @Override
  public boolean acquireAtomicExclusiveLock(Object key) {
    hashTable.acquireAtomicExclusiveLock();
    return true;
  }

  public boolean hasRidBagTreesSupport() {
    return true;
  }
}
