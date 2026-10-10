package com.fungiradar.app
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

class YoloRawDetector(context:Context,file:File):AutoCloseable{
    private val interpreter:Interpreter
    private val labels:List<String>
    private val inputW:Int;private val inputH:Int
    private val channelsFirst:Boolean;private val anchors:Int;private val channels:Int;private val classes:Int
    val description:String
    init{
        val model=FileInputStream(file).channel.use{it.map(FileChannel.MapMode.READ_ONLY,0,it.size())}
        interpreter=Interpreter(model,Interpreter.Options().apply{numThreads=4;setUseXNNPACK(true)})
        val input=interpreter.getInputTensor(0);require(input.dataType()==DataType.FLOAT32){"YOLO input must be FLOAT32"}
        val ishape=input.shape();require(ishape.size==4&&ishape[0]==1&&ishape[3]==3){"Unsupported YOLO input shape: ${ishape.contentToString()}"}
        inputH=ishape[1];inputW=ishape[2]
        val os=interpreter.getOutputTensor(0).shape();require(os.size==3&&os[0]==1){"Unsupported YOLO output shape: ${os.contentToString()}"}
        channelsFirst=os[1]<os[2];channels=if(channelsFirst)os[1]else os[2];anchors=if(channelsFirst)os[2]else os[1];classes=channels-4
        require(classes>0){"YOLO output has no class channels"}
        val all=context.assets.open("openimages_v7_labels.txt").bufferedReader().readLines()
        labels=if(classes==all.size)all else List(classes){"class_$it"}
        description="YOLO raw • ${inputW}×${inputH} • $classes classes"
    }
    fun detect(bitmap:Bitmap,threshold:Float,targetLabel:String?):List<Detection>{
        val scaled=Bitmap.createScaledBitmap(bitmap,inputW,inputH,true)
        val input=ByteBuffer.allocateDirect(inputW*inputH*3*4).order(ByteOrder.nativeOrder())
        val pixels=IntArray(inputW*inputH);scaled.getPixels(pixels,0,inputW,0,0,inputW,inputH)
        for(p in pixels){input.putFloat(((p shr 16)and 255)/255f);input.putFloat(((p shr 8)and 255)/255f);input.putFloat((p and 255)/255f)}
        input.rewind()
        val output=ByteBuffer.allocateDirect(interpreter.getOutputTensor(0).numBytes()).order(ByteOrder.nativeOrder())
        interpreter.run(input,output);output.rewind();val values=FloatArray(output.capacity()/4);output.asFloatBuffer().get(values)
        fun v(c:Int,a:Int)=if(channelsFirst)values[c*anchors+a]else values[a*channels+c]
        val candidates=ArrayList<Detection>()
        for(a in 0 until anchors){
            var bc=-1;var bs=0f
            for(c in 0 until classes){val s=v(4+c,a);if(s>bs){bs=s;bc=c}}
            if(bc<0||bs<threshold)continue
            val label=labels.getOrElse(bc){"class_$bc"}
            if(!targetLabel.isNullOrBlank()&&!label.equals(targetLabel,true))continue
            var cx=v(0,a);var cy=v(1,a);var bw=v(2,a);var bh=v(3,a)
            if(cx>1.5f||cy>1.5f||bw>1.5f||bh>1.5f){cx/=inputW;cy/=inputH;bw/=inputW;bh/=inputH}
            val r=RectF((cx-bw/2).coerceIn(0f,1f),(cy-bh/2).coerceIn(0f,1f),(cx+bw/2).coerceIn(0f,1f),(cy+bh/2).coerceIn(0f,1f))
            if(r.width()>0.002f&&r.height()>0.002f)candidates+=Detection(r,label,bs)
        }
        val kept=ArrayList<Detection>()
        for(d in candidates.sortedByDescending{it.score}){if(kept.none{it.label.equals(d.label,true)&&iou(it.rect,d.rect)>0.45f}){kept+=d;if(kept.size>=20)break}}
        return kept
    }
    private fun iou(a:RectF,b:RectF):Float{val l=max(a.left,b.left);val t=max(a.top,b.top);val r=min(a.right,b.right);val bot=min(a.bottom,b.bottom);val iw=max(0f,r-l);val ih=max(0f,bot-t);val inter=iw*ih;val u=a.width()*a.height()+b.width()*b.height()-inter;return if(u<=0f)0f else inter/u}
    override fun close()=interpreter.close()
}
