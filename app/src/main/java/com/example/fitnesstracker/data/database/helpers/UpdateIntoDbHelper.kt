package com.example.fitnesstracker.data.database.helpers

import android.database.sqlite.SQLiteDatabase

class UpdateIntoDbHelper {
    private var tableName = ""
    private val updatesFields = mutableMapOf<String, Any>()
    private var whereArgs = ""

    fun setName(tableName: String): UpdateIntoDbHelper {
        this.tableName = tableName
        return this
    }

    fun updatesValues(nameOfField: String, updateValue: Int): UpdateIntoDbHelper {
        this.updatesFields[nameOfField] = updateValue
        return this
    }

    fun updatesValues(nameOfField: String, updateValue: Long): UpdateIntoDbHelper {
        this.updatesFields[nameOfField] = updateValue
        return this
    }

    fun where(whereArgs: String): UpdateIntoDbHelper {
        this.whereArgs = whereArgs
        return this
    }

    fun update(db: SQLiteDatabase) {
        val updatingFields = updatesFields.entries.joinToString(",")
        db.compileStatement(
            "UPDATE $tableName SET $updatingFields WHERE $whereArgs"
        ).execute()

    }

    fun delete(db: SQLiteDatabase) {
        if (whereArgs == "") {
            db.execSQL("DELETE FROM $tableName")
        } else {
            db.execSQL("DELETE FROM $tableName WHERE $whereArgs")
        }
    }
}