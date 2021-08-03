package com.example.fitnesstracker.data.database.helpers

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class SelectDbHelper {

    private var table: String = ""
    private var whereArgs: String = ""
    private var allParams: MutableList<String> = mutableListOf()

    fun nameOfTable(table: String): SelectDbHelper {
        this.table = table
        return this
    }

    fun where(whereArgs: String): SelectDbHelper {
        this.whereArgs = whereArgs
        return this
    }

    fun selectParams(allParams: String): SelectDbHelper {
        this.allParams.add(allParams)
        return this
    }

    fun select(db: SQLiteDatabase): Cursor {
        val allParamsText = allParams.joinToString(",")
        return if (whereArgs == "") {
            db.rawQuery("SELECT $allParamsText FROM $table", null)
        } else {
            db.rawQuery("SELECT $allParamsText FROM $table WHERE $whereArgs", null)
        }
    }
}