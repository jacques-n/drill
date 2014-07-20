package org.apache.drill;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import com.google.common.io.Files;
import com.google.common.io.Resources;

public class ReplaceMethodInvoke {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReplaceMethodInvoke.class);

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception{
    String e = "org/apache/drill/ExampleReplaceable.class";
    String r = "org/apache/drill/exec/test/generated/FiltererGen0.class";
    URL url = Resources.getResource(r);
    byte[] clazz = Resources.toByteArray(url);
    ClassReader cr = new ClassReader(clazz);


    ClassWriter cw = writer();
    ClassVisitor visitor = new TraceClassVisitor(cw, new Textifier(), new PrintWriter(System.out));
    //getTracer(false)
    cr.accept(new HolderReplacingVisitor(visitor), ClassReader.EXPAND_FRAMES);
    byte[] output = cw.toByteArray();
    Files.write(output, new File("/src/scratch/bytes/S.class"));
    check(output);



  }

  private static final void check(byte[] b) {
    ClassReader cr = new ClassReader(b);
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    ClassVisitor cv = new CheckClassAdapter(cw);
    cr.accept(cv, 0);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, pw);

    assert sw.toString().length() == 0 : sw.toString();
  }

  private static ClassWriter writer() {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    return cw;
  }

  private static ClassVisitor getTracer(boolean asm) {
    if (asm) {
      return new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out));
    } else {
      return new TraceClassVisitor(null, new Textifier(), new PrintWriter(System.out));
    }
  }

  private static class HolderReplacingVisitor extends ClassVisitor {

    public HolderReplacingVisitor(ClassVisitor cw) {
      super(Opcodes.ASM4, cw);
    }



    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      return new HolderReplacingMethodVisitor(access, name, desc, signature, super.visitMethod(access, name, desc,
          signature, exceptions));
    }
  }

}
