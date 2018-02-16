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
import java.text.SimpleDateFormat

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()

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

            if (!startMonthTextField.text.toString().isNullOrBlank()) {
                //按月计算
                Log.d("1","计算月份")
                startTimeUTC = SimpleDateFormat("yyyyMM").parse(startMonthTextField.text.toString()).time/1000
                endTimeUTC = startTimeUTC + 32L*24L*60L*60L
                fileName = startMonthTextField.text.toString() + ".json"
            }else if (!startTimeEditText.text.toString().isNullOrBlank()) {
                //时间计算
                Log.e("1","计算时间段")
                val formatter = SimpleDateFormat("yyyyMMddHHmmss")
                startTimeUTC = formatter.parse(startTimeEditText.text.toString()).time / 1000
                endTimeUTC = startTimeUTC + (seekBar.progress.toLong() + 1) * 24L * 60L * 60L
                fileName = formatter.format(Date(startTimeUTC*1000)) + "-" + formatter.format(Date(endTimeUTC*1000)) + ".json"
            }else if (!startYearEditText.text.isNullOrBlank()) {
                Log.d("1", "计算年")
                startTimeUTC = SimpleDateFormat("yyyy").parse(startYearEditText.text.toString()).time/1000
                endTimeUTC = startTimeUTC + 370L*24L*60L*60L
                fileName = startYearEditText.text.toString() + ".json"
            }else {
                runOnUiThread {
                    progressTextView.text = "input something"
                }
                Log.e("1","未输入任何开始时间")
                return ""
            }

            if (true) {
                //debug 具体的信息
                var formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val start = formatter.format(Date(startTimeUTC*1000))
                val end = formatter.format(Date(endTimeUTC*1000))
                Log.d("1","开始计算, 开始:${start},结束:${end}")
            }

            val path = "/sdcard/Download/" + fileName
            val file = File(path)

            file.writeText("[\n")

            //分段计算, 提高效率, 计算速度提高了十倍
            var step:Long = 7L*24L*60L*60L
            for (start in startTimeUTC..endTimeUTC+300 step step) {
                var end = start + step
                if (end > endTimeUTC) {
                    end = endTimeUTC
                }
                Log.d("1","start:${start},end:${end}")
                val tokenArray = generateToken(uuid,start,end)
                if (tokenArray == null) {
                    progressTextView.text = "failed"
                    return ""
                }

                file.appendText(joinTokenArray(tokenArray))
                if (end != endTimeUTC) {
                    file.appendText(",\n")
                }

                runOnUiThread {
                    progressTextView.text = ((start-startTimeUTC)*100/(endTimeUTC-startTimeUTC)).toString()+"%"
                }
            }

            file.appendText("\n]")

            runOnUiThread {
                progressTextView.text = "Finish"
            }

            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

        }

    }

    //根据给定的时间段计算 Token
    fun generateToken(inUUID:String,startTimeUTC: Long, endTimeUTC: Long) : MutableList<Token>? {
        val randomUUID:Boolean = inUUID.isNullOrBlank()
        var uuid:String = inUUID

        var tokenArray:MutableList<Token> = mutableListOf()
        var tmpToken:Token?

        var currentTimeUTC = startTimeUTC

        var setSysTimeFailedTime = 0

        while (currentTimeUTC <= endTimeUTC) {

            if (randomUUID) {
                uuid = UUID.randomUUID().toString()
            }

            setSystemTimeWithUTC(currentTimeUTC)

            if (Calendar.getInstance().timeInMillis - currentTimeUTC*1000 > 1000) {
                Log.e("error", "set sys time failed")
                runOnUiThread {
                    Toast.makeText(this, "set sys time failed, retry now", Toast.LENGTH_SHORT).show()
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

            currentTimeUTC += 290
        }

        return tokenArray
    }

    //使用逗号合并 token
    fun joinTokenArray(tokenArray: MutableList<Token>) : String {
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
        return joinedString
    }

    fun writeTextToFile(path: String, text: String) {
        val file = File(path)
        file.writeText(text)
    }

    fun appendTextToFile(path: String, text: String) {
        val file = File(path)
        file.appendText(text)
    }

    fun setSystemTimeWithUTC(time:Long){
        SystemClock.setCurrentTimeMillis(time*1000)
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
}
