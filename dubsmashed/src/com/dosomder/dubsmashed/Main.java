package com.dosomder.dubsmashed;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Main implements IXposedHookLoadPackage {
	private static final boolean DEBUG = false;

	HookVariant variants[] = null;
	private void InitVariants() {
		variants = new HookVariant[] {
				new HookVariant("com.mobilemotion.dubsmash.encoding.CodecOutputSurfaceManager", "setupRenderer", android.graphics.Bitmap.class, setupRenderer),
				new HookVariant("com.mobilemotion.dubsmash.encoding.CodecOutputSurfaceManager", "setupRenderer", android.graphics.Bitmap.class, android.graphics.Bitmap.class, setupRenderer),
				new HookVariant("com.mobilemotion.dubsmash.encoding.CodecOutputSurfaceManager", "setupRenderer", android.graphics.Bitmap.class, android.graphics.Bitmap.class, android.graphics.Bitmap.class, setupRenderer),
				new HookVariant("com.mobilemotion.dubsmash.creation.video.encoding.CodecOutputSurfaceManager", "setupRenderer", android.graphics.Bitmap.class, android.graphics.Bitmap.class, android.graphics.Bitmap.class, setupRenderer),

				new HookVariant("com.github.hiteshsondhi88.libffmpeg.FFmpeg", "execute", String.class, com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler.class, execute),
		};
	}
	
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
		 if(variants == null)
			 InitVariants();

		 Hooking hk = null;
		 for(HookVariant var : variants) {
			 Object[] paramAndCallback = Arrays.copyOf(var.params, var.params.length + 1);
			 paramAndCallback[var.params.length] = var.callback;
			 hk = TryHook(var.clazz, lpparam.classLoader, var.method, paramAndCallback);

			 if(hk == Hooking.HookOK) {
				 XposedBridge.log("hooked " + var.method);
				 break;
			 } else if(hk == Hooking.MethodNotFound) {
				 XposedBridge.log(var.method + " -> method not found");
			 } else if(hk == Hooking.ClassNotFound) {
				 XposedBridge.log(var.clazz + "  -> class not found");
			 }
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
