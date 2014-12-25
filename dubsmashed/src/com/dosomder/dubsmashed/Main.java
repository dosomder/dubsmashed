package com.dosomder.dubsmashed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Main implements IXposedHookLoadPackage {
	private static final boolean DEBUG = false;
	
	 @Override
	 public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable { 
		 if (!lpparam.packageName.equals("com.mobilemotion.dubsmash"))
	            return;
		 XposedBridge.log("Loaded dubsmashed");
		 
		 findAndHookMethod("com.github.hiteshsondhi88.libffmpeg.FFmpeg", lpparam.classLoader, "execute", String.class, com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler.class, execute);
	 }
	 
	 XC_MethodHook execute = new XC_MethodHook() {
		 @Override
		 protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
			 if(DEBUG)
				 for(Object arg : param.args)
					 XposedBridge.log("Argument is: " + arg.toString());
			 
			 //helpful: http://superuser.com/questions/753703/ffmpeg-map-optional-audio-stream & http://superuser.com/questions/606080/ffmpeg-watermark
			 //the word at the end (e.g. [out]) is an alias for the next command
			 String cmdline = param.args[0].toString();
			 //remove watermark input file
			 cmdline = cmdline.replace("-i /data/data/com.mobilemotion.dubsmash/watermerk.png", "");
			 //1:v refers to inputfile[1], i.e. the png
			 cmdline = cmdline.replace("[1:v]scale=480:480[watermerk];", "");
			 //remove the join session of dub and watermerk
			 cmdline = cmdline.replace("[dub];[dub][watermerk]overlay[out]", "[out]");
			 //since we removed inputfile[1], the audio is now at position [1] so use that for audio
			 cmdline = cmdline.replace("map 2:a", "map 1:a");
			 param.args[0] = (Object)cmdline;
			 
			 if(DEBUG)
				 XposedBridge.log("New argument is " + cmdline);
		 }
		 
		 @Override
		 protected void afterHookedMethod(MethodHookParam param) throws Throwable {
			 
		 }
	 };
}
