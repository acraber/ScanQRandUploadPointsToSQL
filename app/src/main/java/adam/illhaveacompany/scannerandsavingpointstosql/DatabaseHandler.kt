package adam.illhaveacompany.scannerandsavingpointstosql

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Picture

class DatabaseHandler (context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
    companion object {
        //changed when I want to add a column
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "PointsDatabase"

        private const val TABLE_POINTS = "PointsTable"

        private const val KEY_ID = "_id"
        private const val KEY_NUMBER_OF_POINTS = "numberOfPoints"
    }//8

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_LIBRARY_TABLE = ("CREATE TABLE " + TABLE_POINTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NUMBER_OF_POINTS + " TEXT" + ")")
        db?.execSQL(CREATE_LIBRARY_TABLE)
    }//10

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_POINTS")
        onCreate(db)
    }//11

    fun addPoints(points: Points) : Long {
        val db = this.writableDatabase

        val contentValues = ContentValues()

        contentValues.put(KEY_NUMBER_OF_POINTS, points.numberOfPoints)

        val success = db.insert(TABLE_POINTS, null, contentValues)
        db.close()

        return success
    }//12

    fun areTherePoints() : Boolean {
        val database = this.readableDatabase
        val noOfRows = DatabaseUtils.queryNumEntries(database, TABLE_POINTS).toInt()

        return if (noOfRows == 0) {
            false
        } else return true
    }//14

    fun areThereMoreThanOneSetOfPoints() : Boolean {
        val database = this.readableDatabase
        val noOfRows = DatabaseUtils.queryNumEntries(database, TABLE_POINTS).toInt()

        return if (noOfRows < 2) {
            false
        } else return true
    }//15

    fun deleteFirstRow() {
        val db = this.readableDatabase

        val cursor: Cursor =
            db.query(TABLE_POINTS, null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val rowId = cursor.getString(cursor.getColumnIndex(KEY_ID))
            db.delete(TABLE_POINTS, "$KEY_ID=?", arrayOf(rowId))
        }
        db.close()
    }//16

    fun getPointsValues() : ArrayList<Points> {
        val pointsList: ArrayList<Points> = ArrayList<Points>()
        val selectQuery = "SELECT * FROM $TABLE_POINTS"

        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery, null)
        }catch (e: SQLiteException){
            db.execSQL(selectQuery)
        }//5

        var id : Int
        var numberOfPoints: Int

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                do{
                    id = cursor.getInt(cursor.getColumnIndex(KEY_ID))
                    numberOfPoints = cursor.getInt(cursor.getColumnIndex(KEY_NUMBER_OF_POINTS))

                    val pointsRow = Points(id, numberOfPoints)

                    pointsList.add(pointsRow)
                } while(cursor.moveToNext())
            }
        }

        return pointsList

    }//18
}