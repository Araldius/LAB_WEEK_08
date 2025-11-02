package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    //Create an instance of a work manager
    //Work manager manages all your requests and workers
    //it also sets up the sequence for all your processes
    private val workManager by lazy {
        WorkManager.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // Request izin notifikasi (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        //Create a constraint of which your workers are bound to.
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        //This request is created for the FirstWorker class
        val firstRequest = OneTimeWorkRequest
            .Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(
                getIdInputData(
                    FirstWorker.INPUT_DATA_ID, id
                )
            )
            .build()

        //This request is created for the SecondWorker class
        val secondRequest = OneTimeWorkRequest
            .Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(
                getIdInputData(
                    SecondWorker.INPUT_DATA_ID, id
                )
            )
            .build()

        // TAMBAHAN: Request untuk ThirdWorker
        val thirdRequest = OneTimeWorkRequest
            .Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(
                getIdInputData(
                    ThirdWorker.INPUT_DATA_ID, id
                )
            )
            .build()

        //Sets up the process sequence from the work manager instance
        //Here it starts with FirstWorker, then SecondWorker, then ThirdWorker
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest) // TAMBAHKAN thirdRequest ke rantai
            .enqueue()

        //Here we're observing the returned LiveData and getting the
        //state result of the worker
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info?.state?.isFinished == true) {
                    showResult("First process is done")
                }
            }

        // MODIFIKASI: Panggil service saat worker 2 selesai
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info?.state?.isFinished == true) {
                    showResult("Second process is done")
                    launchNotificationService() // Panggil service 1
                }
            }

        // TAMBAHAN: Observer untuk ThirdWorker
        // Panggil service 2 saat worker 3 selesai
        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info?.state?.isFinished == true) {
                    showResult("Third process is done")
                    launchSecondNotificationService() // Panggil service 2
                }
            }
    }

    //Build the data into the correct format before passing it to the worker as
    //input
    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    //Show the result as toast
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //Launch the NotificationService
    private fun launchNotificationService() {
        //Observe if the service process is done or not
        NotificationService.trackingCompletion.observe(
            this
        ) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }
        //Create an Intent to start the NotificationService
        val serviceIntent = Intent(
            this,
            NotificationService::class.java
        ).apply {
            putExtra(EXTRA_ID, "001")
        }
        //Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // TAMBAHAN: Fungsi untuk meluncurkan service kedua
    private fun launchSecondNotificationService() {
        //Observe if the service process is done or not
        SecondNotificationService.trackingCompletion.observe(
            this
        ) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }
        //Create an Intent to start the SecondNotificationService
        val serviceIntent = Intent(
            this,
            SecondNotificationService::class.java // Ganti ke service 2
        ).apply {
            putExtra(EXTRA_ID_2, "002") // Ganti ID channel & key
        }
        //Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
    }


    companion object {
        const val EXTRA_ID = "Id"
        const val EXTRA_ID_2 = "Id2" // TAMBAHAN: Key untuk service 2
    }
}