package com.example.core.database.helpers

import android.database.sqlite.SQLiteDatabase

class InsertIntoDBHelper {

    companion object{
        private const val ERROR = "Введи нормальные данные"
        private const val QUESTION = "?"
        private const val EMPTY_STRING = ""
        private const val INDEX_PLUS = 1
    }

    private var tableName: String = EMPTY_STRING
    private val selectedFieldsInTable = mutableMapOf<String, String>()

    fun setTableName(name: String): InsertIntoDBHelper {
        this.tableName = name
        return this
    }

    fun addFieldsAndValuesToInsert(nameOfField: String, insertingValue: String?): InsertIntoDBHelper {
        insertingValue?.let { selectedFieldsInTable.put(nameOfField, it) }
        return this
    }

    fun insertTheValues(db: SQLiteDatabase) {
        val selectedFields = selectedFieldsInTable.keys.joinToString()
        val questionList = mutableListOf<String>()
        val size = selectedFieldsInTable.size
        while (questionList.size != size) {
            questionList.add(QUESTION)
        }
        val stringBuilderForQuestion = questionList.joinToString()
        if (tableName == EMPTY_STRING || selectedFieldsInTable.isEmpty()) {
            error(ERROR)
        } else {
            val statement =
                db.compileStatement("INSERT OR IGNORE INTO $tableName ($selectedFields) VALUES ($stringBuilderForQuestion)")
            selectedFieldsInTable.values.forEachIndexed { index, s ->
                statement.bindString(index + INDEX_PLUS, s)
            }
            statement.execute()
        }
    }
}