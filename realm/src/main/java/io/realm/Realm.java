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

package io.realm;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import io.realm.exceptions.RealmEncryptionNotSupportedException;
import io.realm.exceptions.RealmException;
import io.realm.exceptions.RealmIOException;
import io.realm.exceptions.RealmMigrationNeededException;
import io.realm.internal.ColumnIndices;
import io.realm.internal.RealmObjectProxy;
import io.realm.internal.RealmProxyMediator;
import io.realm.internal.Table;
import io.realm.internal.TableView;
import io.realm.internal.UncheckedRow;
import io.realm.internal.Util;
import io.realm.internal.log.RealmLog;

/**
 * {@link Realm} クラスは永続化されたオブジェクトのためのストレージとトランザクションを管理します。
 * また、{@link RealmObject}のインスタンス生成も担当しています。Realm内に保存されたオブジェクトは
 * いつでも検索し読みだすことができます。オブジェクトの作成、変更、削除はトランザクションの中で
 * 行われる必要があります。詳しくは {@link #beginTransaction()} を参照してください。
 * <p>
 * トランザクションは、異なるスレッド上のそれぞれのRealmインスタンスが同じオブジェクトに対して
 * アクセスする際の完全なACID特性を保証します。
 * <p>
 * Realmインスタンスにに対する処理が完了した際は{@link #close()}メソッドを忘れずに呼び出すことは
 * とても重要です。これを忘れると、{@link OutOfMemoryError}の発生やJNI層のリソースのリークを
 * 引き起こします。
 * <p>
 * Realmインスタンスは１つのインスタンスからしか使用することができません。これは、Realmを使用する
 * スレッドはそれぞれが個別にRealmインスタンスをオープンする必要があることを意味します。
 * Realmインスタンスは参照カウントを用いてスレッド単位でキャッシュされるため、参照カウントが正数の
 * 間は{@link #getInstance(RealmConfiguration)}は単にキャッシュされたインスタンスを返す軽量な操作です。
 * <p>
 * このことは、UIスレッドでのRealmのオープン/クローズは{@code onCreate()}/{@code onDestroy()}や
 * {@code onStart()}/{@code onStop()}で行うことが望ましいことを意味します。
 * <p>
 * Realmインスタンスは{@link Handler}を使ってスレッド間の連係を行います。このことは、{@link Looper}を
 * 持たないスレッド上でオープンされたRealmインスタンスは、手動で{@link #refresh()}を呼ばないかぎり
 * 他のスレッドでの更新を受け取ることができないことを意味します。
 * <p>
 * AndroidのアクティビティでのRealmの標準的な使い方は以下の通りです。
 * <p>
 * <pre>
 * public class RealmActivity extends Activity {
 *
 *   private Realm realm;
 *
 *   \@Override
 *   protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     setContentView(R.layout.layout_main);
 *     realm = Realm.getInstance(this);
 *   }
 *
 *   \@Override
 *   protected void onDestroy() {
 *     super.onDestroy();
 *     realm.close();
 *   }
 * }
 * </pre>
 * <p>
 * Realmは16 MBまでの文字列とバイト配列のフィールドをサポートします。
 * <p>
 * @see <a href="https://ja.wikipedia.org/wiki/ACID_(%E3%82%B3%E3%83%B3%E3%83%94%E3%83%A5%E3%83%BC%E3%82%BF%E7%A7%91%E5%AD%A6)">ACID</a>
 * @see <a href="https://github.com/realm/realm-java/tree/master/examples">Examples using Realm</a>
 */
public final class Realm extends BaseRealm {

    public static final String DEFAULT_REALM_NAME = RealmConfiguration.DEFAULT_REALM_NAME;

    protected static final ThreadLocal<Map<RealmConfiguration, Realm>> realmsCache =
            new ThreadLocal<Map<RealmConfiguration, Realm>>() {
                @Override
                protected Map<RealmConfiguration, Realm> initialValue() {
                    return new HashMap<RealmConfiguration, Realm>();
                }
            };

    private static final ThreadLocal<Map<RealmConfiguration, Integer>> referenceCount =
            new ThreadLocal<Map<RealmConfiguration,Integer>>() {
                @Override
                protected Map<RealmConfiguration, Integer> initialValue() {
                    return new HashMap<RealmConfiguration, Integer>();
                }
            };

    // List of Realm files that has already been validated
    private static final Set<String> validatedRealmFiles = new HashSet<String>();

    // Caches Class objects (both model classes and proxy classes) to Realm Tables
    private final Map<Class<? extends RealmObject>, Table> classToTable =
            new HashMap<Class<? extends RealmObject>, Table>();

    private static RealmConfiguration defaultConfiguration;
    protected ColumnIndices columnIndices = new ColumnIndices();

    /**
     * The constructor is private to enforce the use of the static one.
     *
     * @param configuration {@link RealmConfiguration} used to open the Realm.
     * @param autoRefresh {@code true} if Realm should auto-refresh. {@code false} otherwise.
     * @throws IllegalArgumentException if trying to open an encrypted Realm with the wrong key.
     * @throws RealmEncryptionNotSupportedException if the device doesn't support Realm encryption.
     */
    private Realm(RealmConfiguration configuration, boolean autoRefresh) {
        super(configuration, autoRefresh);
    }

