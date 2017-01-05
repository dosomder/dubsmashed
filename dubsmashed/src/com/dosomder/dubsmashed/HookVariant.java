package com.dosomder.dubsmashed;

import de.robv.android.xposed.XC_MethodHook;

public class HookVariant {
    public String clazz;
    public String method;
    public Object[] params;
    public XC_MethodHook callback;

    public HookVariant(String clazz, String method, Object... parametersAndCallback) {
        this.clazz = clazz;
        this.method = method;

        Object[] parameters = new Object[parametersAndCallback.length - 1];
        for(int i = 0;i < parametersAndCallback.length - 1;i++) //ignore Callback
            parameters[i] = parametersAndCallback[i];
        this.params = parameters;

        this.callback = (XC_MethodHook)parametersAndCallback[parametersAndCallback.length -1];
    }
}
