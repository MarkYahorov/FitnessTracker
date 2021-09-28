package com.example.core.database.helpers

import android.database.sqlite.SQLiteDatabase

class CreateTableHelper {

    companion object{
        private const val ERROR = "Введи нормальные данные"
        private const val EMPTY_STRING = ""
    }

    private var name: String = EMPTY_STRING
    private var fields: MutableMap<String, String> = mutableMapOf()

    fun setName(table: String): CreateTableHelper {
        this.name = table
        return this
    }

    fun addField(title: String, condition: String): CreateTableHelper {
        this.fields[title] = condition
        return this
    }

    fun create(db: SQLiteDatabase?) {
        val stringBuilder = fields.entries.joinToString {
            "${it.key} ${it.value}"
        }
        if (name == EMPTY_STRING || fields.isEmpty()) {
            error(ERROR)
        } else {
            db?.execSQL("CREATE TABLE $name ($stringBuilder)")
        }
    }
}