package com.teqmed.teqruta.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teqmed.teqruta.domain.model.Rol

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["email"], unique = true)]
)
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val email: String,
    val passwordHash: String,
    val rol: Rol,
    /** Jefe a cargo; solo aplica a los técnicos. */
    val jefeId: Long? = null,
    val activo: Boolean = true
)
