package com.banyg.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Category Room entity
 *
 * Represents a transaction category for budgeting and reporting.
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["group_id"]),
        Index(value = ["is_hidden"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "group_id")
    val groupId: String? = null,

    @ColumnInfo(name = "group_name")
    val groupName: String? = null,

    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,

    @ColumnInfo(name = "icon")
    val icon: String? = null,

    @ColumnInfo(name = "color")
    val color: String? = null
)
