package adam.illhaveacompany.scannerandsavingpointstosql
//I want to log the barcode that comes up
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var pointsToAdd : Int = 0
    var doneWithShowingSpinner = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanBtn.setOnClickListener{
            doneWithShowingSpinner = false
            show()
        }//27

        if(areTherePointsInTheDatabase()){
            setProgressBarAndPointsNumber(getPointsValueFromDb())
        }else{
            setProgressBarAndPointsNumber(0)
        }//29

        redeemPointsBtn.setOnClickListener {
            if(areTherePointsInTheDatabase()){
                if(getPointsValueFromDb() >= 50){
                    Toast.makeText(this, "There are enough points to redeem", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this, "There are not enough points to redeem", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this, "There are not enough points to redeem", Toast.LENGTH_SHORT).show()
            }
        }

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
                        addFirstPointsToSQL(pointsToAdd)//27
                        setProgressBarAndPointsNumber(getPointsValueFromDb())
                        pointsToAdd = 0
                    }else{
                        addSecondaryPointsToDb(pointsToAdd)//24
                        setProgressBarAndPointsNumber(getPointsValueFromDb())
                        pointsToAdd = 0
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
        val status = databaseHandler.addFirstPoints(Points(0, points))

        if(status > -1) {
            Toast.makeText(applicationContext, "Points Successfully Added", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(applicationContext, "Record save failed", Toast.LENGTH_LONG).show()
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
        val dbHandler = DatabaseHandler(this)
        val areTherePoints = dbHandler.areTherePoints()
        dbHandler.close()
        return areTherePoints
    }//19

    private fun addSecondaryPointsToDb(points: Int) {
        val databaseHandler = DatabaseHandler(this)
        val status = databaseHandler.addSecondaryPoints(points)

        if(status > -1) {
            Toast.makeText(applicationContext, "Points Successfully Added", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(applicationContext, "Record save failed", Toast.LENGTH_LONG).show()
        }
    }//23

    private fun show() {
    val d = Dialog(this)
    d.setTitle("NumberPicker")
    d.setContentView(R.layout.dialog)
    val b1: Button = d.findViewById(R.id.setButton) as Button
    val b2: Button = d.findViewById(R.id.cancelButton) as Button
    val numberPicker = d.findViewById(R.id.numberPicker1) as NumberPicker
    numberPicker.maxValue = 20
    numberPicker.minValue = 1
    numberPicker.wrapSelectorWheel = false

    b1.setOnClickListener{
        var totalPointsAfterAdding = 0
        pointsToAdd = numberPicker.value
        d.dismiss()
        doneWithShowingSpinner = true

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Adding ${pointsToAdd} points")
        builder.setPositiveButton("SCAN") { dialogInterface: DialogInterface, i: Int ->
            scanCode()
        }
        builder.setNegativeButton("GO BACK") { dialogInterface: DialogInterface, i: Int ->
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
        if(areTherePointsInTheDatabase()){
            var totalPointsAfterAdding = pointsToAdd + getPointsValueFromDb()
            if(totalPointsAfterAdding >= 50){
                builder.setMessage("A Los Amigos employee must verify points before scanning.\n\nThe maximum total points allowed is 50")
                totalPointsAfterAdding = 0
                builder.show()
            }else{
                builder.setMessage("A Los Amigos employee must verify points before scanning.")
                totalPointsAfterAdding = 0
                builder.show()
            }
        }else{
            builder.setMessage("A Los Amigos employee must verify points before scanning.")
            totalPointsAfterAdding = 0
            builder.show()
        }
    }//31 and also //6 earlier

    b2.setOnClickListener {
        d.dismiss()
    }
    d.show()
}//26

    private fun setProgressBarAndPointsNumber(numberOfPoints: Int) {
        progressBar.max = 500

        if(numberOfPoints == 0)
        {
            pointsNumberTextView.text = '0'.toString()
            ObjectAnimator.ofInt(progressBar, "progress", 0).setDuration(2000).start()
        }else{
            pointsNumberTextView.text = numberOfPoints.toString()
            ObjectAnimator.ofInt(progressBar, "progress", numberOfPoints*10).setDuration(2000).start()
        }
    }

}