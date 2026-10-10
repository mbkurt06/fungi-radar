package com.fungiradar.app
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import kotlin.math.max
import kotlin.math.min

class MushroomDetector(private val context:Context):AutoCloseable{
    private var taskEngine:ObjectDetector?=null
    private var yoloEngine:YoloRawDetector?=null
    private var onnxEngine:OnnxYoloDetector?=null
    var activeModel:String?=null;private set
    var engineType:String="none";private set
    var lastError:String?=null;private set

    fun load(file:File?):Boolean{
        close();lastError=null;if(file==null)return false
        if(file.extension.equals("onnx",true)){
            return try{
                onnxEngine=OnnxYoloDetector(context,file)
                activeModel=file.name
                engineType=onnxEngine?.description?:"ONNX YOLO"
                true
            }catch(e:Exception){lastError="ONNX: "+e.message;false}
        }
        try{
            taskEngine=ObjectDetector.createFromFileAndOptions(file,ObjectDetector.ObjectDetectorOptions.builder().setMaxResults(25).setScoreThreshold(0.08f).build())
            activeModel=file.name;engineType="Task Vision";return true
        }catch(te:Exception){
            try{
                yoloEngine=YoloRawDetector(context,file);activeModel=file.name;engineType=yoloEngine?.description?:"YOLO";return true
            }catch(ye:Exception){lastError="Task Vision: "+te.message+"; YOLO: "+ye.message;return false}
        }
    }

    val available:Boolean get()=taskEngine!=null||yoloEngine!=null||onnxEngine!=null

    fun detect(bitmap:Bitmap,threshold:Float,targetLabel:String?):List<Detection>{
        onnxEngine?.let{return it.detect(bitmap,threshold,targetLabel)}
        yoloEngine?.let{return it.detect(bitmap,threshold,targetLabel)}
        val detector=taskEngine?:return emptyList();val w=bitmap.width.toFloat();val h=bitmap.height.toFloat()
        val raw=detector.detect(TensorImage.fromBitmap(bitmap)).mapNotNull{res->
            val cat=res.categories.maxByOrNull{it.score}?:return@mapNotNull null
            if(cat.score<threshold)return@mapNotNull null
            val label=cat.label.ifBlank{"Object"};if(!targetLabel.isNullOrBlank()&&!label.equals(targetLabel,true))return@mapNotNull null
            val r=res.boundingBox;Detection(RectF((r.left/w).coerceIn(0f,1f),(r.top/h).coerceIn(0f,1f),(r.right/w).coerceIn(0f,1f),(r.bottom/h).coerceIn(0f,1f)),label,cat.score)
        }.sortedByDescending{it.score}
        val kept=mutableListOf<Detection>();for(c in raw){if(kept.none{it.label.equals(c.label,true)&&iou(it.rect,c.rect)>0.65f}){kept+=c;if(kept.size>=12)break}};return kept
    }

    private fun iou(a:RectF,b:RectF):Float{val l=max(a.left,b.left);val t=max(a.top,b.top);val r=min(a.right,b.right);val bot=min(a.bottom,b.bottom);val iw=max(0f,r-l);val ih=max(0f,bot-t);val inter=iw*ih;val u=a.width()*a.height()+b.width()*b.height()-inter;return if(u<=0f)0f else inter/u}

    override fun close(){
        taskEngine?.close();taskEngine=null
        yoloEngine?.close();yoloEngine=null
        onnxEngine?.close();onnxEngine=null
        activeModel=null;engineType="none"
    }
}
