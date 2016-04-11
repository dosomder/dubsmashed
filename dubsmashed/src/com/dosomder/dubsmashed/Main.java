package com.dosomder.dubsmashed;

import android.graphics.Bitmap;
import android.graphics.Color;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Main implements IXposedHookLoadPackage {
	private static final boolean DEBUG = false;
	
	private enum Hooking
	{
		HookOK,
		ClassNotFound,
		MethodNotFound,
	}
	
	private Hooking TryHook(String className, ClassLoader classLdr, String method, Object... parametersAndCallback)
	{
		try
		{
			Class<?> clazz = XposedHelpers.findClass(className, classLdr);
			Object[] parameters = new Object[parametersAndCallback.length - 1];
			for(int i = 0;i < parametersAndCallback.length - 1;i++) //ignore Callback
				parameters[i] = parametersAndCallback[i];
					
			XposedHelpers.findMethodExact(clazz, method, parameters);
			findAndHookMethod(className, classLdr, method, parametersAndCallback);
		}
		catch(de.robv.android.xposed.XposedHelpers.ClassNotFoundError e)
		{
			return Hooking.ClassNotFound;
		}
		catch(java.lang.NoSuchMethodError e)
		{
			return Hooking.MethodNotFound;
		}
		return Hooking.HookOK;
	}
		
	 @Override
	 public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		 if(!lpparam.packageName.equals("com.mobilemotion.dubsmash"))
			return;
		 XposedBridge.log("Loaded dubsmashed");
		 
		 Hooking hk = TryHook("com.mobilemotion.dubsmash.encoding.CodecOutputSurfaceManager", lpparam.classLoader, "setupRenderer", android.graphics.Bitmap.class, setupRenderer);
		 
		 if(hk == Hooking.HookOK)
			 XposedBridge.log("hooked setupRenderer");
		 else if(hk == Hooking.MethodNotFound)
		 {
			 XposedBridge.log("setupRenderer -> not found");
			 hk = TryHook("com.mobilemotion.dubsmash.encoding.CodecOutputSurfaceManager", lpparam.classLoader, "setupRenderer", android.graphics.Bitmap.class, android.graphics.Bitmap.class, setupRenderer);
			 if(hk == Hooking.HookOK)
				 XposedBridge.log("hooked setupRenderer (2)");
			 else
			 {
				 hk = TryHook("com.mobilemotion.dubsmash.encoding.CodecOutputSurfaceManager", lpparam.classLoader, "setupRenderer", android.graphics.Bitmap.class, android.graphics.Bitmap.class, android.graphics.Bitmap.class, setupRenderer);
				 if(hk == Hooking.HookOK)
					 XposedBridge.log("hooked setupRenderer(3)");
			 }
		 }
		 else if(hk == Hooking.ClassNotFound)
		 {
			 XposedBridge.log("CodecOutputSurfaceManager -> not found");
			 hk = TryHook("com.github.hiteshsondhi88.libffmpeg.FFmpeg", lpparam.classLoader, "execute", String.class, com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler.class, execute);
			 if(hk == Hooking.HookOK)
				 XposedBridge.log("hooked execute"); 
		 }
		 
		 if(hk != Hooking.HookOK)
			 XposedBridge.log("Error hooking dubsmash");
	 }
	 
	 XC_MethodHook setupRenderer = new XC_MethodHook() {
		 @Override
		 protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
			 Bitmap bm = (Bitmap) param.args[0];
			 if(DEBUG)
				 XposedBridge.log("bitmap config is: " + bm.getConfig().toString());
			 if (!bm.isMutable())
				 param.args[0] = bm.copy(Bitmap.Config.ARGB_8888, true);

			 ((Bitmap) param.args[0]).eraseColor(Color.TRANSPARENT);
		 }
		 
		 @Override
		 protected void afterHookedMethod(MethodHookParam param) throws Throwable {
			 
		 }
	 };
	 
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
