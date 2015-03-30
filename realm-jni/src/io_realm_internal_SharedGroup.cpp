/*
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>

#include "util.hpp"

#include <realm/group_shared.hpp>
#include <realm/replication.hpp>
#include <realm/commit_log.hpp>

#include "util.hpp"
#include "io_realm_internal_SharedGroup.h"

using namespace std;
using namespace realm;

namespace {
template<typename T>
class PrimitiveArray {
public:
    PrimitiveArray(JNIEnv* env, jarray array, jsize size)
    : m_env(env)
    , m_array(array)
    , m_size(size)
    , m_ptr(reinterpret_cast<T*>(env->GetPrimitiveArrayCritical(array, nullptr)))
    {
    }

    ~PrimitiveArray()
    {
        if (m_ptr)
            m_env->ReleasePrimitiveArrayCritical(m_array, m_ptr, 0);
    }

    T* begin()
    {
        return m_ptr;
    }

    T* end()
    {
        return m_ptr + m_size;
    }

private:
    JNIEnv* m_env;
    jarray m_array;
    jsize m_size;
    T* m_ptr;
};

class ModifiedRowParser {
    size_t current_table = 0;

public:
    std::vector<std::vector<size_t>> modified;

    bool insert_group_level_table(size_t table_ndx, size_t num_tables, StringData) noexcept
    {
        if (table_ndx < modified.size()) {
            size_t count = modified.size() - table_ndx;
            modified.resize(modified.size() + num_tables);
            std::move(modified.begin() + table_ndx,
                      modified.begin() + table_ndx + count,
                      modified.begin() + table_ndx + num_tables);
            for (size_t i = table_ndx; i < table_ndx + num_tables; ++i)
                modified[i].clear();
        }
        return true;
    }

    bool erase_group_level_table(size_t table_ndx, size_t num_tables) noexcept
    {
        if (table_ndx < modified.size()) {
            std::move(modified.begin() + table_ndx + num_tables,
                      modified.end(),
                      modified.begin() + table_ndx);
            modified.resize(modified.size() - std::min(modified.size() - table_ndx, num_tables));
        }
        return true;
    }

    bool rename_group_level_table(size_t, StringData) noexcept { return true; }

    bool select_table(size_t group_level_ndx, int, const size_t*) noexcept
    {
        current_table = group_level_ndx;
        if (current_table >= modified.size())
            modified.resize(current_table + 1);
        return true;
    }

    bool insert_empty_rows(size_t row_ndx, size_t num_rows, size_t last_row_ndx, bool unordered)
    {
        if (unordered) {
            for (size_t i = 0; i < num_rows; ++i) {
                mark_dirty(row_ndx + i);
                mark_dirty(last_row_ndx - i - 1);
            }
        }
        else {
            auto& rows = modified[current_table];
            for (auto& row : rows) {
                if (row >= row_ndx)
                    row += num_rows;
            }
            rows.reserve(rows.size() + num_rows);
            for (size_t i = 0; i < num_rows; ++i)
                rows.push_back(i + row_ndx);
        }
        return true;
    }

    bool erase_rows(size_t row_ndx, size_t num_rows, size_t last_row_ndx, bool unordered) noexcept
    {
        auto& rows = modified[current_table];
        if (unordered) {
            size_t out = 0;
            for (auto row : rows) {
                if (row > last_row_ndx - num_rows)
                    ++out;
                else
                    rows[out++] = row;
            }
            for (size_t i = 0; i < num_rows; ++i)
                mark_dirty(row_ndx + i);
        }
        else {
            size_t out = 0;
            for (auto row : rows) {
                if (row >= row_ndx && row < row_ndx - num_rows)
                    ;
                else if (row >= row_ndx)
                    rows[out++] = row - num_rows;
                else
                    rows[out++] = row;
            }
            rows.erase(rows.begin() + out, rows.end());
        }
        return true;
    }

    bool clear_table() noexcept
    {
        modified[current_table].clear();
        return true;
    }

    bool add_int_to_column(size_t, int_fast64_t) { return false; }

    // Things that just mark the row as modified
    bool insert_int(size_t, size_t row_ndx, size_t, int_fast64_t) { return mark_dirty(row_ndx); }
    bool insert_bool(size_t, size_t row_ndx, size_t, bool) { return mark_dirty(row_ndx); }
    bool insert_float(size_t, size_t row_ndx, size_t, float) { return mark_dirty(row_ndx); }
    bool insert_double(size_t, size_t row_ndx, size_t, double) { return mark_dirty(row_ndx); }
    bool insert_string(size_t, size_t row_ndx, size_t, StringData) { return mark_dirty(row_ndx); }
    bool insert_binary(size_t, size_t row_ndx, size_t, BinaryData) { return mark_dirty(row_ndx); }
    bool insert_date_time(size_t, size_t row_ndx, size_t, DateTime) { return mark_dirty(row_ndx); }
    bool insert_table(size_t, size_t row_ndx, size_t) { return mark_dirty(row_ndx); }
    bool insert_mixed(size_t, size_t row_ndx, size_t, const Mixed&) { return mark_dirty(row_ndx); }
    bool insert_link(size_t, size_t row_ndx, size_t, size_t) { return mark_dirty(row_ndx); }
    bool insert_link_list(size_t, size_t row_ndx, size_t) { return mark_dirty(row_ndx); }

    bool set_int(size_t, size_t row_ndx, int_fast64_t) { return mark_dirty(row_ndx); }
    bool set_bool(size_t, size_t row_ndx, bool) { return mark_dirty(row_ndx); }
    bool set_float(size_t, size_t row_ndx, float) { return mark_dirty(row_ndx); }
    bool set_double(size_t, size_t row_ndx, double) { return mark_dirty(row_ndx); }
    bool set_string(size_t, size_t row_ndx, StringData) { return mark_dirty(row_ndx); }
    bool set_binary(size_t, size_t row_ndx, BinaryData) { return mark_dirty(row_ndx); }
    bool set_date_time(size_t, size_t row_ndx, DateTime) { return mark_dirty(row_ndx); }
    bool select_link_list(size_t, size_t row_ndx) { return mark_dirty(row_ndx); }
    bool set_table(size_t, size_t row_ndx) { return mark_dirty(row_ndx); }
    bool set_mixed(size_t, size_t row_ndx, const Mixed&) { return mark_dirty(row_ndx); }
    bool set_link(size_t, size_t row_ndx, size_t) { return mark_dirty(row_ndx); }

    // Things we don't need to do anything for
    bool row_insert_complete() { return true; }
    bool optimize_table() { return true; }
    bool select_descriptor(int, const size_t*) { return true; }
    bool insert_column(size_t, DataType, StringData) { return true; }
    bool insert_link_column(size_t, DataType, StringData, size_t, size_t) { return true; }
    bool erase_column(size_t) { return true; }
    bool erase_link_column(size_t, size_t, size_t) { return true; }
    bool rename_column(size_t, StringData) { return true; }
    bool add_search_index(size_t) { return true; }
    bool remove_search_index(size_t) { return true; }
    bool add_primary_key(size_t) { return true; }
    bool remove_primary_key() { return true; }
    bool set_link_type(size_t, LinkType) { return true; }
    bool link_list_set(size_t, size_t) { return true; }
    bool link_list_insert(size_t, size_t) { return true; }
    bool link_list_move(size_t, size_t) { return true; }
    bool link_list_erase(size_t) { return true; }
    bool link_list_clear() { return true; }

private:
    bool mark_dirty(size_t row_ndx) {
        auto& vec = modified[row_ndx];
        if (find(vec.begin(), vec.end(), row_ndx) == vec.end())
            vec.push_back(row_ndx);
        return true;
    }
};

} // anonymous namespace


#define SG(ptr) reinterpret_cast<SharedGroup*>(ptr)

JNIEXPORT jlong JNICALL Java_io_realm_internal_SharedGroup_nativeCreate(
    JNIEnv* env, jobject, jstring jfile_name, jint durability, jboolean no_create, jboolean enable_replication, jbyteArray keyArray)
{
    TR_ENTER()
    StringData file_name;

    SharedGroup* db = 0;
    try {
        JStringAccessor file_name_tmp(env, jfile_name); // throws
        file_name = StringData(file_name_tmp);

        if (enable_replication) {
#ifdef REALM_ENABLE_REPLICATION
            ThrowException(env, UnsupportedOperation,
                           "Replication is not currently supported by the Java language binding.");
//            db = new SharedGroup(SharedGroup::replication_tag(), *file_name_ptr ? file_name_ptr : 0);
#else
            ThrowException(env, UnsupportedOperation,
                           "Replication was disabled in the native library at compile time.");
#endif
        }
        else {
            SharedGroup::DurabilityLevel level;
            if (durability == 0)
                level = SharedGroup::durability_Full;
            else if (durability == 1)
                level = SharedGroup::durability_MemOnly;
            else if (durability == 2)
#ifdef _WIN32
                level = SharedGroup::durability_Full;   // For Windows, use Full instead of Async
#else
                level = SharedGroup::durability_Async;
#endif
            else {
                ThrowException(env, UnsupportedOperation, "Unsupported durability.");
                return 0;
            }

            KeyBuffer key(env, keyArray);
#ifdef REALM_ENABLE_ENCRYPTION
            db = new SharedGroup(file_name, no_create!=0, level, key.data());
#else
            db = new SharedGroup(file_name, no_create!=0, level);
#endif
        }
        return reinterpret_cast<jlong>(db);
    }
    CATCH_FILE(file_name)
    CATCH_STD()
    return 0;
}

JNIEXPORT jlong JNICALL Java_io_realm_internal_SharedGroup_createNativeWithImplicitTransactions
  (JNIEnv* env, jobject, jlong native_replication_ptr, jbyteArray keyArray)
{
    TR_ENTER()
    try {
        KeyBuffer key(env, keyArray);
#ifdef REALM_ENABLE_ENCRYPTION
        SharedGroup* db = new SharedGroup(*reinterpret_cast<realm::Replication*>(native_replication_ptr), SharedGroup::durability_Full, key.data());
#else
        SharedGroup* db = new SharedGroup(*reinterpret_cast<realm::Replication*>(native_replication_ptr));
#endif

        return reinterpret_cast<jlong>(db);
    }
    CATCH_FILE()
    CATCH_STD()
    return 0;
}

JNIEXPORT jlong JNICALL Java_io_realm_internal_SharedGroup_nativeCreateReplication
  (JNIEnv* env, jobject, jstring jfile_name, jbyteArray keyArray)
{
    TR_ENTER()
    StringData file_name;
    try {     
        JStringAccessor file_name_tmp(env, jfile_name); // throws
        file_name = StringData(file_name_tmp);
        KeyBuffer key(env, keyArray);
#ifdef REALM_ENABLE_ENCRYPTION
        Replication* repl = makeWriteLogCollector(file_name, false, key.data()).release();
#else
        Replication* repl = makeWriteLogCollector(file_name).release();
#endif
        return reinterpret_cast<jlong>(repl);
    }
    CATCH_FILE(file_name)
    CATCH_STD()
    return 0;
}

JNIEXPORT jlong JNICALL Java_io_realm_internal_SharedGroup_nativeBeginImplicit
  (JNIEnv* env, jobject, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr)
    try {
        Group& group = const_cast<Group&>(SG(native_ptr)->begin_read());
        return reinterpret_cast<jlong>(&group);
    }
    CATCH_STD()
    return 0;
}

JNIEXPORT jlongArray JNICALL Java_io_realm_internal_SharedGroup_nativeAdvanceRead
(JNIEnv *env, jobject, jlong native_ptr, jlongArray observedRows, jintArray observedTables)
{
    TR_ENTER_PTR(native_ptr)
    try {
        jsize rowCount = env->GetArrayLength(observedRows);
        jsize tableCount = env->GetArrayLength(observedTables);
        if (rowCount == 0 && tableCount == 0) {
            LangBindHelper::advance_read(*SG(native_ptr));
            return nullptr;
        }

        ModifiedRowParser m;
        LangBindHelper::advance_read(*SG(native_ptr), m);

        std::vector<jlong> mi;
        if (rowCount > 0) {
            PrimitiveArray<Row*> rows(env, observedRows, rowCount);
            size_t i = 0;
            for (auto r : rows) {
                size_t ti = r->get_table()->get_index_in_group();
                if (ti >= m.modified.size())
                    continue;
                if (find(m.modified[ti].begin(), m.modified[ti].end(), r->get_index()) != m.modified[ti].end())
                    mi.push_back(i);
                ++i;
            }
        }

        if (tableCount > 0) {
            PrimitiveArray<int> tables(env, observedTables, tableCount);
            for (auto& t : tables) {
                if (t > m.modified.size() || m.modified[t].size() == 0)
                    t = -1;
            }
        }

        jlongArray ret = env->NewLongArray(mi.size());
        env->SetLongArrayRegion(ret, 0, mi.size(), mi.data());
        return ret;
    }
    CATCH_STD()
    return 0;
}

JNIEXPORT void JNICALL Java_io_realm_internal_SharedGroup_nativePromoteToWrite
  (JNIEnv *env, jobject, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr) 
    try {
        LangBindHelper::promote_to_write( *SG(native_ptr) );
    }
    CATCH_STD()
}

JNIEXPORT void JNICALL Java_io_realm_internal_SharedGroup_nativeCommitAndContinueAsRead
  (JNIEnv *env, jobject, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr)
    try {
        LangBindHelper::commit_and_continue_as_read( *SG(native_ptr) );
    }
    CATCH_STD()
}

JNIEXPORT void JNICALL Java_io_realm_internal_SharedGroup_nativeCloseReplication
  (JNIEnv *, jobject, jlong native_replication_ptr)
{
    TR_ENTER_PTR(native_replication_ptr)
    delete reinterpret_cast<Replication*>(native_replication_ptr);
}

JNIEXPORT void JNICALL Java_io_realm_internal_SharedGroup_nativeClose(
    JNIEnv*, jclass, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr)
    delete SG(native_ptr);
}

JNIEXPORT void JNICALL Java_io_realm_internal_SharedGroup_nativeReserve(
   JNIEnv *env, jobject, jlong native_ptr, jlong bytes)
{
    TR_ENTER_PTR(native_ptr)
    if (bytes <= 0) {
        ThrowException(env, UnsupportedOperation, "number of bytes must be > 0.");
        return;
    }

    try {
         SG(native_ptr)->reserve(S(bytes));
    }
    CATCH_STD()
}

JNIEXPORT jlong JNICALL Java_io_realm_internal_SharedGroup_nativeBeginRead(
    JNIEnv* env, jobject, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr)
    try {
        const Group& group = SG(native_ptr)->begin_read();
        return reinterpret_cast<jlong>(&group);
    }
    CATCH_STD()
    return 0;
}

JNIEXPORT void JNICALL Java_io_realm_internal_SharedGroup_nativeEndRead(
    JNIEnv *, jobject, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr)
    SG(native_ptr)->end_read();     // noexcept
}

JNIEXPORT jlong JNICALL Java_io_realm_internal_SharedGroup_nativeBeginWrite(
    JNIEnv* env, jobject, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr)
    try {
        Group& group = SG(native_ptr)->begin_write();
        return reinterpret_cast<jlong>(&group);
    }
    CATCH_STD()
    return 0;
}

JNIEXPORT void JNICALL Java_io_realm_internal_SharedGroup_nativeCommit(
    JNIEnv*, jobject, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr)
    SG(native_ptr)->commit();   // noexcept
}

JNIEXPORT void JNICALL Java_io_realm_internal_SharedGroup_nativeRollback(
    JNIEnv*, jobject, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr)
    SG(native_ptr)->rollback();   // noexcept
}

JNIEXPORT void JNICALL Java_io_realm_internal_SharedGroup_nativeRollbackAndContinueAsRead(
    JNIEnv *, jobject, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr)
    LangBindHelper::rollback_and_continue_as_read(*SG(native_ptr));
}


JNIEXPORT jboolean JNICALL Java_io_realm_internal_SharedGroup_nativeHasChanged
  (JNIEnv *, jobject, jlong native_ptr)
{
    TR_ENTER_PTR(native_ptr)
    return SG(native_ptr)->has_changed();   // noexcept
}

JNIEXPORT jstring JNICALL Java_io_realm_internal_SharedGroup_nativeGetDefaultReplicationDatabaseFileName(
    JNIEnv* env, jclass)
{
    TR_ENTER()
#ifdef REALM_ENABLE_REPLICATION
    ThrowException(env, UnsupportedOperation,
                   "Replication is not currently supported by the Java language binding.");
    return 0;
//    return to_jstring(env, Replication::get_path_to_database_file());
#else
    ThrowException(env, UnsupportedOperation,
                   "Replication was disable in the native library at compile time");
    return 0;
#endif
}
