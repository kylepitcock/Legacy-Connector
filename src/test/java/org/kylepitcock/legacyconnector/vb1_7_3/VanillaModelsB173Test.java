package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Compares shim superclass animation and pivots with the user's original Beta jar. */
class VanillaModelsB173Test {
    @Test void vanillaModelPivotsAndAnimationMatchOriginalBeta() throws Exception {
        var jar=java.nio.file.Path.of("build/devtools/b1.7.3/client.jar");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isRegularFile(jar));
        String[][] models={
            {"fh","BipedEntityModel","a:head,b:hat,c:body,d:rightArm,e:leftArm,f:rightLeg,g:leftLeg,h:ears,i:cape"},
            {"nn","QuadrupedEntityModel","d:head,e:body,f:rightHindLeg,g:leftHindLeg,h:rightFrontLeg,i:leftFrontLeg"},
            {"td","ChickenEntityModel","a:head,b:body,c:rightLeg,d:leftLeg,e:rightWing,f:leftWing,g:beak,h:wattle"},
            {"eh","PigEntityModel","d:head,e:body,f:rightHindLeg,g:leftHindLeg,h:rightFrontLeg,i:leftFrontLeg"},
            {"ej","ZombieEntityModel","a:head,b:hat,c:body,d:rightArm,e:leftArm,f:rightLeg,g:leftLeg"},
            {"lc","SkeletonEntityModel","a:head,b:hat,c:body,d:rightArm,e:leftArm,f:rightLeg,g:leftLeg"},
            {"hh","CowEntityModel","d:head,e:body,f:rightHindLeg,g:leftHindLeg,h:rightFrontLeg,i:leftFrontLeg"},
            {"mw","SheepEntityModel","d:head,e:body,f:rightHindLeg,g:leftHindLeg,h:rightFrontLeg,i:leftFrontLeg"},
            {"ea","SheepWoolEntityModel","d:head,e:body,f:rightHindLeg,g:leftHindLeg,h:rightFrontLeg,i:leftFrontLeg"}
        };
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{jar.toUri().toURL()},null)) {
            for(var row:models) {
                Class<?> originalType=loader.loadClass(row[0]);
                Class<?> shimType=Class.forName("legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model."+row[1]);
                Object original,shim;
                if(row[0].equals("nn")){original=originalType.getConstructor(int.class,float.class).newInstance(9,.2F);shim=shimType.getConstructor(int.class,float.class).newInstance(9,.2F);}
                else if(row[0].equals("fh")){original=originalType.getConstructor(float.class,float.class).newInstance(.1F,.25F);shim=shimType.getConstructor(float.class,float.class).newInstance(.1F,.25F);}
                else {original=originalType.getConstructor().newInstance();shim=shimType.getConstructor().newInstance();}
                compareParts(row,original,shim);
                for(boolean riding:new boolean[]{false,true}) {
                    originalType.getField("m").setFloat(original,.67F);originalType.getField("n").setBoolean(original,riding);
                    shimType.getField("handSwingProgress").setFloat(shim,.67F);shimType.getField("riding").setBoolean(shim,riding);
                    Class<?>[] signature={float.class,float.class,float.class,float.class,float.class,float.class};
                    originalType.getMethod("b",signature).invoke(original,1.3F,.6F,4.5F,27F,-13F,.0625F);
                    shimType.getMethod("setAngles",signature).invoke(shim,1.3F,.6F,4.5F,27F,-13F,.0625F);
                    compareParts(row,original,shim);
                }
            }
        }
    }
    private static void compareParts(String[] row,Object original,Object shim)throws Exception {
        String[] source={"a","b","c","d","e","f"},target={"pivotX","pivotY","pivotZ","pitch","yaw","roll"};
        for(String pair:row[2].split(",")) {
            String[] names=pair.split(":");
            Object oldPart=original.getClass().getField(names[0]).get(original),newPart=shim.getClass().getField(names[1]).get(shim);
            for(int i=0;i<source.length;i++)assertEquals(oldPart.getClass().getField(source[i]).getFloat(oldPart),newPart.getClass().getField(target[i]).getFloat(newPart),.0005F,row[1]+"."+names[1]+"."+target[i]);
        }
    }
}
