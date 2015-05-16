package darkyenus.sbthotswap;

/**
 * @author Darkyen
 */
public class ProtocolActions {
    private static byte i = 0;

    public static final byte FILE_CHANGED = i++;
    public static final byte FILE_ADDED = i++;
    public static final byte FILE_REMOVED = i++;
    public static final byte FLUSH = i++;
}
