package com.profiler.modifier.db.oracle;

import com.profiler.Agent;
import com.profiler.DefaultAgent;
import com.profiler.interceptor.bci.ByteCodeInstrumentor;
import com.profiler.logging.LoggerFactory;
import javassist.CtClass;
import javassist.CtMethod;

import com.profiler.modifier.AbstractModifier;
import com.profiler.trace.DatabaseRequestTracer;

import java.security.ProtectionDomain;
import java.util.logging.Level;
import com.profiler.logging.Logger;

public class OracleResultSetModifier extends AbstractModifier {

    private final Logger logger = LoggerFactory.getLogger(OracleResultSetModifier.class.getName());

    public OracleResultSetModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    public String getTargetClass() {
        return "oracle/jdbc/driver/OracleResultSetImpl";
    }

    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. " + javassistClassName);
        }
        this.byteCodeInstrumentor.checkLibrary(classLoader, javassistClassName);
        return changeMethod(javassistClassName, classFileBuffer);
    }

    private byte[] changeMethod(String javassistClassName, byte[] classfileBuffer) {
        try {
            CtClass cc = null;

            updateNextMethod(cc);
            updateCloseMethod(cc);

            printClassConvertComplete(javassistClassName);

            return cc.toBytecode();
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage(), e);
            }
        }
        return null;
    }

    private void updateNextMethod(CtClass cc) throws Exception {
        CtMethod method = cc.getDeclaredMethod("next", null);
        method.insertBefore("{" + DatabaseRequestTracer.FQCN + ".updateFetchCount(); }");
    }

    private void updateCloseMethod(CtClass cc) throws Exception {
        CtMethod method = cc.getDeclaredMethod("close", null);
        method.insertBefore("{" + DatabaseRequestTracer.FQCN + ".addResultSetData(); }");
    }
}
