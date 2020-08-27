package adam.illhaveacompany.scannerandsavingpointstosql
//I want to log the barcode that comes up
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanBtn.setOnClickListener{
            scanCode()
        }//6

        if(areTherePointsInTheDatabase()){
            pointsNumberTextView.text = getPointsValueFromDb().toString()
        } //20

    }

    private fun scanCode() {
        val integrator = IntentIntegrator(this)
        integrator.captureActivity = CaptureAct::class.java
        integrator.setOrientationLocked(false)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES) //can change to QR later
        integrator.setPrompt("Scanning Code")
        integrator.initiateScan()
    } //5

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                if(result.contents == "TESTCODE") {
                    if(! areTherePointsInTheDatabase()) {
                        addFirstPointsToSQL(80)
                        pointsNumberTextView.text = getPointsValueFromDb().toString()
                    }else{
                        addFirstPointsToSQL(100)
                        pointsNumberTextView.text = getPointsValueFromDb().toString()
                        Toast.makeText(this@MainActivity, "There is already data in the database", Toast.LENGTH_SHORT).show()
                    }//21

                    if(isThereMoreThanOneSetOfPoints()){
                        val databaseHandler = DatabaseHandler(this)
                        databaseHandler.deleteFirstRow()
                        databaseHandler.close()
                    }
                }else {
                    Toast.makeText(this, "Barcode Not Recognized", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "No Results", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    } //7

    private fun addFirstPointsToSQL(points: Int) {
        val databaseHandler = DatabaseHandler(this)
        val status = databaseHandler.addPoints(Points(0, points))

        if(status > -1) {
            Toast.makeText(applicationContext, "Points Successfully Added", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(applicationContext,"Record save failed", Toast.LENGTH_LONG).show()
        }
    }//13

    private fun isThereMoreThanOneSetOfPoints(): Boolean {
        //returns whether there's two sets of points - found from DatabaseHandler's function
        val databaseHandler = DatabaseHandler(this)
        val twoSetsOfPoints = databaseHandler.areThereMoreThanOneSetOfPoints()
        databaseHandler.close()
        return twoSetsOfPoints
    }//17

    private fun getPointsValueFromDb() : Int {
        val databaseHandler = DatabaseHandler(this)
        val pointsValueList = databaseHandler.getPointsValues()
        val lastPointsValueRow = pointsValueList[pointsValueList.size - 1]
        val lastPointsValue = lastPointsValueRow.numberOfPoints

        return lastPointsValue
    }//18

    private fun areTherePointsInTheDatabase() : Boolean {
        val dbhandler = DatabaseHandler(this)
        val areTherePoints = dbhandler.areTherePoints()
        dbhandler.close()
        return areTherePoints
    }//19

}