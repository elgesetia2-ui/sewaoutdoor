package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Transaction
    @Query("SELECT * FROM products ORDER BY nama ASC")
    fun observeProductsWithVariants(): Flow<List<ProductWithVariants>>

    @Transaction
    @Query("SELECT * FROM products ORDER BY nama ASC")
    suspend fun getProductsWithVariantsOnce(): List<ProductWithVariants>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariants(variants: List<ProductVariantEntity>)

    @Query("DELETE FROM product_variants WHERE productId = :productId")
    suspend fun deleteVariantsForProduct(productId: String)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: String)

    @Query("UPDATE product_variants SET stok = MAX(0, stok + :delta) WHERE id = :variantId")
    suspend fun adjustVariantStock(variantId: String, delta: Int)

    @Query("DELETE FROM product_variants")
    suspend fun clearAllVariants()

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()
}

@Dao
interface PackageDao {
    @Query("SELECT * FROM packages ORDER BY nama ASC")
    fun observePackages(): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages ORDER BY nama ASC")
    suspend fun getPackagesOnce(): List<PackageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackages(packages: List<PackageEntity>)

    @Query("DELETE FROM packages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM packages")
    suspend fun clearAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAtMillis DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY createdAtMillis DESC")
    suspend fun getTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE tanggalTransaksi = :dateIso")
    suspend fun countByDate(dateIso: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY totalBelanja DESC, nama ASC")
    fun observeMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members ORDER BY nama ASC")
    suspend fun getMembersOnce(): List<MemberEntity>

    @Query("SELECT * FROM members WHERE noHp = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM members")
    suspend fun clearAll()
}

@Dao
interface CashEntryDao {
    @Query("SELECT * FROM cash_entries ORDER BY createdAtMillis DESC")
    fun observeCashEntries(): Flow<List<CashEntryEntity>>

    @Query("SELECT * FROM cash_entries ORDER BY createdAtMillis DESC")
    suspend fun getCashEntriesOnce(): List<CashEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CashEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<CashEntryEntity>)

    @Query("DELETE FROM cash_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cash_entries")
    suspend fun clearAll()
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY role ASC, nama ASC")
    fun observeEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees")
    suspend fun getEmployeesOnce(): List<EmployeeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<EmployeeEntity>)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM employees")
    suspend fun clearAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SettingsEntity)
}
