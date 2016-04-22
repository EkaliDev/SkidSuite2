package me.lpk.analysis;

import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;

/**
 * An {@link Interpreter} for {@link InsnValue} values.
 * 
 * @author Eric Bruneton
 * @author Bing Ran
 * 
 * @editor Matt
 */
public class InsnInterpreter extends Interpreter<InsnValue> implements Opcodes {

	public InsnInterpreter() {
		super(ASM5);
	}

	@Override
	public InsnValue newValue(final Type type) {
		if (type == null) {
			return InsnValue.UNINITIALIZED_VALUE;
		}
		switch (type.getSort()) {
		case Type.VOID:
			return null;
		case Type.BOOLEAN:
		case Type.CHAR:
		case Type.BYTE:
		case Type.SHORT:
		case Type.INT:
			return InsnValue.INT_VALUE;
		case Type.FLOAT:
			return InsnValue.FLOAT_VALUE;
		case Type.LONG:
			return InsnValue.LONG_VALUE;
		case Type.DOUBLE:
			return InsnValue.DOUBLE_VALUE;
		case Type.ARRAY:
		case Type.OBJECT:
			return new InsnValue(type);
		// InsnValue.REFERENCE_VALUE;
		default:
			throw new Error("Internal error");
		}
	}

	@Override
	public InsnValue newOperation(final AbstractInsnNode insn) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case ACONST_NULL:
			return newValue(Type.getObjectType("null"));
		case ICONST_M1:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
		case BIPUSH:
		case SIPUSH:
			return InsnValue.intValue(insn);
		case LCONST_0:
		case LCONST_1:
			return InsnValue.longValue(insn.getOpcode());
		case FCONST_0:
		case FCONST_1:
		case FCONST_2:
			return InsnValue.floatValue(insn.getOpcode());
		case DCONST_0:
		case DCONST_1:
			return InsnValue.doubleValue(insn.getOpcode());
		case LDC:
			Object cst = ((LdcInsnNode) insn).cst;
			if (cst instanceof Integer) {
				return InsnValue.intValue(cst);
			} else if (cst instanceof Float) {
				return InsnValue.floatValue(cst);
			} else if (cst instanceof Long) {
				return InsnValue.longValue(cst);
			} else if (cst instanceof Double) {
				return InsnValue.doubleValue(cst);
			} else if (cst instanceof String) {
				return InsnValue.stringValue(cst);
			} else if (cst instanceof Type) {
				int sort = ((Type) cst).getSort();
				if (sort == Type.OBJECT || sort == Type.ARRAY) {
					return newValue(Type.getObjectType("java/lang/Class"));
				} else if (sort == Type.METHOD) {
					return newValue(Type.getObjectType("java/lang/invoke/MethodType"));
				} else {
					throw new IllegalArgumentException("Illegal LDC constant " + cst);
				}
			} else if (cst instanceof Handle) {
				return newValue(Type.getObjectType("java/lang/invoke/MethodHandle"));
			} else {
				throw new IllegalArgumentException("Illegal LDC constant " + cst);
			}
		case JSR:
			return InsnValue.RETURNADDRESS_VALUE;
		case GETSTATIC:
			return newValue(Type.getType(((FieldInsnNode) insn).desc));
		case NEW:
			return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
		default:
			throw new Error("Internal error.");
		}
	}

	@Override
	public InsnValue copyOperation(final AbstractInsnNode insn, final InsnValue value) throws AnalyzerException {
		return value;
	}

	@Override
	public InsnValue unaryOperation(final AbstractInsnNode insn, final InsnValue value) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case INEG:
		case IINC:
		case L2I:
		case F2I:
		case D2I:
		case I2B:
		case I2C:
		case I2S:
			return doUnaryInt(insn, value);
		case FNEG:
		case I2F:
		case L2F:
		case D2F:
			return doUnaryFloat(insn, value);
		case LNEG:
		case I2L:
		case F2L:
		case D2L:
			return doUnaryLong(insn, value);
		case DNEG:
		case I2D:
		case L2D:
		case F2D:
			return doUnaryDouble(insn, value);
		case IFEQ:
		case IFNE:
		case IFLT:
		case IFGE:
		case IFGT:
		case IFLE:
		case TABLESWITCH:
		case LOOKUPSWITCH:
		case IRETURN:
		case LRETURN:
		case FRETURN:
		case DRETURN:
		case ARETURN:
		case PUTSTATIC:
			return null;
		case GETFIELD:
			return newValue(Type.getType(((FieldInsnNode) insn).desc));
		case NEWARRAY:
			switch (((IntInsnNode) insn).operand) {
			case T_BOOLEAN:
				return newValue(Type.getType("[Z"));
			case T_CHAR:
				return newValue(Type.getType("[C"));
			case T_BYTE:
				return newValue(Type.getType("[B"));
			case T_SHORT:
				return newValue(Type.getType("[S"));
			case T_INT:
				return newValue(Type.getType("[I"));
			case T_FLOAT:
				return newValue(Type.getType("[F"));
			case T_DOUBLE:
				return newValue(Type.getType("[D"));
			case T_LONG:
				return newValue(Type.getType("[J"));
			default:
				throw new AnalyzerException(insn, "Invalid array type");
			}
		case ANEWARRAY:
			String desc = ((TypeInsnNode) insn).desc;
			return newValue(Type.getType("[" + Type.getObjectType(desc)));
		case ARRAYLENGTH:
			return InsnValue.INT_VALUE;
		case ATHROW:
			return null;
		case CHECKCAST:
			desc = ((TypeInsnNode) insn).desc;
			return newValue(Type.getObjectType(desc));
		case INSTANCEOF:
			return InsnValue.INT_VALUE;
		case MONITORENTER:
		case MONITOREXIT:
		case IFNULL:
		case IFNONNULL:
			return null;
		default:
			throw new Error("Internal error.");
		}
	}

	private InsnValue doUnaryDouble(AbstractInsnNode insn, InsnValue value) {
		if (value.getValue() == null) {
			return InsnValue.DOUBLE_VALUE;
		}
		switch (insn.getOpcode()) {
		case DNEG:
			double d = (double) value.getValue();
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), -d);
		case I2D:
		case L2D:
		case F2D:
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), (double) value.getValue());
		}
		return InsnValue.DOUBLE_VALUE;
	}

	private InsnValue doUnaryLong(AbstractInsnNode insn, InsnValue value) {
		if (value.getValue() == null) {
			return InsnValue.LONG_VALUE;
		}
		switch (insn.getOpcode()) {
		case LNEG:
			long l = (long) value.getValue();
			return new InsnValue(InsnValue.LONG_VALUE.getType(), -l);
		case I2L:
		case F2L:
		case D2L:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), (long) value.getValue());
		}
		return InsnValue.LONG_VALUE;
	}

	private InsnValue doUnaryFloat(AbstractInsnNode insn, InsnValue value) {
		if (value.getValue() == null) {
			return InsnValue.FLOAT_VALUE;
		}
		switch (insn.getOpcode()) {
		case FNEG:
			float f = (float) value.getValue();
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), -f);
		case I2F:
		case L2F:
		case D2F:
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), (float) value.getValue());
		}
		return InsnValue.FLOAT_VALUE;
	}

	private InsnValue doUnaryInt(AbstractInsnNode insn, InsnValue value) {
		if (value.getValue() == null) {
			return InsnValue.INT_VALUE;
		}
		switch (insn.getOpcode()) {
		case INEG:
			int i = (int) value.getValue();
			return new InsnValue(InsnValue.INT_VALUE.getType(), -i);
		case IINC:
			int i2 = (int) value.getValue();
			IincInsnNode iinc = (IincInsnNode) insn;
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 + iinc.incr);
		case L2I:
		case F2I:
		case D2I:
		case I2B:
		case I2C:
		case I2S:
			return new InsnValue(InsnValue.INT_VALUE.getType(), (int) value.getValue());
		}
		return InsnValue.INT_VALUE;
	}

	@Override
	public InsnValue binaryOperation(final AbstractInsnNode insn, final InsnValue value1, final InsnValue value2) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case LCMP:
		case FCMPL:
		case FCMPG:
		case DCMPL:
		case DCMPG:
			return new InsnValue(InsnValue.INT_VALUE.getType(), value1.getValue() == value2.getValue() ? 1 : 0);
		case IALOAD:
		case BALOAD:
		case CALOAD:
		case SALOAD:
		case IADD:
		case ISUB:
		case IMUL:
		case IDIV:
		case IREM:
		case ISHL:
		case ISHR:
		case IUSHR:
		case IAND:
		case IOR:
		case IXOR:
			if (!(value1.getValue() instanceof Integer) || !(value2.getValue() instanceof Integer)) {
				return InsnValue.INT_VALUE;
			}
			int i1 = (int) value1.getValue();
			int i2 = (int) value2.getValue();
			return doBinaryInt(insn, i1, i2);
		case FALOAD:
		case FADD:
		case FSUB:
		case FMUL:
		case FDIV:
		case FREM:
			if (!(value1.getValue() instanceof Float) || !(value2.getValue() instanceof Float)) {
				return InsnValue.FLOAT_VALUE;
			}
			float f1 = (float) value1.getValue();
			float f2 = (float) value2.getValue();
			return doBinaryFloat(insn, f1, f2);
		case LALOAD:
		case LADD:
		case LSUB:
		case LMUL:
		case LDIV:
		case LREM:
		case LSHL:
		case LSHR:
		case LUSHR:
		case LAND:
		case LOR:
		case LXOR:
			if (!(value1.getValue() instanceof Long) || !(value2.getValue() instanceof Long)) {
				return InsnValue.LONG_VALUE;
			}
			long l1 = (long) value1.getValue();
			long l2 = (long) value2.getValue();
			return doBinaryLong(insn, l1, l2);
		case DALOAD:
		case DADD:
		case DSUB:
		case DMUL:
		case DDIV:
		case DREM:
			if (!(value1.getValue() instanceof Double) || !(value2.getValue() instanceof Double)) {
				return InsnValue.DOUBLE_VALUE;
			}
			double d1 = (double) value1.getValue();
			double d2 = (double) value2.getValue();
			return doBinaryDouble(insn, d1, d2);
		case AALOAD:
			return InsnValue.REFERENCE_VALUE;
		case IF_ICMPEQ:
		case IF_ICMPNE:
		case IF_ICMPLT:
		case IF_ICMPGE:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ACMPEQ:
		case IF_ACMPNE:
		case PUTFIELD:
			return null;
		default:
			throw new Error("Internal error.");
		}
	}

	private InsnValue doBinaryDouble(AbstractInsnNode insn, double d1, double d2) {
		switch (insn.getOpcode()) {
		case DALOAD:
			return InsnValue.DOUBLE_VALUE;
		case DADD:
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), d2 + d1);
		case DSUB:
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), d2 - d1);
		case DMUL:
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), d2 * d1);
		case DDIV:
			if (d1 == 0L) {
				return InsnValue.DOUBLE_VALUE;
			}
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), d2 / d1);
		case DREM:
			if (d1 == 0L) {
				return InsnValue.DOUBLE_VALUE;
			}
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), d2 % d1);
		}
		return InsnValue.DOUBLE_VALUE;
	}

	private InsnValue doBinaryLong(AbstractInsnNode insn, long l1, long l2) {
		switch (insn.getOpcode()) {
		case LALOAD:
			return InsnValue.LONG_VALUE;
		case LADD:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 + l1);
		case LSUB:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 - l1);
		case LMUL:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 * l1);
		case LDIV:
			if (l1 == 0L) {
				return InsnValue.LONG_VALUE;
			}
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 / l1);
		case LREM:
			if (l1 == 0L) {
				return InsnValue.LONG_VALUE;
			}
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 % l1);
		case LSHL:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 << l1);
		case LSHR:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 >> l1);
		case LUSHR:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 >>> l1);
		case LAND:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 & l1);
		case LOR:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 | l1);
		case LXOR:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 ^ l1);
		}
		return InsnValue.LONG_VALUE;
	}

	private InsnValue doBinaryFloat(AbstractInsnNode insn, float f1, float f2) {
		switch (insn.getOpcode()) {
		case FALOAD:
			return InsnValue.FLOAT_VALUE;
		case FADD:
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), f2 + f1);
		case FSUB:
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), f2 - f1);
		case FMUL:
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), f2 * f1);
		case FDIV:
			if (f1 == 0f) {
				return InsnValue.FLOAT_VALUE;
			}
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), f2 / f1);
		case FREM:
			if (f1 == 0f) {
				return InsnValue.FLOAT_VALUE;
			}
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), f2 % f1);
		}
		return InsnValue.FLOAT_VALUE;
	}

	private InsnValue doBinaryInt(AbstractInsnNode insn, int i1, int i2) {
		switch (insn.getOpcode()) {
		case IALOAD:
		case BALOAD:
		case CALOAD:
		case SALOAD:
			return InsnValue.INT_VALUE;
		case IADD:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 + i1);
		case ISUB:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 - i1);
		case IMUL:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 * i1);
		case IDIV:
			if (i1 == 0) {
				return InsnValue.INT_VALUE;
			}
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 / i1);
		case IREM:
			if (i1 == 0) {
				return InsnValue.INT_VALUE;
			}
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 % i1);
		case ISHL:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 << i1);
		case ISHR:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 >> i1);
		case IUSHR:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 >>> i1);
		case IAND:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 & i1);
		case IOR:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 | i1);
		case IXOR:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 ^ i1);
		}
		return InsnValue.INT_VALUE;
	}

	@Override
	public InsnValue ternaryOperation(final AbstractInsnNode insn, final InsnValue value1, final InsnValue value2, final InsnValue value3) throws AnalyzerException {
		return null;
	}

	@Override
	public InsnValue naryOperation(final AbstractInsnNode insn, final List<? extends InsnValue> values) throws AnalyzerException {
		int opcode = insn.getOpcode();
		if (opcode == MULTIANEWARRAY) {
			return newValue(Type.getType(((MultiANewArrayInsnNode) insn).desc));
		} else if (opcode == INVOKEDYNAMIC) {
			return newValue(Type.getReturnType(((InvokeDynamicInsnNode) insn).desc));
		} else {
			return newValue(Type.getReturnType(((MethodInsnNode) insn).desc));
		}
	}

	@Override
	public void returnOperation(final AbstractInsnNode insn, final InsnValue value, final InsnValue expected) throws AnalyzerException {
	}

	@Override
	public InsnValue merge(final InsnValue v, final InsnValue w) {
		if (!v.equals(w)) {
			return InsnValue.UNINITIALIZED_VALUE;
		}
		return v;
	}
}