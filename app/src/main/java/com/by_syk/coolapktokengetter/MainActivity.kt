/*
 * Copyright 2017 By_syk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.by_syk.coolapktokengetter

import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import com.coolapk.market.util.AuthUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import android.widget.SeekBar
import android.os.Environment
import android.text.TextUtils
import org.w3c.dom.Text
import java.io.FileOutputStream
import java.sql.Time
import java.text.SimpleDateFormat

/**
 * Created by By_syk on 2016-07-16.
 */

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                           fromUser: Boolean) {
                daysTextView.text = (progress+1).toString()+"d"
            }
        })
    }

    fun onStartClick(view: View) {
        TokenTask().execute()
    }

    inner class TokenTask: AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.progress = 0
            progressTextView.text = "0%"
        }

        override fun doInBackground(vararg p0: String?): String {
            generateToken()
            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

        }

    }

    fun generateToken(){

        val randomUUID:Boolean = randomCheckBox.isChecked
        var uuid:String = ""
        if (!randomUUID) {
            uuid = uuidEditText.text.toString()
            if (uuid.isNullOrBlank()){
                uuid = UUID.randomUUID().toString()
            }
        }
        var targetCalendar = Calendar.getInstance()
        var startTimeText = startTimeEditText.text.toString()
        if (!startTimeText.isNullOrBlank()) {
            //we have a custom start time
            try {
                val startTime = SimpleDateFormat("yyyyMMddHHmmss").parse(startTimeText)
                //success
                targetCalendar.time = startTime
            }catch (e: Exception) {
                //failed
                Log.e("error", "parse start time failed")
                runOnUiThread {
                    Toast.makeText(this, "parse start time failed", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }

        val startTime = targetCalendar.time
        var startTimeS:Long = targetCalendar.timeInMillis/1000
        var targetTimeS:Long = startTimeS - 290

        var totalS:Long = 24L*60L*60L*(seekBar.progress+1).toLong()
        var setTimeFailedTime = 0
        try {
            var tokenArray:MutableList<Token> = mutableListOf()
            var tmpToken:Token?
            var progress = 0
            while (targetTimeS - startTimeS < totalS) {
                targetTimeS += 290 //290 seconds
                targetCalendar.timeInMillis = targetTimeS*1000
                setSystemTimeWithCalendar(targetCalendar)

                if (randomUUID) {
                    uuid = UUID.randomUUID().toString()
                }
                if (Calendar.getInstance().timeInMillis - targetTimeS*1000 > 1000) { //设置后的时间和目标时间差值超过1s则视为失败
                    Log.e("error", "set sys time failed")
                    runOnUiThread {
                        Toast.makeText(this, "set sys time failed, retry", Toast.LENGTH_SHORT).show()
                    }
                    setTimeFailedTime += 1
                    if (setTimeFailedTime > 3) {
                        runOnUiThread {
                            Toast.makeText(this, "can not set sys time, return", Toast.LENGTH_SHORT).show()
                            progressBar.progress = 0
                            progressTextView.text = "Failed"
                        }
                        return
                    }
                    targetTimeS -= 290
                    continue
                }
                setTimeFailedTime = 0 //clear the error time
                tmpToken = Token(targetTimeS, AuthUtils.getAS(uuid))
                tokenArray.add(tmpToken)

                if ((100*(targetTimeS - startTimeS)/totalS).toInt() - progress >= 2) {
                    Log.v("tokengGeneratorProgres", progress.toString())
                    progress = (100*(targetTimeS - startTimeS)/totalS).toInt()
                    runOnUiThread {
                        progressTextView.text = progress.toString()+"%"
                        progressBar.progress = progress
                    }
                }
            }

            Log.v("tokenGenerator", "Token count:")
            Log.v("tokenGenerator", tokenArray.count().toString())

            val joinToString = tokenArray.joinToString(",","","",-1,"",{
                "\n{\n\"time\":${it.time},\n\"token\":\"${it.token}\"\n}" as CharSequence
            })
            val tokenJsonString = "[\n${joinToString}\n]"

            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e("CAToken", "No External Storage")
                return
            }
            val dateFormatter = SimpleDateFormat("yyyyMMddHHmmss")
            val start = dateFormatter.format(startTime)
            val end = dateFormatter.format(targetCalendar.time)

            val fileName = "/sdcard/Download/" + start + "-" + end + ".json"
            writeSDFile(fileName, tokenJsonString)

            runOnUiThread {
                Toast.makeText(this, "Finish", Toast.LENGTH_SHORT).show()
                progressTextView.text = "Finish"
            }
        } catch (e: Exception) {
            Log.e("1", "Error, ${e}")
            runOnUiThread {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                progressTextView.text = "ERROR"
            }
        }

    }

    fun setSystemTimeWithCalendar(c: Calendar) {

        requestPermission()

        val `when` = c.timeInMillis

        if (`when` / 1000 < Integer.MAX_VALUE) {
            SystemClock.setCurrentTimeMillis(`when`)
        }
    }

    @Throws(InterruptedException::class, IOException::class)
    internal fun requestPermission() {
        createSuProcess("chmod 666 /dev/alarm").waitFor()
    }

    @Throws(IOException::class)
    internal fun createSuProcess(): Process {
        val rootUser = File("/system/xbin/ru")
        if (rootUser.exists()) {
            return Runtime.getRuntime().exec(rootUser.absolutePath)
        } else {
            return Runtime.getRuntime().exec("su")
        }
    }

    @Throws(IOException::class)
    internal fun createSuProcess(cmd: String): Process {

        var os: DataOutputStream? = null
        val process = createSuProcess()

        try {
            os = DataOutputStream(process.outputStream)
            os.writeBytes(cmd + "\n")
            os.writeBytes("exit $?\n")
        } finally {
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                }

            }
        }

        return process
    }

    @Throws(IOException::class)
    fun writeSDFile(fileName: String, write_str: String) {

        val file = File(fileName)

        val fos = FileOutputStream(file)

        val bytes = write_str.toByteArray()

        fos.write(bytes)

        fos.close()
    }
}
