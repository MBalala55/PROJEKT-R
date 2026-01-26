# Implementacijska Uputstva za Android Tim

## Arhitektura

```
┌─────────────────────────────────────────────────────┐
│         Android Aplikacija (Offline-First)          │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────────────────────────────────────┐   │
│  │  UI Layer (Activities, Fragments)            │   │
│  └────────────────────┬─────────────────────────┘   │
│                       │                             │
│  ┌────────────────────▼─────────────────────────┐   │
│  │  ViewModel (State Management)                │   │
│  └────────────────────┬─────────────────────────┘   │
│                       │                             │
│  ┌────────────────────▼─────────────────────────┐   │
│  │  Repository (Local + Remote)                 │   │
│  │  - Room Database (offline storage)           │   │
│  │  - Retrofit Client (sync with server)        │   │
│  └──────────────────────────────────────────────┘   │
│                                                     │
└─────────────────────────────────────────────────────┘
              │
              │ (When Online)
              │
┌─────────────────────────────────────────────────────┐
│     Elektropregled Backend (Spring Boot API)        │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────────────────────────────────────┐   │
│  │  REST Endpoints (/v1/...)                    │   │
│  └────────────────────┬─────────────────────────┘   │
│                       │                             │
│  ┌────────────────────▼─────────────────────────┐   │
│  │  Spring Security + JWT                       │   │
│  └────────────────────┬─────────────────────────┘   │
│                       │                             │
│  ┌────────────────────▼─────────────────────────┐   │
│  │  Service Layer (Validation, Logic)           │   │
│  └────────────────────┬─────────────────────────┘   │
│                       │                             │
│  ┌────────────────────▼─────────────────────────┐   │
│  │  PostgreSQL 15+ Database                     │   │
│  └──────────────────────────────────────────────┘   │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Korak 1: Setup Projekta (Android)

### Gradle Dependencies

```gradle
dependencies {
    // Networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // Local Database
    def room_version = "2.6.1"
    implementation "androidx.room:room-runtime:$room_version"
    annotationProcessor "androidx.room:room-compiler:$room_version"
    
    // Lifecycle & ViewModel
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.2"
    
    // JSON
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

---

## Korak 2: Definiranje Data Modela

### Room Database Entity za Pregled

```kotlin
@Entity(tableName = "pregledi")
data class PregledEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "lokalni_id")
    val lokalniId: String,  // UUID
    
    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,  // Null dok se ne sinkronizira
    
    @ColumnInfo(name = "id_korisnika")
    val idKorisnika: Int,
    
    @ColumnInfo(name = "id_postr")
    val idPostr: Int,
    
    @ColumnInfo(name = "pocetak")
    val pocetak: LocalDateTime,
    
    @ColumnInfo(name = "kraj")
    val kraj: LocalDateTime? = null,
    
    @ColumnInfo(name = "napomena")
    val napomena: String? = null,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "pending",  // pending, synced, failed
    
    @ColumnInfo(name = "kreirano")
    val kreirano: LocalDateTime = LocalDateTime.now(),
    
    @ColumnInfo(name = "azurirano")
    val azurirano: LocalDateTime = LocalDateTime.now()
)
```

### Room Database Entity za Stavku Pregleda

```kotlin
@Entity(
    tableName = "stavke_pregleda",
    foreignKeys = [
        ForeignKey(
            entity = PregledEntity::class,
            parentColumns = ["id"],
            childColumns = ["id_pregled"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StavkaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "id_pregled")
    val idPregled: Long,  // FK -> PregledEntity
    
    @ColumnInfo(name = "lokalni_id")
    val lokalniId: String,  // UUID
    
    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,
    
    @ColumnInfo(name = "id_ured")
    val idUred: Int,
    
    @ColumnInfo(name = "id_parametra")
    val idParametra: Int,
    
    @ColumnInfo(name = "vrijednost_bool")
    val vrijednostBool: Boolean? = null,
    
    @ColumnInfo(name = "vrijednost_num")
    val vrijednostNum: Double? = null,
    
    @ColumnInfo(name = "vrijednost_txt")
    val vrijednostTxt: String? = null,
    
    @ColumnInfo(name = "napomena")
    val napomena: String? = null,
    
    @ColumnInfo(name = "vrijeme_unosa")
    val vrijemeUnosa: LocalDateTime? = null,
    
    @ColumnInfo(name = "azurirano")
    val azurirano: LocalDateTime = LocalDateTime.now()
)
```

### Data Class za Prikaz Checklistte

```kotlin
data class ChecklistUredaj(
    val idUred: Int,
    val natpPlocica: String,
    val tvBroj: String,
    val oznVrUred: String,
    val nazVrUred: String,
    val idPolje: Int?,
    val nazPolje: String,
    val napRazina: Double?,
    val parametri: List<ChecklistParametar>
)

data class ChecklistParametar(
    val idParametra: Int,
    val nazParametra: String,
    val tipPodataka: String,  // BOOLEAN, NUMERIC, TEXT
    val minVrijednost: Double?,
    val maxVrijednost: Double?,
    val mjernaJedinica: String?,
    val obavezan: Boolean,
    val redoslijed: Int,
    val defaultVrijednostBool: Boolean?,
    val defaultVrijednostNum: Double?,
    val defaultVrijednostTxt: String?,
    val zadnjaProveraDatum: String?,
    val opis: String
)
```

---

## Korak 3: Retrofit API Service

```kotlin
interface ElektropregledApiService {
    
    @POST("/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @GET("/v1/postrojenja")
    suspend fun getPostrojenja(
        @Header("Authorization") token: String
    ): List<PostrojenjaSummary>
    
    @GET("/v1/postrojenja/{id}/polja")
    suspend fun getPolja(
        @Path("id") postrojenjeId: Int,
        @Header("Authorization") token: String
    ): List<PoljeDto>
    
    @GET("/v1/postrojenja/{id}/checklist")
    suspend fun getChecklist(
        @Path("id") postrojenjeId: Int,
        @Query("id_polje") idPolje: Int,
        @Header("Authorization") token: String
    ): List<ChecklistUredaj>
    
    @POST("/v1/pregled/sync")
    suspend fun syncPregled(
        @Body request: SyncRequest,
        @Header("Authorization") token: String
    ): SyncResponse
}

// Request/Response DTOs
data class LoginRequest(
    val korisnicko_ime: String,
    val lozinka: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val username: String
)

data class SyncRequest(
    val pregled: PregledRequest,
    val stavke: List<StavkaRequest>
)

data class PregledRequest(
    val lokalni_id: String,
    val pocetak: String,  // ISO 8601
    val kraj: String?,
    val id_korisnika: Int,
    val id_postr: Int,
    val napomena: String?
)

data class StavkaRequest(
    val lokalni_id: String,
    val id_ured: Int,
    val id_parametra: Int,
    val vrijednost_bool: Boolean?,
    val vrijednost_num: Double?,
    val vrijednost_txt: String?,
    val napomena: String?,
    val vrijeme_unosa: String?
)

data class SyncResponse(
    val success: Boolean,
    val message: String,
    val serverPregledId: Long,
    val idMappings: IdMappings,
    val timestamp: String
)

data class IdMappings(
    val pregled: ServerIdMapping,
    val stavke: List<ServerIdMapping>
)

data class ServerIdMapping(
    val lokalniId: String,
    val serverId: Long
)
```

---

## Korak 4: Repository Pattern

```kotlin
class PregledRepository(
    private val pregledDao: PregledDao,
    private val stavkaDao: StavkaDao,
    private val apiService: ElektropregledApiService,
    private val tokenStorage: TokenStorage
) {
    
    // Sve preglede iz lokalne baze
    fun getAllPregledi(): Flow<List<PregledEntity>> {
        return pregledDao.getAllPregledi()
    }
    
    // Spremi novi pregled lokalno
    suspend fun savePregledLocally(
        pregled: PregledEntity,
        stavke: List<StavkaEntity>
    ) {
        val pregledId = pregledDao.insert(pregled)
        stavke.forEach { stavka ->
            stavkaDao.insert(stavka.copy(idPregled = pregledId))
        }
    }
    
    // Sinkronizira preglede sa serverom
    suspend fun syncPregledi() {
        val token = tokenStorage.getToken() ?: return
        
        val pendingPregledi = pregledDao.getPendingPregledi()
        
        for (pregled in pendingPregledi) {
            try {
                val stavke = stavkaDao.getStavkeByPregled(pregled.id)
                
                val syncRequest = SyncRequest(
                    pregled = PregledRequest(
                        lokalniId = pregled.lokalniId,
                        pocetak = pregled.pocetak.toString(),
                        kraj = pregled.kraj?.toString(),
                        idKorisnika = pregled.idKorisnika,
                        idPostr = pregled.idPostr,
                        napomena = pregled.napomena
                    ),
                    stavke = stavke.map { s ->
                        StavkaRequest(
                            lokalniId = s.lokalniId,
                            idUred = s.idUred,
                            idParametra = s.idParametra,
                            vrijednostBool = s.vrijednostBool,
                            vrijednostNum = s.vrijednostNum,
                            vrijednostTxt = s.vrijednostTxt,
                            napomena = s.napomena,
                            vrijemeUnosa = s.vrijemeUnosa?.toString()
                        )
                    }
                )
                
                val response = apiService.syncPregled(syncRequest, "Bearer $token")
                
                if (response.success) {
                    // Spremi server ID-eve
                    pregledDao.update(
                        pregled.copy(
                            serverId = response.serverPregledId,
                            syncStatus = "synced"
                        )
                    )
                    
                    response.idMappings.stavke.forEach { mapping ->
                        stavkaDao.updateServerId(mapping.lokalniId, mapping.serverId)
                    }
                } else {
                    pregledDao.update(
                        pregled.copy(syncStatus = "failed")
                    )
                }
                
            } catch (e: Exception) {
                pregledDao.update(
                    pregled.copy(syncStatus = "failed")
                )
            }
        }
    }
}
```

---

## Korak 5: ViewModel

```kotlin
class PregledViewModel(
    private val repository: PregledRepository,
    private val apiService: ElektropregledApiService,
    private val tokenStorage: TokenStorage
) : ViewModel() {
    
    private val _postrojenja = MutableLiveData<List<PostrojenjaSummary>>()
    val postrojenja: LiveData<List<PostrojenjaSummary>> = _postrojenja
    
    private val _polja = MutableLiveData<List<PoljeDto>>()
    val polja: LiveData<List<PoljeDto>> = _polja
    
    private val _checklist = MutableLiveData<List<ChecklistUredaj>>()
    val checklist: LiveData<List<ChecklistUredaj>> = _checklist
    
    private val _loadingState = MutableLiveData<Boolean>()
    val loadingState: LiveData<Boolean> = _loadingState
    
    private val _errorState = MutableLiveData<String>()
    val errorState: LiveData<String> = _errorState
    
    fun loadPostrojenja() {
        viewModelScope.launch {
            try {
                _loadingState.value = true
                val token = tokenStorage.getToken() ?: return@launch
                val data = apiService.getPostrojenja("Bearer $token")
                _postrojenja.value = data
                _errorState.value = ""
            } catch (e: Exception) {
                _errorState.value = e.message ?: "Greška pri učitavanju"
            } finally {
                _loadingState.value = false
            }
        }
    }
    
    fun loadPolja(postrojenjeId: Int) {
        viewModelScope.launch {
            try {
                _loadingState.value = true
                val token = tokenStorage.getToken() ?: return@launch
                val data = apiService.getPolja(postrojenjeId, "Bearer $token")
                _polja.value = data
                _errorState.value = ""
            } catch (e: Exception) {
                _errorState.value = e.message ?: "Greška pri učitavanju polja"
            } finally {
                _loadingState.value = false
            }
        }
    }
    
    fun loadChecklist(postrojenjeId: Int, poljeId: Int) {
        viewModelScope.launch {
            try {
                _loadingState.value = true
                val token = tokenStorage.getToken() ?: return@launch
                val data = apiService.getChecklist(
                    postrojenjeId,
                    poljeId,
                    "Bearer $token"
                )
                _checklist.value = data
                _errorState.value = ""
            } catch (e: Exception) {
                _errorState.value = e.message ?: "Greška pri učitavanju checklistte"
            } finally {
                _loadingState.value = false
            }
        }
    }
    
    fun syncPregledi() {
        viewModelScope.launch {
            try {
                _loadingState.value = true
                repository.syncPregledi()
                _errorState.value = ""
            } catch (e: Exception) {
                _errorState.value = "Sinkronizacija neuspješna: ${e.message}"
            } finally {
                _loadingState.value = false
            }
        }
    }
}
```

---

## Korak 6: Fragment za Checklist UI

```kotlin
class ChecklistFragment : Fragment() {
    
    private val viewModel: PregledViewModel by viewModels()
    private lateinit var checklistAdapter: ChecklistAdapter
    private var currentPregledId: String? = null
    private var currentUredajValues: MutableMap<String, Any?> = mutableMapOf()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val postrojenjeId = arguments?.getInt("postrojenjeId") ?: return
        val poljeId = arguments?.getInt("poljeId") ?: return
        
        setupRecyclerView()
        observeChecklist()
        
        viewModel.loadChecklist(postrojenjeId, poljeId)
    }
    
    private fun setupRecyclerView() {
        checklistAdapter = ChecklistAdapter { uređajId, parametrId, vrijednost ->
            // Spremi vrijednost u memoriju
            val key = "$uređajId-$parametrId"
            currentUredajValues[key] = vrijednost
        }
        
        binding.recyclerView.adapter = checklistAdapter
    }
    
    private fun observeChecklist() {
        viewModel.checklist.observe(viewLifecycleOwner) { checklist ->
            checklistAdapter.submitList(checklist)
        }
    }
    
    fun savePregled() {
        val postrojenjeId = arguments?.getInt("postrojenjeId") ?: return
        val korisnikId = getLoginData()?.idKorisnika ?: return
        
        val lokalniId = UUID.randomUUID().toString()
        val pregled = PregledEntity(
            lokalniId = lokalniId,
            idKorisnika = korisnikId,
            idPostr = postrojenjeId,
            pocetak = LocalDateTime.now()
        )
        
        // Konvertuj values u StavkeEntity
        val stavke = currentUredajValues.mapIndexed { index, (key, vrijednost) ->
            val (uředajId, parametrId) = key.split("-")
            
            StavkaEntity(
                lokalniId = UUID.randomUUID().toString(),
                idUred = uređajId.toInt(),
                idParametra = parametrId.toInt(),
                vrijednostBool = vrijednost as? Boolean,
                vrijednostNum = vrijednost as? Double,
                vrijednostTxt = vrijednost as? String
            )
        }
        
        viewModel.savePregledLocally(pregled, stavke)
    }
}
```

---

## Korak 7: Token Storage (Secure Storage)

```kotlin
interface TokenStorage {
    fun saveToken(token: String, expiresIn: Int)
    fun getToken(): String?
    fun isTokenValid(): Boolean
    fun clearToken()
}

class SharedPreferencesTokenStorage(context: Context) : TokenStorage {
    
    private val sharedPreferences = context.getSharedPreferences(
        "app_tokens",
        Context.MODE_PRIVATE
    )
    
    private val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        context,
        "app_secure",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    override fun saveToken(token: String, expiresIn: Int) {
        encryptedSharedPreferences.edit().apply {
            putString("access_token", token)
            putLong("token_created_at", System.currentTimeMillis())
            putInt("expires_in", expiresIn)
            apply()
        }
    }
    
    override fun getToken(): String? {
        return encryptedSharedPreferences.getString("access_token", null)
    }
    
    override fun isTokenValid(): Boolean {
        val token = getToken() ?: return false
        val createdAt = encryptedSharedPreferences.getLong("token_created_at", 0L)
        val expiresIn = encryptedSharedPreferences.getInt("expires_in", 0)
        
        val expirationTime = createdAt + (expiresIn * 1000L)
        return System.currentTimeMillis() < expirationTime
    }
    
    override fun clearToken() {
        encryptedSharedPreferences.edit().remove("access_token").apply()
    }
}
```

---

## Checklist za Implementaciju

- [ ] Setup Gradle dependencies
- [ ] Kreiranje Room database entities
- [ ] Kreiranje DAO interfacea
- [ ] Kreiranje Retrofit API servisa
- [ ] Implementacija repository patternaImplementacija ViewModela
- [ ] Kreiranje UI fragmenta sa RecyclerView
- [ ] Implementacija token storage-a
- [ ] Kreiranje sync managera
- [ ] Testiranje svih endpointa
- [ ] Error handling i logging
- [ ] Offline mode
- [ ] Progressive sync retrying

---

## Testing

### Unit Test za Repository

```kotlin
class PregledRepositoryTest {
    
    private val mockApiService = mockk<ElektropregledApiService>()
    private val mockPregledDao = mockk<PregledDao>(relaxed = true)
    private val mockStavkaDao = mockk<StavkaDao>(relaxed = true)
    private val mockTokenStorage = mockk<TokenStorage>()
    
    private lateinit var repository: PregledRepository
    
    @Before
    fun setup() {
        repository = PregledRepository(
            mockPregledDao,
            mockStavkaDao,
            mockApiService,
            mockTokenStorage
        )
    }
    
    @Test
    fun `test sync succeeds`() = runTest {
        val pregled = PregledEntity(
            lokalniId = "test-id",
            idKorisnika = 1,
            idPostr = 1001,
            pocetak = LocalDateTime.now()
        )
        
        coEvery { mockTokenStorage.getToken() } returns "test-token"
        coEvery { mockPregledDao.getPendingPregledi() } returns listOf(pregled)
        coEvery { mockStavkaDao.getStavkeByPregled(any()) } returns emptyList()
        
        coEvery { mockApiService.syncPregled(any(), any()) } returns SyncResponse(
            success = true,
            message = "OK",
            serverPregledId = 42,
            idMappings = IdMappings(
                pregled = ServerIdMapping("test-id", 42),
                stavke = emptyList()
            ),
            timestamp = LocalDateTime.now().toString()
        )
        
        repository.syncPregledi()
        
        coVerify {
            mockPregledDao.update(
                match { it.serverId == 42L && it.syncStatus == "synced" }
            )
        }
    }
}
```

---

## Performance Optimizacije

1. **Pagination:** Za velike liste, implementirati pagination
2. **Caching:** Spremi polja i postrojenja lokalno nakon prvog učitavanja
3. **Images:** Ako ima slika, koristi picasso/glide s disk cache
4. **Database Indexing:** Room automatski indeksira ForeignKey-eve
5. **Batch Operations:** Koristi transaction za više stavki

---

## Security Preporuke

1. **Token Storage:** Koristi EncryptedSharedPreferences (nitko direktno ne vidi token)
2. **HTTPS:** U produkciji, koristi HTTPS (ne HTTP)
3. **Certificate Pinning:** Zaštiti se od MITM napada
4. **Input Validation:** Validiraj sve podatke prije slanja na server
5. **ProGuard/R8:** Obfuskacija koda u release buildu
