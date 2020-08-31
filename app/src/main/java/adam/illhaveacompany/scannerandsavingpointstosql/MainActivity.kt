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


        setProgressBarAndPointsNumber(getPointsValueFromDb())

        redeemPointsBtn.setOnClickListener {
            redeemPoints()
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

                    addPointsToDb(pointsToAdd)//24
                    setProgressBarAndPointsNumber(getPointsValueFromDb())
                    pointsToAdd = 0

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


    private fun isThereMoreThanOneSetOfPoints(): Boolean {
        //returns whether there's two sets of points - found from DatabaseHandler's function
        val databaseHandler = DatabaseHandler(this)
        val twoSetsOfPoints = databaseHandler.areThereMoreThanOneSetOfPoints()
        databaseHandler.close()
        return twoSetsOfPoints
    }//17

    private fun getPointsValueFromDb() : Int {
        var lastPointsValue = 0
        if(areTherePointsInTheDatabase()){
        val databaseHandler = DatabaseHandler(this)
        val pointsValueList = databaseHandler.getPointsValues()
        val lastPointsValueRow = pointsValueList[pointsValueList.size - 1]
        lastPointsValue = lastPointsValueRow.numberOfPoints
        }else{
            lastPointsValue = 0
        }
        return lastPointsValue
    }//18

    private fun areTherePointsInTheDatabase() : Boolean {
        val dbHandler = DatabaseHandler(this)
        val areTherePoints = dbHandler.areTherePoints()
        dbHandler.close()
        return areTherePoints
    }//19

    private fun addPointsToDb(points: Int) {
        if(areTherePointsInTheDatabase()) {
            val databaseHandler = DatabaseHandler(this)
            val status = databaseHandler.addSecondaryPoints(points)

            if (status > -1) {
                Toast.makeText(applicationContext, "Points Successfully Added", Toast.LENGTH_LONG)
                    .show()
            } else {
                Toast.makeText(applicationContext, "Record save failed", Toast.LENGTH_LONG).show()
            }
            databaseHandler.close()
        }else{
            val databaseHandler = DatabaseHandler(this)
            val status = databaseHandler.addFirstPoints(Points(0, points))

            if(status > -1) {
                Toast.makeText(applicationContext, "Points Successfully Added", Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(applicationContext, "Record save failed", Toast.LENGTH_LONG).show()
            }
            databaseHandler.close()
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

        totalPointsAfterAdding = pointsToAdd + getPointsValueFromDb()
        if(totalPointsAfterAdding >= 50){
            builder.setMessage("A Los Amigos employee must verify points before scanning.\n\nThe maximum total points allowed is 50")
            totalPointsAfterAdding = 0
            builder.show()
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

    private fun redeemPoints(){
        if(getPointsValueFromDb() >= 50){
            val builder2 = AlertDialog.Builder(this)
            builder2.setTitle("Redeeming Points")
            builder2.setMessage("Are you sure you would like to redeem your points?\n\nThis can only be done once\n\nIf you do this " +
                    "away from a Los Amigos employee the points become voided")
            builder2.setPositiveButton("YES") { dialogInterface: DialogInterface, i: Int ->
                val builder4 = AlertDialog.Builder(this)
                builder4.setTitle("SHOW TO EMPLOYEE")
                builder4.setMessage("The user has chosen to redeem their points\n\nShow this message to Los Amigos employee or points may be voided")
                builder4.setPositiveButton("Okay") { dialogInterface: DialogInterface, i: Int ->
                    Toast.makeText(this, "Now Remove Points", Toast.LENGTH_SHORT).show()
                }

                builder4.show()
            }
            builder2.setNegativeButton("NO") { dialogInterface: DialogInterface, i: Int ->
                val builder3 = AlertDialog.Builder(this)
                builder3.setTitle("NOT REDEEMING POINTS")
                builder3.setMessage("User cancelled points redemption")
                builder3.setPositiveButton("Okay") { dialogInterface: DialogInterface, i: Int ->
                }
                builder3.show()
            }
               builder2.show()
        }else{
            Toast.makeText(this, "There are not enough points to redeem", Toast.LENGTH_SHORT).show()
        }

    }

}