package uchicago.ffix;

import java.io.*;
import java.util.*;

import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

import uchicago.ffix.*;

public class NameParser{

  public static String getDefName(IR ir, SSAInstruction inst_) {
    if (inst_.hasDef() == false) {
      System.out.println("Parse invalid defined variables. Exit..");
      System.exit(-1);
    }

    return parserMethod(ir).get(inst_.getDef());
  }

  public static String getUseName(IR ir, SSAInstruction inst_, int useIdx) {
    if (inst_.getNumberOfUses() <= useIdx) {
      System.out.println("Parse invalid used variables with index: " + useIdx);
      System.exit(-1);
    }

    return parserMethod(ir).get(inst_.getUse(useIdx));
  }


  static boolean debug = false;
  public static void print(String str) {
    if (debug) System.out.println(str);
  }

  public static Map<Integer, String> parserMethod(IR ir) {
    Map<Integer, String> map = new HashMap<>();

    for (int i = 0; i < ir.getInstructions().length; i++) {
      SSAInstruction inst = ir.getInstructions()[i];
      if (inst == null) continue;
      print("Inst: " + inst);

      addPredefined(ir, i, inst, map);
      if (inst instanceof com.ibm.wala.ssa.SSAFieldAccessInstruction) {
        com.ibm.wala.ssa.SSAFieldAccessInstruction fieldInst = (com.ibm.wala.ssa.SSAFieldAccessInstruction)inst;
        String str = fieldInst.getDeclaredField().getName().toString();
        if (fieldInst.isStatic() == false) {
          str = map.get(inst.getUse(0)) + "." + str;
        }
        else {
          str = typeToPack(fieldInst.getDeclaredField().getDeclaringClass().getName().toString()) + "." + str;
        }

        if (inst.hasDef()) {
          map.put(inst.getDef(), str);
          print("HP " + inst.getDef() + " => " + str);
        }
        else {
          str += " = " + map.get(inst.getUse(1));
          print("HP " + str);
        }
      }
      else if (inst instanceof SSAInvokeInstruction) {
        SSAInvokeInstruction invokeInst = (SSAInvokeInstruction) inst;
        String str = "";
        int par = 0;
        if (invokeInst.isDispatch()) {
          if (map.containsKey(invokeInst.getUse(0))) {
            str += map.get(invokeInst.getUse(0)) + ".";
            par++;
          }
        }
        else {
          str += typeToPack(invokeInst.getDeclaredTarget().getDeclaringClass().getName().toString()) + ".";
        }
        str += invokeInst.getDeclaredTarget().getName().toString() + "(";

        for (; par < invokeInst.getNumberOfUses(); par++) {
          str += map.get(invokeInst.getUse(par));
          if (par < invokeInst.getNumberOfUses() - 1) { str += ", "; }
        }

        str += ")";

        if (inst.hasDef()) {
          map.put(inst.getDef(), str);
          print("HP " + inst.getDef() + " => " + str);
        }
        else print("HP " + str);
      }
      print("");
    }
    return map;
  }

  public static void addPredefined(IR ir, int instIdx, SSAInstruction inst, Map<Integer, String>map) {
    if (inst.hasDef()) {
      String[] names = ir.getLocalNames(instIdx, inst.getDef());
      if (names != null) map.put(inst.getDef(), names[0]);
    }

    for (int par = 0; par < inst.getNumberOfUses(); par++) {
      String[] names = ir.getLocalNames(instIdx, inst.getUse(par));
      if (names != null) { map.put(inst.getUse(par), names[0]); }
    }
  }

  public static String typeToPack (String type) {
    String rt = type;
    if (rt.startsWith("L")) rt = rt.substring(1);
    rt = rt.replaceAll("/", ".");
    return rt;
  }
}
