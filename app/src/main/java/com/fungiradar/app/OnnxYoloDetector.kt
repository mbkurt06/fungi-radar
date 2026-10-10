package com.fungiradar.app

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

class OnnxYoloDetector(context:Context,file:File):AutoCloseable{
    private val env=OrtEnvironment.getEnvironment()
    private val options=OrtSession.SessionOptions().apply{setIntraOpNumThreads(4)}
    private val session=env.createSession(file.absolutePath,options)
    private val inputName=session.inputNames.first()
    private val labels=context.assets.open("openimages_v7_labels.txt").bufferedReader().readLines()
    private val inputW=640
    private val inputH=640
    val description="ONNX YOLO • 640×640 • "+labels.size+" classes"

    fun detect(bitmap:Bitmap,threshold:Float,targetLabel:String?):List<Detection>{
        val scaled=Bitmap.createScaledBitmap(bitmap,inputW,inputH,true)
        val pixels=IntArray(inputW*inputH)
        scaled.getPixels(pixels,0,inputW,0,0,inputW,inputH)
        val chw=FloatArray(3*inputW*inputH)
        val plane=inputW*inputH
        for(i in pixels.indices){
            val p=pixels[i]
            chw[i]=((p shr 16) and 255)/255f
            chw[plane+i]=((p shr 8) and 255)/255f
            chw[2*plane+i]=(p and 255)/255f
        }

        OnnxTensor.createTensor(env,FloatBuffer.wrap(chw),longArrayOf(1,3,inputH.toLong(),inputW.toLong())).use{tensor->
            session.run(mapOf(inputName to tensor)).use{result->
                val out=result[0] as OnnxTensor
                val shape=out.info.shape
                require(shape.size==3&&shape[0]==1L){"Unsupported ONNX YOLO output: "+shape.contentToString()}
                val d1=shape[1].toInt();val d2=shape[2].toInt()
                val channelsFirst=d1<d2
                val channels=if(channelsFirst)d1 else d2
                val anchors=if(channelsFirst)d2 else d1
                val classes=channels-4
                require(classes>0){"ONNX YOLO output has no class channels"}
                val values=out.floatBuffer.array()
                fun v(c:Int,a:Int)=if(channelsFirst)values[c*anchors+a] else values[a*channels+c]

                val candidates=ArrayList<Detection>()
                for(a in 0 until anchors){
                    var bestClass=-1;var bestScore=0f
                    val classCount=min(classes,labels.size)
                    for(c in 0 until classCount){
                        val s=v(4+c,a)
                        if(s>bestScore){bestScore=s;bestClass=c}
                    }
                    if(bestClass<0||bestScore<threshold)continue
                    val label=labels[bestClass]
                    if(!targetLabel.isNullOrBlank()&&!label.equals(targetLabel,true))continue
                    var cx=v(0,a);var cy=v(1,a);var bw=v(2,a);var bh=v(3,a)
                    if(cx>1.5f||cy>1.5f||bw>1.5f||bh>1.5f){
                        cx/=inputW;cy/=inputH;bw/=inputW;bh/=inputH
                    }
                    val rect=RectF((cx-bw/2).coerceIn(0f,1f),(cy-bh/2).coerceIn(0f,1f),(cx+bw/2).coerceIn(0f,1f),(cy+bh/2).coerceIn(0f,1f))
                    if(rect.width()>0.002f&&rect.height()>0.002f)candidates+=Detection(rect,label,bestScore)
                }

                val kept=ArrayList<Detection>()
                for(d in candidates.sortedByDescending{it.score}){
                    if(kept.none{it.label.equals(d.label,true)&&iou(it.rect,d.rect)>0.45f}){
                        kept+=d
                        if(kept.size>=20)break
                    }
                }
                return kept
            }
        }
    }

    private fun iou(a:RectF,b:RectF):Float{
        val l=max(a.left,b.left);val t=max(a.top,b.top);val r=min(a.right,b.right);val bot=min(a.bottom,b.bottom)
        val iw=max(0f,r-l);val ih=max(0f,bot-t);val inter=iw*ih
        val union=a.width()*a.height()+b.width()*b.height()-inter
        return if(union<=0f)0f else inter/union
    }

    override fun close(){session.close();options.close()}
}
