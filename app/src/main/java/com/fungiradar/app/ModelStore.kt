package com.fungiradar.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModelStore(private val context:Context){
    private val dir=File(context.filesDir,"models").apply{mkdirs()}
    private val preferences=context.getSharedPreferences("models",Context.MODE_PRIVATE)

    fun list():List<File> = dir.listFiles()
        ?.filter{it.isFile && (it.extension.equals("tflite",true)||it.extension.equals("onnx",true))}
        ?.sortedBy{it.name} ?: emptyList()

    fun selected():File?=list().firstOrNull{it.name==preferences.getString("selected","")}
    fun select(file:File){require(file.parentFile==dir);preferences.edit().putString("selected",file.name).apply()}

    fun import(uri:Uri):File{
        val displayName=context.contentResolver.query(uri,null,null,null,null)?.use{c->
            val i=c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if(i>=0&&c.moveToFirst()) c.getString(i) else null
        }?:"imported.tflite"
        val ext=displayName.substringAfterLast('.', "").lowercase()
        require(ext=="tflite"||ext=="onnx"){"Choose a .tflite or .onnx model file"}
        val safe=displayName.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9_.-]"),"_").take(90)
        val target=File(dir,safe+"."+ext)
        try{
            context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{input.copyTo(it)}}?:error("Cannot open selected file")
            validate(target)
        }catch(e:Exception){target.delete();throw e}
        return target
    }

    fun download(preset:ModelPreset,progress:((Int)->Unit)?=null):File{
        val source=requireNotNull(preset.url){"This model must be exported on a computer and imported."}
        val target=File(dir,preset.fileName)
        if(target.exists()&&target.length()>1_000_000)return target
        val temp=File(dir,preset.fileName+".part");temp.delete()
        var connection:HttpURLConnection?=null
        try{
            connection=(URL(source).openConnection() as HttpURLConnection).apply{
                connectTimeout=15000;readTimeout=180000;instanceFollowRedirects=true;requestMethod="GET"
                setRequestProperty("User-Agent","FungiRadar/0.1")
            }
            connection.connect()
            require(connection.responseCode in 200..299){"Download failed: HTTP "+connection.responseCode}
            val total=connection.contentLengthLong
            connection.inputStream.use{input->temp.outputStream().use{out->
                val buf=ByteArray(65536);var copied=0L
                while(true){
                    val n=input.read(buf);if(n<=0)break
                    out.write(buf,0,n);copied+=n
                    if(total>0)progress?.invoke(((copied*100)/total).toInt().coerceIn(0,100))
                }
            }}
            validate(temp)
            if(target.exists())target.delete()
            require(temp.renameTo(target)){"Could not store model"}
            return target
        }catch(e:Exception){temp.delete();throw e}finally{connection?.disconnect()}
    }

    private fun validate(file:File){require(file.length() in 1_000_000..300_000_000){"Invalid model file size"}}
}
