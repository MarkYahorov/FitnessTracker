package com.example.fitnesstracker.data.database.helpers

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.fitnesstracker.App
import com.example.fitnesstracker.data.database.FitnessDatabase

class UpdateDbHelper {
    private var tableName = ""
    private val updatesFields = mutableMapOf<String, Any>()
    private var whereArgs = ""

    fun setName(tableName:String): UpdateDbHelper{
        this.tableName = tableName
        return UpdateDbHelper()
    }

    fun updatesValues(nameOfField:String, updateValue:Int): UpdateDbHelper{
        this.updatesFields[nameOfField] = updateValue
        return this
    }

    fun updatesValues(nameOfField:String, updateValue:Long):UpdateDbHelper{
        this.updatesFields[nameOfField] = updateValue
        return this
    }

    fun where(whereArgs:String): UpdateDbHelper{
        this.whereArgs = whereArgs
        return this
    }

    fun update(db: SQLiteDatabase){
        val stri = updatesFields.entries.joinToString(",")
        db.compileStatement(
            "UPDATE $tableName SET $stri WHERE $whereArgs")

    }

    fun delete(db: SQLiteDatabase){
        if (whereArgs == "") {
            db.execSQL("DELETE FROM $tableName")
        } else {
            db.execSQL("DELETE FROM $tableName WHERE $whereArgs")
        }
    }
}