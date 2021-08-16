package com.example.fitnesstracker.data.database.helpers

import android.database.sqlite.SQLiteDatabase

class CreateTableHelper {
    private var name: String = ""
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
        if (name == "" || fields.isEmpty()) {
            error("Заполнить имя или поля")
        } else {
            db?.execSQL("CREATE TABLE $name ($stringBuilder)")
        }
    }
}