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
import java.io.FileOutputStream
import java.text.SimpleDateFormat

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

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.e("CAToken", "No External Storage")
            progressTextView.text = "No External Storage"
            return
        }

        TokenTask().execute()
    }

    inner class TokenTask: AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
            progressTextView.text = "0%"
        }

        override fun doInBackground(vararg p0: String?): String {

            //generate param
            var uuid:String
            var startTimeUTC:Long
            var endTimeUTC:Long
            var fileName:String

            if (uuidEditText.text.toString().isNullOrBlank()) {
                uuid = ""
            }else{
                uuid = uuidEditText.text.toString()
            }

            if (startMonthTextField.text.toString().isNullOrBlank()) {
                if (startTimeEditText.text.toString().isNullOrBlank()) {
                    runOnUiThread {
                        progressTextView.text = "input something"
                    }
                    Log.e("1","未输入任何开始时间")
                    return ""
                }else{
                    //时间计算
                    Log.e("1","计算时间段")
                    val formatter = SimpleDateFormat("yyyyMMddHHmmss")
                    startTimeUTC = formatter.parse(startTimeEditText.text.toString()).time / 1000
                    endTimeUTC = startTimeUTC + (seekBar.progress.toLong() + 1) * 24L * 60L * 60L
                    fileName = formatter.format(Date(startTimeUTC*1000)) + "-" + formatter.format(Date(endTimeUTC*1000)) + ".json"
                }
            }else{
                //按月计算
                Log.d("1","计算月份")
                startTimeUTC = SimpleDateFormat("yyyyMM").parse(startMonthTextField.text.toString()).time/1000
                endTimeUTC = startTimeUTC + 30L*24L*60L*60L/100
                fileName = startMonthTextField.text.toString() + ".json"
            }

            if (true) {
                var formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val start = formatter.format(Date(startTimeUTC*1000))
                val end = formatter.format(Date(endTimeUTC*1000))

                Log.d("1","开始计算, 开始:${start},结束:${end}")
            }

            val tokenArray = generateToken(uuid,startTimeUTC,endTimeUTC)
            if (tokenArray == null) {
                progressTextView.text = "generate with error"
                return ""
            }

            Log.v("tokenGenerator", "Token count:${tokenArray.count()}")

            val path = "/sdcard/Download/" + fileName
            writeTokenArray(path,tokenArray)

            runOnUiThread {
                progressTextView.text = "Finish"
            }
            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

        }

    }

    fun generateToken(inUUID:String,startTimeUTC: Long, endTimeUTC: Long) : MutableList<Token>? {
        val randomUUID:Boolean = inUUID.isNullOrBlank()
        var uuid:String = inUUID

        var tokenArray:MutableList<Token> = mutableListOf()
        var tmpToken:Token?
        var progress:Int

        var currentTimeUTC = startTimeUTC
        val calendar = Calendar.getInstance()

        var setSysTimeFailedTime = 0

        while (currentTimeUTC <= endTimeUTC) {

            if (randomUUID) {
                uuid = UUID.randomUUID().toString()
            }

            calendar.timeInMillis = currentTimeUTC*1000
            setSystemTimeWithCalendar(calendar)

            if (Calendar.getInstance().timeInMillis - currentTimeUTC*1000 > 1000) {
                Log.e("error", "set sys time failed")
                runOnUiThread {
                    Toast.makeText(this, "set sys time failed, retry", Toast.LENGTH_SHORT).show()
                }
                setSysTimeFailedTime += 1
                if (setSysTimeFailedTime > 3) {
                    runOnUiThread {
                        Toast.makeText(this, "can not set sys time, return", Toast.LENGTH_SHORT).show()
                        progressTextView.text = "Failed"
                    }
                    return null
                }
                continue
            }
            setSysTimeFailedTime = 0

            tmpToken = Token(currentTimeUTC,uuid,AuthUtils.getAS(uuid))
            tokenArray.add(tmpToken)

            progress = ((currentTimeUTC-startTimeUTC)/(endTimeUTC-startTimeUTC)).toInt() * 100
            runOnUiThread {
                progressTextView.text = progress.toString()+"%"
            }

            currentTimeUTC += 290
        }

        return tokenArray
    }

    fun writeTokenArray(path:String, tokenArray:MutableList<Token>){
        var joinedString:String
        if (debugCheckBox.isChecked) {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            joinedString = tokenArray.joinToString(",", "", "", -1, "", {
                val checkString = formatter.format(Date(it.time*1000)) + "--" + Integer.toHexString(it.time.toInt())
                "\n{\n\"time\":${it.time},\n\"check\":\"${checkString}\",\n\"uuid\":\"${it.uuid}\",\n\"token\":\"${it.token}\"\n}"
            })
        }else {
            joinedString = tokenArray.joinToString(",", "", "", -1, "", {
                "\n{\n\"time\":${it.time},\n\"token\":\"${it.token}\"\n}"
            })
        }

        val tokenJsonString = "[\n${joinedString}\n]"

        writeSDFile(path, tokenJsonString)
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
