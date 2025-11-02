package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    //In order to make the required notification, a service is required
    //to do the job for us in the foreground process
    //Create the notification builder that'll be called Later on
    private lateinit var notificationBuilder: NotificationCompat.Builder
    //Create a system handler which controls what thread the process is being
    // executed on
    private lateinit var serviceHandler: Handler

    //This is used to bind a two-way communication
    //In this tutorial, we will only be using a one-way communication
    //therefore, the return can be set to null
    override fun onBind(intent: Intent): IBinder? = null

    //this is a callback and part of the Life cycle
    //the onCreate callback will be called when this service
    //is created for the first time
    override fun onCreate() {
        super.onCreate()
        //Create the notification with all of its contents and configurations
        //in the startForegroundService() custom function
        notificationBuilder = startForegroundService()
        //Create the handler to control which thread the
        //notification will be executed on.
        //'Handler Thread' provides the different thread for the process to be
        // executed on,
        //while on the other hand, 'Handler' enqueues the process to Handler Thread
        // to be executed.
        //Here, we're instantiating a new Handler Thread called "ThirdThread" (Ganti nama thread)
        val handlerThread = HandlerThread("ThirdThread")
            .apply { start() }
        //then we pass that Handler Thread into the main Handler called
        // serviceHandler
        serviceHandler = Handler(handlerThread.looper)
    }

    //Create the notification with all of its contents and configurations all set up
    private fun startForegroundService(): NotificationCompat.Builder {
        //Create a pending Intent which is used to be executed
        //when the user clicks the notification
        val pendingIntent = getPendingIntent()
        //To make a notification, you should know the keyword 'channel'
        val channelId = createNotificationChannel() // Panggil channel yang dimodifikasi
        //Combine both the pending Intent and the channel
        //into a notification builder
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )
        //After all has been set and the notification builder is ready,
        //start the foreground service and the notification
        //will appear on the user's device
        startForeground(NOTIFICATION_ID, notificationBuilder.build()) // Gunakan NOTIFICATION_ID unik
        return notificationBuilder
    }

    //A pending Intent is the Intent used to be executed
    //when the user clicks the notification
    private fun getPendingIntent(): PendingIntent {
        //In order to create a pending Intent, a Flag is needed
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0
        //Here, we're setting MainActivity into the pending Intent
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    //To make a notification, a channel is required to
    //set up the required configurations
    private fun createNotificationChannel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create the channel id
            val channelId = "002" // ID CHANNEL DIUBAH
            //Create the channel name
            val channelName = "002 Channel" // NAMA CHANNEL DIUBAH
            //Create the channel priority
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT
            //Build the channel notification based on all 3 previous attributes
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )
            //Get the NotificationManager class
            val service = requireNotNull(
                ContextCompat.getSystemService(
                    this,
                    NotificationManager::class.java
                )
            )
            //Binds the channel into the NotificationManager
            service.createNotificationChannel(channel)
            //Return the channel id
            channelId
        } else {
            ""
        }

    //Build the notification with all of its contents and configurations
    private fun getNotificationBuilder(
        pendingIntent: PendingIntent, channelId:
        String
    ) =
        NotificationCompat.Builder(this, channelId)
            //Sets the title
            .setContentTitle("Third worker process is done") // JUDUL DIUBAH
            //Sets the content
            .setContentText("Check it out!")
            //Sets the notification icon
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            //Sets the action/intent to be executed when the user clicks the
            //notification
            .setContentIntent(pendingIntent)
            //Sets the ticker message (brief message on top of your device)
            .setTicker("Third worker process is done, check it out!") // TICKER DIUBAH
            //setOnGoing() controls whether the notification is dismissible or not
            .setOngoing(true)

    //This is a callback and part of a Life cycle
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        //Gets the channel id passed from the MainActivity through the Intent
        val Id = intent?.getStringExtra(EXTRA_ID) // Gunakan EXTRA_ID unik
            ?: throw IllegalStateException("Channel ID must be provided")

        //Posts the notification task to the handler,
        serviceHandler.post {
            //Modul menyarankan ganti timer, misal jadi 5 detik
            countDownFromFiveToZero(notificationBuilder)
            //Here we're notifying the MainActivity that the service process is
            //done
            notifyCompletion(Id)
            //Stops the foreground service, which closes the notification
            stopForeground(STOP_FOREGROUND_REMOVE)
            //Stop and destroy the service
            stopSelf()
        }
        return returnValue
    }

    //A function to update the notification to display a count down from 5 to 0
    private fun countDownFromFiveToZero( // Ganti nama fungsi
        notificationBuilder: NotificationCompat.Builder
    ) {
        //Gets the notification manager
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        //Count down from 5 to 0
        for (i in 5 downTo 0) { // Ganti timer
            Thread.sleep(1000L)
            //Updates the notification content text
            notificationBuilder
                .setContentText("$i seconds until final warning") // Teks diubah
                .setSilent(true)
            //Notify the notification manager about the content update
            notificationManager.notify(
                NOTIFICATION_ID, // ID unik
                notificationBuilder.build()
            )
        }
    }

    //Update the LiveData with the returned channel id through the Main Thread
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA8 // NOTIFICATION ID DIUBAH
        const val EXTRA_ID = "Id2" // EXTRA ID DIUBAH

        //this is a LiveData which is a data holder that automatically
        //updates the UI based on what is observed
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}