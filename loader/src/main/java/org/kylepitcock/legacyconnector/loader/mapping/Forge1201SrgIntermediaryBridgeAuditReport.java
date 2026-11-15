package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/** Deterministic, build-local per-symbol report for the read-only SRG -> Intermediary candidate path. */
public final class Forge1201SrgIntermediaryBridgeAuditReport {
    private Forge1201SrgIntermediaryBridgeAuditReport() {}
    private record Counts(int total, Map<String, Integer> byStatus) {}

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: Forge1201SrgIntermediaryBridgeAuditReport <joined.tsrg> <source-tiny> <target-tiny> <report.json>");
        }
        Path tsrg=Path.of(args[0]), source=Path.of(args[1]), target=Path.of(args[2]), report=Path.of(args[3]);
        var index=Forge1201SrgIntermediaryBridgeResolver.readIndex(tsrg,source,target);
        Counts classes=countClasses(index), methods=countMethods(index), fields=countFields(index);
        Files.createDirectories(report.toAbsolutePath().getParent());
        try(BufferedWriter out=Files.newBufferedWriter(report,StandardCharsets.UTF_8)){
            writeHeader(out,tsrg,source,target,classes,methods,fields);
            writeClasses(out,index);
            writeMethods(out,index);
            writeFields(out,index);
            out.write("  },\n  \"limitations\": [\n");
            line(out,"    \"Read-only feasibility report; no selector is transformed.\",");
            line(out,"    \"External descriptor types are preserved but not verified against the target classpath.\",");
            line(out,"    \"Report ends at target official-obfuscated 1.21.1; final target Mojang-name conversion is separate.\",");
            line(out,"    \"A structurally exact mapping candidate does not prove binary or gameplay compatibility.\"");
            out.write("  ]\n}\n");
        }
        System.out.printf("Composed Forge SRG -> Intermediary bridge: classes %d, methods %d, fields %d; report %s%n",
                classes.total(),methods.total(),fields.total(),report);
        System.out.println("No selector transformation or runtime mapping table was produced.");
    }

    private static Counts countClasses(Forge1201SrgIntermediaryBridgeResolver.Index index){
        Map<String,Integer> counts=new TreeMap<>();
        for(var row:index.classSelectors()){
            String status=row.srgName()==null?"SRG_CLASS_NAME_MISSING":index.resolveClass(row.srgName()).status().name();
            counts.merge(status,1,Integer::sum);
        }
        return new Counts(index.classSelectors().size(),Map.copyOf(counts));
    }
    private static Counts countMethods(Forge1201SrgIntermediaryBridgeResolver.Index index){
        Map<String,Integer> counts=new TreeMap<>();
        for(var row:index.methodSelectors())counts.merge(index.resolveMethodRow(row).status().name(),1,Integer::sum);
        return new Counts(index.methodSelectors().size(),Map.copyOf(counts));
    }
    private static Counts countFields(Forge1201SrgIntermediaryBridgeResolver.Index index){
        Map<String,Integer> counts=new TreeMap<>();
        for(var row:index.fieldSelectors())counts.merge(index.resolveFieldRow(row).status().name(),1,Integer::sum);
        return new Counts(index.fieldSelectors().size(),Map.copyOf(counts));
    }

    private static void writeHeader(BufferedWriter out,Path tsrg,Path source,Path target,
            Counts classes,Counts methods,Counts fields)throws IOException{
        out.write("{\n  \"schemaVersion\": 1,\n");
        property(out,"status","PARTIAL",true);
        property(out,"sourceNamespace","minecraft:forge-srg@1.20.1",true);
        property(out,"sourceOfficialNamespace","minecraft:official-obfuscated@1.20.1",true);
        property(out,"sourceIntermediaryNamespace","minecraft:fabric-intermediary@1.20.1",true);
        property(out,"targetIntermediaryNamespace","minecraft:fabric-intermediary@1.21.1",true);
        property(out,"targetNamespace","minecraft:official-obfuscated@1.21.1",true);
        out.write("  \"completeForgeSrgSelectorPath\": false,\n  \"selectorTransformationPerformed\": false,\n  \"runtimeMappingTableProduced\": false,\n");
        out.write("  \"artifacts\": {\n");
        artifact(out,"mcpconfigJoinedTsrg",tsrg,true);
        artifact(out,"fabricIntermediary1201Tiny",source,true);
        artifact(out,"fabricIntermediary1211Tiny",target,false);
        out.write("  },\n  \"coverage\": {\n");
        writeCounts(out,"classes",classes,true);writeCounts(out,"methods",methods,true);writeCounts(out,"fields",fields,false);
        out.write("  },\n  \"symbols\": {\n    \"classes\": [\n");
    }
    private static void writeClasses(BufferedWriter out,Forge1201SrgIntermediaryBridgeResolver.Index index)throws IOException{
        boolean first=true;
        for(var row:index.classSelectors()){
            if(!first)out.write(",\n");first=false;
            if(row.srgName()==null){
                out.write("      {\"srgName\":null,\"sourceOfficialObfuscatedName\":"+quote(row.obfuscatedName())+
                        ",\"status\":\"SRG_CLASS_NAME_MISSING\",\"sourceIntermediaryName\":null,\"targetOfficialObfuscatedName\":null}");
                continue;
            }
            var r=index.resolveClass(row.srgName());
            out.write("      {\"srgName\":"+quote(row.srgName())+",\"sourceOfficialObfuscatedName\":"+quote(row.obfuscatedName())+
                    ",\"status\":"+quote(r.status().name())+",\"sourceIntermediaryName\":"+nullable(r.sourceIntermediaryClass())+
                    ",\"targetOfficialObfuscatedName\":"+nullable(r.targetOfficialObfuscatedClass())+"}");
        }
        out.write("\n    ],\n    \"methods\": [\n");
    }
    private static void writeMethods(BufferedWriter out,Forge1201SrgIntermediaryBridgeResolver.Index index)throws IOException{
        boolean first=true;
        for(var row:index.methodSelectors()){
            if(!first)out.write(",\n");first=false;
            var r=index.resolveMethodRow(row);
            out.write("      {\"srg\":{\"owner\":"+nullable(row.srgOwner())+",\"name\":"+quote(row.srgName())+
                    ",\"descriptor\":"+nullable(row.srgDescriptor())+"},\"sourceOfficialObfuscated\":{\"owner\":"+
                    quote(row.obfuscatedOwner())+",\"name\":"+quote(row.obfuscatedName())+",\"descriptor\":"+
                    quote(row.obfuscatedDescriptor())+"},\"status\":"+quote(r.status().name())+",\"candidates\":");
            writeCandidates(out,r);out.write("}");
        }
        out.write("\n    ],\n    \"fields\": [\n");
    }
    private static void writeFields(BufferedWriter out,Forge1201SrgIntermediaryBridgeResolver.Index index)throws IOException{
        boolean first=true;
        for(var row:index.fieldSelectors()){
            if(!first)out.write(",\n");first=false;
            var r=index.resolveFieldRow(row);
            out.write("      {\"srg\":{\"owner\":"+nullable(row.srgOwner())+",\"name\":"+quote(row.srgName())+
                    ",\"descriptor\":"+nullable(row.sourceDescriptor())+"},\"sourceOfficialObfuscated\":{\"owner\":"+
                    quote(row.obfuscatedOwner())+",\"name\":"+quote(row.obfuscatedName())+",\"descriptor\":"+
                    nullable(row.obfuscatedDescriptor())+"},\"status\":"+quote(r.status().name())+",\"candidates\":");
            writeCandidates(out,r);out.write("}");
        }
        out.write("\n    ]");
    }
    private static void writeCandidates(BufferedWriter out,Forge1201SrgIntermediaryBridgeResolver.SymbolResolution r)throws IOException{
        out.write("[");
        for(int i=0;i<r.candidates().size();i++){
            if(i>0)out.write(",");
            var c=r.candidates().get(i);
            out.write("{\"sourceIntermediaryOwner\":"+nullable(c.sourceIntermediaryOwner())+
                    ",\"sourceIntermediaryId\":"+nullable(c.sourceIntermediaryId())+
                    ",\"sourceIntermediaryDescriptor\":"+nullable(c.sourceIntermediaryDescriptor())+
                    ",\"targetOfficialObfuscatedOwner\":"+nullable(c.targetOfficialObfuscatedOwner())+
                    ",\"targetOfficialObfuscatedName\":"+nullable(c.targetOfficialObfuscatedName())+
                    ",\"targetOfficialObfuscatedDescriptor\":"+nullable(c.targetOfficialObfuscatedDescriptor())+
                    ",\"targetIntermediaryOwner\":"+nullable(c.targetIntermediaryOwner())+
                    ",\"targetIntermediaryDescriptor\":"+nullable(c.targetIntermediaryDescriptor())+
                    ",\"externalDescriptorTypeCount\":"+c.externalDescriptorTypeCount()+"}");
        }
        out.write("]");
    }
    private static void writeCounts(BufferedWriter out,String name,Counts c,boolean comma)throws IOException{
        out.write("    "+quote(name)+": {\"total\":"+c.total()+",\"byStatus\":{");
        boolean first=true;
        for(var e:new TreeMap<>(c.byStatus()).entrySet()){
            if(!first)out.write(",");first=false;out.write(quote(e.getKey())+":"+e.getValue());
        }
        out.write("}}"+(comma?",":"")+"\n");
    }
    private static void artifact(BufferedWriter out,String name,Path p,boolean comma)throws IOException{
        out.write("    "+quote(name)+": {\"fileName\":"+quote(p.getFileName().toString())+
                ",\"sizeBytes\":"+Files.size(p)+",\"sha256\":"+quote(sha256(p))+"}"+(comma?",":"")+"\n");
    }
    private static void property(BufferedWriter out,String name,String value,boolean comma)throws IOException{
        out.write("  "+quote(name)+":"+quote(value)+(comma?",":"")+"\n");
    }
    private static void line(BufferedWriter out,String value)throws IOException{out.write(value+"\n");}
    private static String nullable(String s){return s==null?"null":quote(s);}
    private static String quote(String s){return "\""+escape(s)+"\"";}
    private static String escape(String s){
        StringBuilder b=new StringBuilder(s.length()+16);
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            switch(c){
                case '"'->b.append("\\\"");
                case '\\'->b.append("\\\\");
                case '\n'->b.append("\\n");
                case '\r'->b.append("\\r");
                case '\t'->b.append("\\t");
                default->{if(c<0x20)b.append(String.format("\\u%04x",(int)c));else b.append(c);}
            }
        }
        return b.toString();
    }
    private static String sha256(Path p)throws IOException{
        final MessageDigest d;
        try{d=MessageDigest.getInstance("SHA-256");}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable.",e);}
        try(var in=Files.newInputStream(p)){byte[] buf=new byte[64*1024];int n;while((n=in.read(buf))!=-1)d.update(buf,0,n);}
        StringBuilder h=new StringBuilder(64);for(byte v:d.digest())h.append(String.format("%02x",v&0xff));return h.toString();
    }
}
