package com.fungiradar.app
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class FindStore(context:Context){
    private val root=File(context.filesDir,"finds").apply{mkdirs()}
    private val images=File(root,"images").apply{mkdirs()}
    private val csv=File(root,"finds.csv")
    private val stamp=SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US)
    init{if(!csv.exists())csv.writeText("timestamp,label,score,latitude,longitude,model,image\n")}
    @Synchronized fun save(bitmap:Bitmap,detection:Detection,model:String,lat:Double?,lon:Double?):Int{
        val name="find_${stamp.format(Date())}.jpg";File(images,name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,88,it)}
        csv.appendText(listOf(System.currentTimeMillis(),detection.label.replace(","," "),String.format(Locale.US,"%.4f",detection.score),lat?:"",lon?:"",model.replace(","," "),name).joinToString(",")+"\n")
        return count()
    }
    fun count():Int=if(!csv.exists())0 else (csv.useLines{it.count()}-1).coerceAtLeast(0)
}
