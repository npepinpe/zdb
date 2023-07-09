/*
 * Copyright © 2021 Christopher Kujawa (zelldon91@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zell.zdb.db.readonly.transaction;

import static org.rocksdb.Status.Code.Aborted;
import static org.rocksdb.Status.Code.Busy;
import static org.rocksdb.Status.Code.Expired;
import static org.rocksdb.Status.Code.IOError;
import static org.rocksdb.Status.Code.MergeInProgress;
import static org.rocksdb.Status.Code.Ok;
import static org.rocksdb.Status.Code.TimedOut;
import static org.rocksdb.Status.Code.TryAgain;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.RocksObject;
import org.rocksdb.Status;
import org.rocksdb.Status.Code;
import org.rocksdb.Transaction;

public final class RocksDbInternal {

  static Field nativeHandle;
  static Method getWithHandle;
  static Method seekMethod;

  static {
    RocksDB.loadLibrary();

    try {
      resolveInternalMethods();
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static void resolveInternalMethods() throws NoSuchFieldException, NoSuchMethodException {
    nativeHandles();
    getWithHandle();
    seekWithHandle();
  }

  private static void nativeHandles() throws NoSuchFieldException {
    nativeHandle = RocksObject.class.getDeclaredField("nativeHandle_");
    nativeHandle.setAccessible(true);
  }

  private static void getWithHandle() throws NoSuchMethodException {
    getWithHandle =
        RocksDB.class.getDeclaredMethod(
            "get", Long.TYPE, Long.TYPE, byte[].class, Integer.TYPE, Integer.TYPE);
    getWithHandle.setAccessible(true);
  }

  private static void seekWithHandle() throws NoSuchMethodException {
    seekMethod =
        RocksIterator.class.getDeclaredMethod("seek0", long.class, byte[].class, int.class);
    seekMethod.setAccessible(true);
  }

  public static void seek(
      final RocksIterator iterator,
      final long nativeHandle,
      final byte[] target,
      final int targetLength) {
    try {
      seekMethod.invoke(iterator, nativeHandle, target, targetLength);
    } catch (final IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Unexpected error occurred trying to seek with RocksIterator", e);
    }
  }
}