    @Override
    protected void finalize() throws Throwable {
        if (sharedGroupManager != null && sharedGroupManager.isOpen()) {
            RealmLog.w("Remember to call close() on all Realm instances. " +
                            "Realm " + configuration.getPath() + " is being finalized without being closed, " +
                            "this can lead to running out of native memory."
            );
        }
        super.finalize();
    }

    /**
     * デフォルトのRealmファイル{@value io.realm.RealmConfiguration#DEFAULT_REALM_NAME}を対象とする
     * Realmインスタンスを返すstaticコンストラクタです。
     * このメソッドは{@code Realm.getInstance(new RealmConfiguration(getContext()).build()) }と
     * 等価です。
     *
     * このコンストラクタは簡易利用のために提供されています。
     * {@link #getInstance(RealmConfiguration)}または{@link #getDefaultInstance()}の利用を推奨します。
     *
     * @param context Androidの{@link android.content.Context}。{@code null}不可。
     * @return Realmインスタンスを返します。
     *
     * @throws java.lang.IllegalArgumentException {@link Context}が{@code null}の場合にスローされます。
     * @throws RealmMigrationNeededException モデルクラスが変更されたためマイグレーションが必要な場合に
     *                                       スローされます。
     * @throws RealmIOException              ファイルアクセスでエラーがが発生した場合にスローされます。
     */
    public static Realm getInstance(Context context) {
        return Realm.getInstance(new RealmConfiguration.Builder(context)
                .name(DEFAULT_REALM_NAME)
                .build());
    }

    /**
     * {@link #setDefaultConfiguration(RealmConfiguration)}でセットされた{@link RealmConfiguration}
     * にしたがって作成されたRealmインスタンスを返すstaticコンストラクタです。
     *
     * @return Realmインスタンスを返します。
     *
     * @throws java.lang.NullPointerException デフォルトの{@link RealmConfiguration}がセットされて
     *                                        いない場合にスローされます。
     * @throws RealmMigrationNeededException  モデルクラスの変更やスキーマバージョンの変更により
     *                                        マイグレーションが必要な状況で、デフォルトコンフィ
     *                                        ギュレーションにマイグレーション処理が指定されていない
     *                                        場合にスローされます。
     */
    public static Realm getDefaultInstance() {
        if (defaultConfiguration == null) {
            throw new NullPointerException("No default RealmConfiguration was found. Call setDefaultConfiguration() first");
        }
        return create(defaultConfiguration);
    }

