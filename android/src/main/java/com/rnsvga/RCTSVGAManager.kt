package com.rnsvga

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.util.Log
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.opensource.svgaplayer.*
import java.lang.Exception
import java.net.URL

class RCTSVGAManager : SimpleViewManager<RCTSVGAImageView>() {
  lateinit var context: ThemedReactContext
  override fun getName(): String {
    return REACT_CLASS
  }

  public override fun createViewInstance(c: ThemedReactContext): RCTSVGAImageView {
    context = c
    return RCTSVGAImageView(c, null, 0)
  }

  @ReactProp(name = "source")
  fun setSource(view: RCTSVGAImageView, source: String) {
    try {
      var url = source
      var imageURL = ""
      var imageKey = ""
      if(source.contains("|")){
        val info = source.split("|")
        url = info[0]
        imageURL = info[1]
        imageKey = info[2]
      }
      val svgaParser = SVGAParser.shareParser()
      if (source.startsWith("http")) {
        svgaParser.decodeFromURL(URL(url), object : SVGAParser.ParseCompletion {
          override fun onComplete(videoItem: SVGAVideoEntity) {
            if(imageURL.isNotEmpty()){
              val dynamicEntity = SVGADynamicEntity()
              dynamicEntity.setDynamicImage(imageURL,imageKey)
              val drawable = SVGADrawable(videoItem,dynamicEntity)
              view.setImageDrawable(drawable)
              view.startAnimation()
            }else {
              view.setVideoItem(videoItem)
              view.startAnimation()
            }
          }

          override fun onError() {}
        })
      } else if (source.startsWith("file")) {
        val uri = Uri.parse(source)
        context.contentResolver?.let { resolver ->
          resolver.openAssetFileDescriptor(uri, "r")?.let { descriptor ->
            descriptor.createInputStream()?.let { inputStream ->
              val cacheKay = SVGACache.buildCacheKey(source)
              svgaParser?.decodeFromInputStream(inputStream, cacheKay, object : SVGAParser.ParseCompletion {
                override fun onComplete(videoItem: SVGAVideoEntity) {
                  view.setVideoItem(videoItem)
                  view.startAnimation()
                }

                override fun onError() {}
              }, true)
            }
          }
        }
      } else {
        val id = context.resources.getIdentifier(source, "raw", context.packageName)
        if (id > 0) {
          val inputStream = context.resources.openRawResource(id)
          val cacheKay = SVGACache.buildCacheKey(source)
          svgaParser.decodeFromInputStream(inputStream, cacheKay, object : SVGAParser.ParseCompletion {
            override fun onComplete(videoItem: SVGAVideoEntity) {
              view.setVideoItem(videoItem)
              view.startAnimation()
            }

            override fun onError() {}
          }, true)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

  }

  @ReactProp(name = "loops", defaultInt = 0)
  fun setLoops(view: RCTSVGAImageView, loops: Int) {
    view.loops = loops
  }

  @ReactProp(name = "clearsAfterStop", defaultBoolean = true)
  fun setClearsAfterStop(view: RCTSVGAImageView, clearsAfterStop: Boolean?) {
    view.clearsAfterStop = clearsAfterStop!!
  }

  @ReactProp(name = "currentState")
  fun setCurrentState(view: RCTSVGAImageView, currentState: String?) {
    view.currentState = currentState
    when (currentState) {
      "start" -> view.startAnimation()
      "pause" -> view.pauseAnimation()
      "stop" -> view.stopAnimation()
      "clear" -> view.stopAnimation(true)
      else -> {
      }
    }
  }

  @ReactProp(name = "toFrame", defaultInt = -1)
  fun setToFrame(view: RCTSVGAImageView, toFrame: Int) {
    if (toFrame < 0) {
      return
    }
    view.stepToFrame(toFrame, view.currentState == "play")
  }

  @ReactProp(name = "toPercentage", defaultFloat = -1.0f)
  fun setToPercentage(view: RCTSVGAImageView, toPercentage: Float) {
    if (toPercentage < 0) {
      return
    }
    view.stepToPercentage(toPercentage.toDouble(), view.currentState == "play")
  }

  companion object {
    const val REACT_CLASS = "RNSVGA"
  }
}
