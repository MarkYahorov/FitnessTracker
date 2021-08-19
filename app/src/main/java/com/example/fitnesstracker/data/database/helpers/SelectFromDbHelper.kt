package com.example.fitnesstracker.data.database.helpers

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class SelectFromDbHelper {

    companion object{
        private const val SEPARATOR = ","
        private const val EMPTY_STRING = ""
    }

    private var table: String = EMPTY_STRING
    private var whereArgs: String = EMPTY_STRING
    private var allParams: MutableList<String> = mutableListOf()

    fun nameOfTable(table: String): SelectFromDbHelper {
        this.table = table
        return this
    }

    fun where(whereArgs: String): SelectFromDbHelper {
        this.whereArgs = whereArgs
        return this
    }

    fun selectParams(allParams: String): SelectFromDbHelper {
        this.allParams.add(allParams)
        return this
    }

    fun select(db: SQLiteDatabase): Cursor {
        val allParamsText = allParams.joinToString(SEPARATOR)
        return if (whereArgs == EMPTY_STRING) {
            db.rawQuery("SELECT $allParamsText FROM $table", null)
        } else {
            db.rawQuery("SELECT $allParamsText FROM $table WHERE $whereArgs", null)
        }
    }
}