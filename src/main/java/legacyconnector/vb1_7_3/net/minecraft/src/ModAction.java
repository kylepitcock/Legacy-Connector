package legacyconnector.vb1_7_3.net.minecraft.src;

import java.lang.reflect.*;
import java.util.*;
/** Reflection-backed GuiAPI action; callbacks execute original mod methods. */
public class ModAction implements Runnable {
    protected List<Object> handlerObjects=new ArrayList<>();protected List<String> handlerMethods=new ArrayList<>();
    protected Object[] defaultArguments=new Object[0];protected Class[] params;
    public String name;public Object data;
    public ModAction(String name,Class... params){this.name=name;this.params=params;}
    public ModAction(Object object,String method,Class... params){this(method,params);addHandler(object,method);}
    public ModAction(Object object,String method,String name,Class... params){this(name,params);addHandler(object,method);}
    public ModAction(Object object,String method,Object data,Class... params){this(object,method,params);this.data=data;}
    Method GetMethodRecursively(Object object,String name){
        Class<?> type=object instanceof Class<?> c?c:object.getClass();
        while(type!=null){try{Method m=type.getDeclaredMethod(name,params);m.setAccessible(true);return m;}catch(NoSuchMethodException e){type=type.getSuperclass();}}
        throw new IllegalArgumentException("No action method "+name);
    }
    public ModAction addHandler(Object object,String method){GetMethodRecursively(object,method);if(!hasHandler(object,method)){handlerObjects.add(object);handlerMethods.add(method);}return this;}
    public boolean hasHandler(Object object,String method){for(int i=0;i<handlerObjects.size();i++)if(Objects.equals(object,handlerObjects.get(i))&&method.equals(handlerMethods.get(i)))return true;return false;}
    public boolean removeHandler(Object object,String method){for(int i=0;i<handlerObjects.size();i++)if(Objects.equals(object,handlerObjects.get(i))&&method.equals(handlerMethods.get(i))){handlerObjects.remove(i);handlerMethods.remove(i);return true;}return false;}
    public Object[] call(Object... arguments){
        Object[] result=new Object[handlerObjects.size()];
        for(int i=0;i<result.length;i++){Object target=handlerObjects.get(i);try{result[i]=GetMethodRecursively(target,handlerMethods.get(i)).invoke(target instanceof Class?null:target,arguments);}catch(ReflectiveOperationException e){throw new RuntimeException("GuiAPI action "+name+" failed",e instanceof InvocationTargetException ite?ite.getCause():e);}}
        return result;
    }
    public ModAction setDefaultArguments(Object... arguments){defaultArguments=arguments;return this;}
    public void run(){call(defaultArguments);}
}
