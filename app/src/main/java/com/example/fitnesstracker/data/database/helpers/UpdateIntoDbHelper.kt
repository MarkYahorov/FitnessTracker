package com.example.fitnesstracker.data.database.helpers

import android.database.sqlite.SQLiteDatabase

class UpdateIntoDbHelper {

    companion object{
        private const val EMPTY_STRING = ""
        private const val SEPARATOR = ","
    }

    private var tableName = EMPTY_STRING
    private val updatesFields = mutableMapOf<String, Any>()
    private var whereArgs = EMPTY_STRING

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
        val updatingFields = updatesFields.entries.joinToString(SEPARATOR)
        db.compileStatement(
            "UPDATE $tableName SET $updatingFields WHERE $whereArgs"
        ).execute()

    }

    fun delete(db: SQLiteDatabase) {
        if (whereArgs == EMPTY_STRING) {
            db.execSQL("DELETE FROM $tableName")
        } else {
            db.execSQL("DELETE FROM $tableName WHERE $whereArgs")
        }
    }
}