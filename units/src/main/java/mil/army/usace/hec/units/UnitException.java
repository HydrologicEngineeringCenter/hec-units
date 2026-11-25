package mil.army.usace.hec.units;

public class UnitException extends RuntimeException {
    
    public UnitException(String msg) {
        super(msg);
    }

    public UnitException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