    /**
     * 引き数で渡された{@link RealmConfiguration}に従ったRealmインスタンスを返すstaticコンストラクタです。
     *
     * @param configuration 取得する Realm の設定情報を保持した {@link RealmConfiguration}
     * @return Realmインスタンスを返します。
     *
     * @throws RealmMigrationNeededException        モデルクラスの変更やスキーマバージョンの変更により
     *                                              マイグレーションが必要な状況で、デフォルト
     *                                              コンフィギュレーションにマイグレーション処理が
     *                                              指定されていない場合にスローされます。
     * @throws RealmEncryptionNotSupportedException デバイスがRealmデータベースの暗号化に対応して
     *                                              いない場合にスローされます。
     * @see RealmConfiguration Realmの設定方法の詳細についてはこちらを参照してください。
     */
    public static Realm getInstance(RealmConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("A non-null RealmConfiguration must be provided");
        }
        return create(configuration);
    }

    /**
     * {@link #getDefaultInstance()}で使用する{@link io.realm.RealmConfiguration}をセットします。
     *
     * @param configuration RealmConfiguration デフォルトコンフィギュレーションとして使用する
     *                      {@link RealmConfiguration}。
     * @see RealmConfiguration Realmの設定方法の詳細についてはこちらを参照してください。
     */
    public static void setDefaultConfiguration(RealmConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("A non-null RealmConfiguration must be provided");
        }
        defaultConfiguration = configuration;
    }

    /**
     * デフォルトコンフィギュレーションを削除します。{@link #setDefaultConfiguration(RealmConfiguration)}
     * で新たなデフォルトコンフィギュレーションがセットされるまで、以降の{@link #getDefaultInstance()}の
     * 呼び出しは失敗します。
     */
    public static void removeDefaultConfiguration() {
        defaultConfiguration = null;
    }

    private static synchronized Realm create(RealmConfiguration configuration) {
        boolean autoRefresh = Looper.myLooper() != null;
        try {
            boolean validateSchema = !validatedRealmFiles.contains(configuration.getPath());
            return createAndValidate(configuration, validateSchema, autoRefresh);

        } catch (RealmMigrationNeededException e) {
            if (configuration.shouldDeleteRealmIfMigrationNeeded()) {
                deleteRealm(configuration);
            } else {
                migrateRealm(configuration);
            }

            return createAndValidate(configuration, true, autoRefresh);
        }
    }

    private static Realm createAndValidate(RealmConfiguration configuration, boolean validateSchema, boolean autoRefresh) {
        synchronized (BaseRealm.class) {
            // Check if a cached instance already exists for this thread
            String canonicalPath = configuration.getPath();
            Map<RealmConfiguration, Integer> localRefCount = referenceCount.get();
            Integer references = localRefCount.get(configuration);
            if (references == null) {
                references = 0;
            }
            Map<RealmConfiguration, Realm> realms = realmsCache.get();
            Realm realm = realms.get(configuration);
            if (realm != null) {
                localRefCount.put(configuration, references + 1);
                return realm;
            }

            // Create new Realm and cache it. All exception code paths must close the Realm otherwise we risk serving
            // faulty cache data.
            validateAgainstExistingConfigurations(configuration);
            realm = new Realm(configuration, autoRefresh);
            List<RealmConfiguration> pathConfigurationCache = globalPathConfigurationCache.get(canonicalPath);
            if (pathConfigurationCache == null) {
                pathConfigurationCache = new CopyOnWriteArrayList<RealmConfiguration>();
                globalPathConfigurationCache.put(canonicalPath, pathConfigurationCache);
            }
            pathConfigurationCache.add(configuration);
            realms.put(configuration, realm);
            localRefCount.put(configuration, references + 1);

            // Increment global reference counter
            realm.acquireFileReference(configuration);

            // Check versions of Realm
            long currentVersion = realm.getVersion();
            long requiredVersion = configuration.getSchemaVersion();
            if (currentVersion != UNVERSIONED && currentVersion < requiredVersion && validateSchema) {
                realm.close();
                throw new RealmMigrationNeededException(configuration.getPath(), String.format("Realm on disk need to migrate from v%s to v%s", currentVersion, requiredVersion));
            }
            if (currentVersion != UNVERSIONED && requiredVersion < currentVersion && validateSchema) {
                realm.close();
                throw new IllegalArgumentException(String.format("Realm on disk is newer than the one specified: v%s vs. v%s", currentVersion, requiredVersion));
            }

            // Initialize Realm schema if needed
            if (validateSchema) {
                try {
                    initializeRealm(realm);
                } catch (RuntimeException e) {
                    realm.close();
                    throw e;
                }
            }
            setupColumnIndices(realm);

            return realm;
        }
    }

    private static void setupColumnIndices(Realm realm) {
        RealmProxyMediator mediator = realm.configuration.getSchemaMediator();
        Set<Class<? extends RealmObject>> modelClasses = mediator.getModelClasses();
        for (Class<? extends RealmObject> modelClass : modelClasses) {
            realm.columnIndices.addClass(modelClass, mediator.getColumnIndices(modelClass));
        }
    }

    @SuppressWarnings("unchecked")
    private static void initializeRealm(Realm realm) {
        long version = realm.getVersion();
        boolean commitNeeded = false;
        try {
            realm.beginTransaction();
            if (version == UNVERSIONED) {
                commitNeeded = true;
                realm.setVersion(realm.configuration.getSchemaVersion());
            }

            RealmProxyMediator mediator = realm.configuration.getSchemaMediator();
            for (Class<? extends RealmObject> modelClass : mediator.getModelClasses()) {
                // Create and validate table
                if (version == UNVERSIONED) {
                    mediator.createTable(modelClass, realm.sharedGroupManager.getTransaction());
                }
                mediator.validateTable(modelClass, realm.sharedGroupManager.getTransaction());
            }
            validatedRealmFiles.add(realm.getPath());
        } finally {
            if (commitNeeded) {
                realm.commitTransaction();
            } else {
                realm.cancelTransaction();
            }
        }
    }

    /**
     * JSONArrayの中のそれぞれのオブジェクトに対してRealmオブジェクトを作成します。このメソッドは
     * トランザクションの中で呼び出す必要があります。
     * JSONプロパティのうち、値が{@code null}のものは対応するデータ型のデフォルト値に置き換えられます。
     * また、モデルに含まれていないプロパティは無視します。
     *
     * @param clazz 作成するRealmオブジェクトの型。
     * @param json  指定された種類のRealmオブジェクトに変換される要素を保持しているJSONArray。
     *
     * @throws RealmException 変換が失敗した場合にスローされます。
     */
    public <E extends RealmObject> void createAllFromJson(Class<E> clazz, JSONArray json) {
        if (clazz == null || json == null) {
            return;
        }

        for (int i = 0; i < json.length(); i++) {
            try {
                configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json.getJSONObject(i), false);
            } catch (Exception e) {
                throw new RealmException("Could not map Json", e);
            }
        }
    }

    /**
     * プライマリキーによって特定される既存オブジェクト群を、わたされたJSONデータで更新します。
     * 対応する既存オブジェクトがRealm内に存在しない場合は、新規にオブジェクトの作成を行います。
     * このメソッドはトランザクション内で呼び出す必要があります。
     *
     * @param clazz 作成/更新する{@link io.realm.RealmObject}の型。プライマリキーが指定されている必要があります。
     * @param json  更新データを保持する JSONArray。
     *
     * @throws java.lang.IllegalArgumentException 指定された型が{@link io.realm.annotations.PrimaryKey}の指定されたプロパティを持っていない場合。
     * @see #createAllFromJson(Class, org.json.JSONArray)
     */
    public <E extends RealmObject> void createOrUpdateAllFromJson(Class<E> clazz, JSONArray json) {
        if (clazz == null || json == null) {
            return;
        }
        checkHasPrimaryKey(clazz);
        for (int i = 0; i < json.length(); i++) {
            try {
                configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json.getJSONObject(i), true);
            } catch (Exception e) {
                throw new RealmException("Could not map Json", e);
            }
        }
    }

    /**
     * 配列形式のJSON文字列中の各要素に対してそれぞれ対応するRealmオブジェクトを作成します。
     * このメソッドはトランザクションの中で呼び出す必要があります。
     * JSONプロパティのうち、値が{@code null}のものは対応するデータ型のデフォルト値に置き換えられます。
     * また、モデルに含まれていないプロパティは無視します。
     *
     * @param clazz 作成するRealmオブジェクトの型。
     * @param json  指定された種類のRealmオブジェクトに変換される要素を保持している配列形式のJSON文字列。
     *
     * @throws RealmException 変換が失敗した場合にスローされます。
     */
    public <E extends RealmObject> void createAllFromJson(Class<E> clazz, String json) {
        if (clazz == null || json == null || json.length() == 0) {
            return;
        }

        JSONArray arr;
        try {
            arr = new JSONArray(json);
        } catch (Exception e) {
            throw new RealmException("Could not create JSON array from string", e);
        }

        createAllFromJson(clazz, arr);
    }

    /**
     * プライマリキーによって特定される既存オブジェクト群を、わたされたJSONデータで更新します。
     * 対応する既存オブジェクトがRealm内に存在しない場合は、新規にオブジェクトの作成を行います。
     * このメソッドはトランザクション内で呼び出す必要があります。
     *
     * @param clazz 作成/更新する{@link io.realm.RealmObject}の型。プライマリキーが指定されている必要があります。
     * @param json  JSON配列の文字列。
     *
     * @throws java.lang.IllegalArgumentException 指定された型が{@link io.realm.annotations.PrimaryKey}の指定されたプロパティを持っていない場合。
     * @see #createAllFromJson(Class, String)
     */
    public <E extends RealmObject> void createOrUpdateAllFromJson(Class<E> clazz, String json) {
        if (clazz == null || json == null || json.length() == 0) {
            return;
        }
        checkHasPrimaryKey(clazz);

        JSONArray arr;
        try {
            arr = new JSONArray(json);
        } catch (JSONException e) {
            throw new RealmException("Could not create JSON array from string", e);
        }

        createOrUpdateAllFromJson(clazz, arr);
    }

    /**
     * 配列形式のJSON文字列中の各要素に対してそれぞれ対応するRealmオブジェクトを作成します。
     * このメソッドはトランザクションの中で呼び出す必要があります。
     * JSONプロパティのうち、値が{@code null}のものは対応するデータ型のデフォルト値に置き換えられます。
     * また、モデルに含まれていないプロパティは無視します。
     *
     * @param clazz       作成するRealmオブジェクトの型。
     * @param inputStream 指定された種類のRealmオブジェクトに変換される要素を保持している配列形式のJSON文字列のストリーム。
     *
     * @throws RealmException 変換が失敗した場合にスローされます。
     * @throws IOException    {@code inputStream}からの読み込みに失敗した場合にスローされます。
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public <E extends RealmObject> void createAllFromJson(Class<E> clazz, InputStream inputStream) throws IOException {
        if (clazz == null || inputStream == null) {
            return;
        }

        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        try {
            reader.beginArray();
            while (reader.hasNext()) {
                configuration.getSchemaMediator().createUsingJsonStream(clazz, this, reader);
            }
            reader.endArray();
        } finally {
            reader.close();
        }
    }

    /**
     * プライマリキーによって特定される既存オブジェクト群を、わたされたJSONデータで更新します。
     * 対応する既存オブジェクトがRealm内に存在しない場合は、新規にオブジェクトの作成を行います。
     * このメソッドはトランザクション内で呼び出す必要があります。
     *
     * @param clazz 作成/更新する{@link io.realm.RealmObject}の型。プライマリキーが指定されている必要があります。
     * @param in    指定された種類のRealmオブジェクトに変換される要素を保持している配列形式のJSON文字列のストリーム。
     *
     * @throws java.lang.IllegalArgumentException 指定された型が{@link io.realm.annotations.PrimaryKey}の指定されたプロパティを持っていない場合。
     * @see #createOrUpdateAllFromJson(Class, java.io.InputStream)
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public <E extends RealmObject> void createOrUpdateAllFromJson(Class<E> clazz, InputStream in) throws IOException {
        if (clazz == null || in == null) {
            return;
        }
        checkHasPrimaryKey(clazz);

        // As we need the primary key value we have to first parse the entire input stream as in the general
        // case that value might be the last property :(
        Scanner scanner = null;
        try {
            scanner = getFullStringScanner(in);
            JSONArray json = new JSONArray(scanner.next());
            for (int i = 0; i < json.length(); i++) {
                configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json.getJSONObject(i), true);
            }
        } catch (JSONException e) {
            throw new RealmException("Failed to read JSON", e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * JSONオブジェクトの保持するデータでRealmオブジェクトを作成します。
     * このメソッドはトランザクションの中で呼び出す必要があります。
     * JSONプロパティのうち、値が{@code null}のものは対応するデータ型のデフォルト値に置き換えられます。
     * また、モデルに含まれていないプロパティは無視します。
     *
     * @param clazz 作成するRealmオブジェクトの型。
     * @param json  作成するオブジェクトのデータを保持しているJSONObject。
     * @return 作成されたRealmオブジェクト。{@code json}が{@code null}の場合は{@code null}を返します。
     *
     * @throws RealmException 変換が失敗した場合にスローされます。
     * @see #createOrUpdateObjectFromJson(Class, org.json.JSONObject)
     */
    public <E extends RealmObject> E createObjectFromJson(Class<E> clazz, JSONObject json) {
        if (clazz == null || json == null) {
            return null;
        }

        try {
            return configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json, false);
        } catch (Exception e) {
            throw new RealmException("Could not map Json", e);
        }
    }

    /**
     * Tries to update an existing object defined by its primary key with new JSON data. If no existing object could be
     * found a new object will be saved in the Realm. This must happen within a transaction.
     *
     * @param clazz Type of {@link io.realm.RealmObject} to create or update. It must have a primary key defined.
     * @param json  {@link org.json.JSONObject} with object data.
     * @return Created or updated {@link io.realm.RealmObject}.
     * @throws java.lang.IllegalArgumentException if trying to update a class without a
     * {@link io.realm.annotations.PrimaryKey}.
     * @see #createObjectFromJson(Class, org.json.JSONObject)
     */
    public <E extends RealmObject> E createOrUpdateObjectFromJson(Class<E> clazz, JSONObject json) {
        if (clazz == null || json == null) {
            return null;
        }
        checkHasPrimaryKey(clazz);
        try {
            return configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json, true);
        } catch (JSONException e) {
            throw new RealmException("Could not map Json", e);
        }
    }

    /**
     * Create a Realm object pre-filled with data from a JSON object. This must be done inside a
     * transaction. JSON properties with a null value will map to the default value for the data
     * type in Realm and unknown properties will be ignored.
     *
     * @param clazz Type of Realm object to create.
     * @param json  JSON string with object data.
     * @return Created object or null if json string was empty or null.
     *
     * @throws RealmException 変換が失敗した場合にスローされます。
     */
    public <E extends RealmObject> E createObjectFromJson(Class<E> clazz, String json) {
        if (clazz == null || json == null || json.length() == 0) {
            return null;
        }

        JSONObject obj;
        try {
            obj = new JSONObject(json);
        } catch (Exception e) {
            throw new RealmException("Could not create Json object from string", e);
        }

        return createObjectFromJson(clazz, obj);
    }

    /**
     * Tries to update an existing object defined by its primary key with new JSON data. If no existing object could be
     * found a new object will be saved in the Realm. This must happen within a transaction.
     *
     * @param clazz Type of {@link io.realm.RealmObject} to create or update. It must have a primary key defined.
     * @param json  String with object data in JSON format.
     * @return Created or updated {@link io.realm.RealmObject}.
     * @throws java.lang.IllegalArgumentException if trying to update a class without a
     * {@link io.realm.annotations.PrimaryKey}.
     *
     * @see #createObjectFromJson(Class, String)
     */
    public <E extends RealmObject> E createOrUpdateObjectFromJson(Class<E> clazz, String json) {
        if (clazz == null || json == null || json.length() == 0) {
            return null;
        }
        checkHasPrimaryKey(clazz);

        JSONObject obj;
        try {
            obj = new JSONObject(json);
        } catch (Exception e) {
            throw new RealmException("Could not create Json object from string", e);
        }

        return createOrUpdateObjectFromJson(clazz, obj);
    }

    /**
     * Create a Realm object pre-filled with data from a JSON object. This must be done inside a
     * transaction. JSON properties with a null value will map to the default value for the data
     * type in Realm and unknown properties will be ignored.
     *
     * @param clazz         Type of Realm object to create.
     * @param inputStream   JSON object data as a InputStream.
     * @return Created object or null if json string was empty or null.
     *
     * @throws RealmException 変換が失敗した場合にスローされます。
     * @throws IOException if something was wrong with the input stream.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public <E extends RealmObject> E createObjectFromJson(Class<E> clazz, InputStream inputStream) throws IOException {
        if (clazz == null || inputStream == null) {
            return null;
        }

        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        try {
            return configuration.getSchemaMediator().createUsingJsonStream(clazz, this, reader);
        } finally {
            reader.close();
        }
    }

    /**
     * Tries to update an existing object defined by its primary key with new JSON data. If no existing object could be
     * found a new object will be saved in the Realm. This must happen within a transaction.
     *
     * @param clazz Type of {@link io.realm.RealmObject} to create or update. It must have a primary key defined.
     * @param in    {@link InputStream} with object data in JSON format.
     * @return Created or updated {@link io.realm.RealmObject}.
     * @throws java.lang.IllegalArgumentException if trying to update a class without a
     * {@link io.realm.annotations.PrimaryKey}.
     * @see #createObjectFromJson(Class, java.io.InputStream)
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public <E extends RealmObject> E createOrUpdateObjectFromJson(Class<E> clazz, InputStream in) throws IOException {
        if (clazz == null || in == null) {
            return null;
        }
        checkHasPrimaryKey(clazz);

        // As we need the primary key value we have to first parse the entire input stream as in the general
        // case that value might be the last property :(
        Scanner scanner = null;
        try {
            scanner = getFullStringScanner(in);
            JSONObject json = new JSONObject(scanner.next());
            return configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json, true);
        } catch (JSONException e) {
            throw new RealmException("Failed to read JSON", e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private Scanner getFullStringScanner(InputStream in) {
        return new Scanner(in, "UTF-8").useDelimiter("\\A");
    }

    /**
     * Instantiates and adds a new object to the Realm.
     *
     * @param clazz The Class of the object to create
     * @return The new object
     * @throws RealmException An object could not be created
     */
    public <E extends RealmObject> E createObject(Class<E> clazz) {
        Table table = getTable(clazz);
        long rowIndex = table.addEmptyRow();
        return get(clazz, rowIndex);
    }

    /**
     * Creates a new object inside the Realm with the Primary key value initially set.
     * If the value violates the primary key constraint, no object will be added and a
     * {@link RealmException} will be thrown.
     *
     * @param clazz The Class of the object to create
     * @param primaryKeyValue Value for the primary key field.
     * @return The new object
     * @throws RealmException if object could not be created.
     */
    <E extends RealmObject> E createObject(Class<E> clazz, Object primaryKeyValue) {
        Table table = getTable(clazz);
        long rowIndex = table.addEmptyRowWithPrimaryKey(primaryKeyValue);
        return get(clazz, rowIndex);
    }

    void remove(Class<? extends RealmObject> clazz, long objectIndex) {
        getTable(clazz).moveLastOver(objectIndex);
    }

    <E extends RealmObject> E get(Class<E> clazz, long rowIndex) {
        Table table = getTable(clazz);
        UncheckedRow row = table.getUncheckedRow(rowIndex);
        E result = configuration.getSchemaMediator().newInstance(clazz);
        result.row = row;
        result.realm = this;
        return result;
    }

    /**
     * Copies a RealmObject to the Realm instance and returns the copy. Any further changes to the original RealmObject
     * will not be reflected in the Realm copy. This is a deep copy, so all referenced objects will be copied. Objects
     * already in this Realm will be ignored.
     *
     * @param object {@link io.realm.RealmObject} to copy to the Realm.
     * @return A managed RealmObject with its properties backed by the Realm.
     *
     * @throws java.lang.IllegalArgumentException if RealmObject is {@code null}.
     */
    public <E extends RealmObject> E copyToRealm(E object) {
        checkNotNullObject(object);
        return copyOrUpdate(object, false);
    }

    /**
     * Updates an existing RealmObject that is identified by the same {@link io.realm.annotations.PrimaryKey} or create
     * a new copy if no existing object could be found. This is a deep copy or update, so all referenced objects will be
     * either copied or updated.
     *
     * @param object    {@link io.realm.RealmObject} to copy or update.
     * @return The new or updated RealmObject with all its properties backed by the Realm.
     *
     * @throws java.lang.IllegalArgumentException if RealmObject is {@code null} or doesn't have a Primary key defined.
     * @see #copyToRealm(RealmObject)
     */
    public <E extends RealmObject> E copyToRealmOrUpdate(E object) {
        checkNotNullObject(object);
        checkHasPrimaryKey(object.getClass());
        return copyOrUpdate(object, true);
    }

    /**
     * Copies a collection of RealmObjects to the Realm instance and returns their copy. Any further changes
     * to the original RealmObjects will not be reflected in the Realm copies. This is a deep copy, so all referenced
     * objects will be copied. Objects already in this Realm will be ignored.
     *
     * @param objects RealmObjects to copy to the Realm.
     * @return A list of the the converted RealmObjects that all has their properties managed by the Realm.
     *
     * @throws io.realm.exceptions.RealmException if any of the objects has already been added to Realm.
     * @throws java.lang.IllegalArgumentException if any of the elements in the input collection is {@code null}.
     */
    public <E extends RealmObject> List<E> copyToRealm(Iterable<E> objects) {
        if (objects == null) {
            return new ArrayList<E>();
        }

        ArrayList<E> realmObjects = new ArrayList<E>();
        for (E object : objects) {
            realmObjects.add(copyToRealm(object));
        }

        return realmObjects;
    }

    /**
     * Updates a list of existing RealmObjects that is identified by their {@link io.realm.annotations.PrimaryKey} or create a
     * new copy if no existing object could be found. This is a deep copy or update, so all referenced objects will be
     * either copied or updated.
     *
     * @param objects   List of objects to update or copy into Realm.
     * @return A list of all the new or updated RealmObjects.
     *
     * @throws java.lang.IllegalArgumentException if RealmObject is {@code null} or doesn't have a Primary key defined.
     * @see #copyToRealm(Iterable)
     */
    public <E extends RealmObject> List<E> copyToRealmOrUpdate(Iterable<E> objects) {
        if (objects == null) {
            return new ArrayList<E>();
        }

        ArrayList<E> realmObjects = new ArrayList<E>();
        for (E object : objects) {
            realmObjects.add(copyToRealmOrUpdate(object));
        }

        return realmObjects;
    }

    boolean contains(Class<? extends RealmObject> clazz) {
        return configuration.getSchemaMediator().getModelClasses().contains(clazz);
    }

    /**
     * Returns a typed RealmQuery, which can be used to query for specific objects of this type
     *
     * @param clazz The class of the object which is to be queried for
     * @return A typed RealmQuery, which can be used to query for specific objects of this type
     * @throws java.lang.RuntimeException Any other error
     * @see io.realm.RealmQuery
     */
    public <E extends RealmObject> RealmQuery<E> where(Class<E> clazz) {
        checkIfValid();
        return new RealmQuery<E>(this, clazz);
    }

    /**
     * Get all objects of a specific Class. If no objects exist, the returned RealmResults will not
     * be null. The RealmResults.size() to check the number of objects instead.
     *
     * @param clazz the Class to get objects of
     * @return A RealmResult list containing the objects
     * @throws java.lang.RuntimeException Any other error
     * @see io.realm.RealmResults
     */
    public <E extends RealmObject> RealmResults<E> allObjects(Class<E> clazz) {
        return where(clazz).findAll();
    }

    /**
     * Get all objects of a specific Class sorted by a field.  If no objects exist, the returned
     * RealmResults will not be null. The RealmResults.size() to check the number of objects instead.
     *
     * @param clazz the Class to get objects of.
     * @param fieldName the field name to sort by.
     * @param sortAscending sort ascending if SORT_ORDER_ASCENDING, sort descending if SORT_ORDER_DESCENDING.
     * @return A sorted RealmResults containing the objects.
     * @throws java.lang.IllegalArgumentException if field name does not exist.
     */
    public <E extends RealmObject> RealmResults<E> allObjectsSorted(Class<E> clazz, String fieldName,
                                                                    boolean sortAscending) {
        checkIfValid();
        Table table = getTable(clazz);
        TableView.Order order = sortAscending ? TableView.Order.ascending : TableView.Order.descending;
        long columnIndex = columnIndices.getColumnIndex(clazz, fieldName);
        if (columnIndex < 0) {
            throw new IllegalArgumentException(String.format("Field name '%s' does not exist.", fieldName));
        }

        TableView tableView = table.getSortedView(columnIndex, order);
        return new RealmResults<E>(this, tableView, clazz);
    }


    /**
     * Get all objects of a specific class sorted by two specific field names.  If no objects exist,
     * the returned RealmResults will not be null. The RealmResults.size() to check the number of
     * objects instead.
     *
     * @param clazz the class ti get objects of.
     * @param fieldName1 first field name to sort by.
     * @param sortAscending1 sort order for first field.
     * @param fieldName2 second field name to sort by.
     * @param sortAscending2 sort order for second field.
     * @return A sorted RealmResults containing the objects.
     * @throws java.lang.IllegalArgumentException if a field name does not exist.
     */
    public <E extends RealmObject> RealmResults<E> allObjectsSorted(Class<E> clazz, String fieldName1,
                                                                    boolean sortAscending1, String fieldName2,
                                                                    boolean sortAscending2) {
        return allObjectsSorted(clazz, new String[]{fieldName1, fieldName2}, new boolean[]{sortAscending1,
                sortAscending2});
    }

    /**
     * Get all objects of a specific class sorted by two specific field names.  If no objects exist,
     * the returned RealmResults will not be null. The RealmResults.size() to check the number of
     * objects instead.
     *
     * @param clazz the class ti get objects of.
     * @param fieldName1 first field name to sort by.
     * @param sortAscending1 sort order for first field.
     * @param fieldName2 second field name to sort by.
     * @param sortAscending2 sort order for second field.
     * @param fieldName3 third field name to sort by.
     * @param sortAscending3 sort order for third field.
     * @return A sorted RealmResults containing the objects.
     * @throws java.lang.IllegalArgumentException if a field name does not exist.
     */
    public <E extends RealmObject> RealmResults<E> allObjectsSorted(Class<E> clazz, String fieldName1,
                                                                    boolean sortAscending1,
                                                                    String fieldName2, boolean sortAscending2,
                                                                    String fieldName3, boolean sortAscending3) {
        return allObjectsSorted(clazz, new String[]{fieldName1, fieldName2, fieldName3},
                new boolean[]{sortAscending1, sortAscending2, sortAscending3});
    }

    /**
     * Get all objects of a specific Class sorted by multiple fields.  If no objects exist, the
     * returned RealmResults will not be null. The RealmResults.size() to check the number of
     * objects instead.
     *
     * @param clazz the Class to get objects of.
     * @param sortAscending sort ascending if SORT_ORDER_ASCENDING, sort descending if SORT_ORDER_DESCENDING.
     * @param fieldNames an array of field names to sort objects by.
     *        The objects are first sorted by fieldNames[0], then by fieldNames[1] and so forth.
     * @return A sorted RealmResults containing the objects.
     * @throws java.lang.IllegalArgumentException if a field name does not exist.
     */
    @SuppressWarnings("unchecked")
    public <E extends RealmObject> RealmResults<E> allObjectsSorted(Class<E> clazz, String fieldNames[],
                                                                    boolean sortAscending[]) {
        if (fieldNames == null) {
            throw new IllegalArgumentException("fieldNames must be provided.");
        } else if (sortAscending == null) {
            throw new IllegalArgumentException("sortAscending must be provided.");
        }

        // Convert field names to column indices
        Table table = this.getTable(clazz);
        long columnIndices[] = new long[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            long columnIndex = table.getColumnIndex(fieldName);
            if (columnIndex == -1) {
                throw new IllegalArgumentException(String.format("Field name '%s' does not exist.", fieldName));
            }
            columnIndices[i] = columnIndex;
        }

        // Perform sort
        TableView tableView = table.getSortedView(columnIndices, sortAscending);
        return new RealmResults(this, tableView, clazz);
    }

    /**
     * Return change listeners
     * For internal testing purpose only
     *
     * @return changeListeners list of this realm instance
     */
    protected List<WeakReference<RealmChangeListener>> getChangeListeners() {
        return changeListeners;
    }

    @SuppressWarnings("UnusedDeclaration")
    boolean hasChanged() {
        return sharedGroupManager.hasChanged();
    }

    /**
     * Executes a given transaction on the Realm. {@link #beginTransaction()} and
     * {@link #commitTransaction()} will be called automatically. If any exception is thrown
     * during the transaction {@link #cancelTransaction()} will be called instead of {@link #commitTransaction()}.
     *
     * @param transaction {@link io.realm.Realm.Transaction} to execute.
     * @throws RealmException if any error happened during the transaction.
     */
    public void executeTransaction(Transaction transaction) {
        if (transaction == null)
            return;
        beginTransaction();
        try {
            transaction.execute(this);
            commitTransaction();
        } catch (RuntimeException e) {
            cancelTransaction();
            throw new RealmException("Error during transaction.", e);
        } catch (Error e) {
            cancelTransaction();
            throw e;
        }
    }

    /**
     * Remove all objects of the specified class.
     *
     * @param clazz The class which objects should be removed
     * @throws java.lang.RuntimeException Any other error
     */
    public void clear(Class<? extends RealmObject> clazz) {
        getTable(clazz).clear();
    }

    @SuppressWarnings("unchecked")
    private <E extends RealmObject> E copyOrUpdate(E object, boolean update) {
        return configuration.getSchemaMediator().copyOrUpdate(this, object, update, new HashMap<RealmObject, RealmObjectProxy>());
    }

    private <E extends RealmObject> void checkNotNullObject(E object) {
        if (object == null) {
            throw new IllegalArgumentException("Null objects cannot be copied into Realm.");
        }
    }

    private void checkHasPrimaryKey(Class<? extends RealmObject> clazz) {
        if (!getTable(clazz).hasPrimaryKey()) {
            throw new IllegalArgumentException("A RealmObject with no @PrimaryKey cannot be updated: " + clazz.toString());
        }
    }

    @Override
    protected Map<RealmConfiguration, Integer> getLocalReferenceCount() {
        return referenceCount.get();
    }

    @Override
    protected void lastLocalInstanceClosed() {
        validatedRealmFiles.remove(configuration.getPath());
        realmsCache.get().remove(configuration);
    }

    /**
     * Manually trigger the migration associated with a given RealmConfiguration. If Realm is already at the
     * latest version, nothing will happen.
     * @param configuration {@link RealmConfiguration}
     */
    public static void migrateRealm(RealmConfiguration configuration) {
        migrateRealm(configuration, null);
    }

    /**
     * Manually trigger a migration on a RealmMigration.
     *
     * @param configuration {@link RealmConfiguration}
     * @param migration {@link RealmMigration} to run on the Realm. This will override any migration set on the
     * configuration.
     */
    public static void migrateRealm(RealmConfiguration configuration, RealmMigration migration) {
        BaseRealm.migrateRealm(configuration, migration, new MigrationCallback() {

            @Override
            public BaseRealm getRealm(RealmConfiguration configuration) {
                return Realm.createAndValidate(configuration, false, Looper.myLooper() != null);
           }

            @Override
            public void migrationComplete() {
                realmsCache.remove();
            }
        });
    }

    /**
     * Delete the Realm file specified by the given {@link RealmConfiguration} from the filesystem.
     * The Realm must be unused and closed before calling this method.
     *
     * @param configuration A {@link RealmConfiguration}
     * @return false if a file could not be deleted. The failing file will be logged.
     *
     * @throws java.lang.IllegalStateException if trying to delete a Realm that is already open.
     */
    public static boolean deleteRealm(RealmConfiguration configuration) {
        return BaseRealm.deleteRealm(configuration);
    }

    /**
     * Compact a Realm file. A Realm file usually contain free/unused space.
     * This method removes this free space and the file size is thereby reduced.
     * Objects within the Realm files are untouched.
     * <p>
     * The file must be closed before this method is called.<br>
     * The file system should have free space for at least a copy of the Realm file.<br>
     * The Realm file is left untouched if any file operation fails.<br>
     *
     * @param configuration a {@link RealmConfiguration} pointing to a Realm file.
     * @return true if successful, false if any file operation failed
     *
     * @throws java.lang.IllegalStateException if trying to compact a Realm that is already open.
     */
    public static boolean compactRealm(RealmConfiguration configuration) {
        return BaseRealm.compactRealm(configuration);
    }

    // Get the canonical path for a given file
    static String getCanonicalPath(File realmFile) {
        try {
            return realmFile.getCanonicalPath();
        } catch (IOException e) {
            throw new RealmException("Could not resolve the canonical path to the Realm file: " + realmFile.getAbsolutePath());
        }
    }

    // Return all handlers registered for this Realm
    static Map<Handler, String> getHandlers() {
        return handlers;
    }

    // Public because of migrations
    public Table getTable(Class<? extends RealmObject> clazz) {
        Table table = classToTable.get(clazz);
        if (table == null) {
            clazz = Util.getOriginalModelClass(clazz);
            table = sharedGroupManager.getTable(configuration.getSchemaMediator().getTableName(clazz));
            classToTable.put(clazz, table);
        }
        return table;
    }

    /**
     * Returns the default Realm module. This module contains all Realm classes in the current project, but not
     * those from library or project dependencies. Realm classes in these should be exposed using their own module.
     *
     * @return The default Realm module or null if no default module exists.
     * @see io.realm.RealmConfiguration.Builder#setModules(Object, Object...)
     */
    public static Object getDefaultModule() {
        String moduleName = "io.realm.DefaultRealmModule";
        Class<?> clazz;
        try {
            clazz = Class.forName(moduleName);
            Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (InvocationTargetException e) {
            throw new RealmException("Could not create an instance of " + moduleName, e);
        } catch (InstantiationException e) {
            throw new RealmException("Could not create an instance of " + moduleName, e);
        } catch (IllegalAccessException e) {
            throw new RealmException("Could not create an instance of " + moduleName, e);
        }
    }

    /**
     * Encapsulates a Realm transaction.
     * <p>
     * Using this class will automatically handle {@link #beginTransaction()} and {@link #commitTransaction()}
     * If any exception is thrown during the transaction {@link #cancelTransaction()} will be called
     * instead of {@link #commitTransaction()}.
     */
    public interface Transaction {
        void execute(Realm realm);
    }

}
