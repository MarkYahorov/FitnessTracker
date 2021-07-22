package com.example.fitnesstracker.data.database.helpers

import android.database.sqlite.SQLiteDatabase

class InsertDBHelper {

    private var tableName: String = ""
    private val selectedFieldsInTable = mutableMapOf<String, String>()

    fun setTableName(name: String): InsertDBHelper {
        this.tableName = name
        return this
    }

    fun addFieldsAndValuesToInsert(nameOfField: String, insertingValue: String): InsertDBHelper {
        selectedFieldsInTable[nameOfField] = insertingValue
        return this
    }

    fun insertTheValues(db: SQLiteDatabase) {
        val selectedFields = selectedFieldsInTable.keys.joinToString()
        val questionList = mutableListOf<String>()
        val size = selectedFieldsInTable.size
        while (questionList.size!= size){
            questionList.add("?")
        }
        val stringBuilderForQuestion = questionList.joinToString()
        if (tableName == "" || selectedFieldsInTable.isEmpty()){
            error("Введи нормальные данные")
        } else {
            val statement =
                db.compileStatement("INSERT INTO $tableName ($selectedFields) VALUES ($stringBuilderForQuestion)")
            selectedFieldsInTable.values.forEachIndexed { index, s ->
                statement.bindString(index + 1, s)
            }
            statement.execute()
        }
    }
}