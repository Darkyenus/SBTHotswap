package darkyenus.sbthotswap.agent;

import darkyenus.sbthotswap.ProtocolActions;
import gnu.bytecode.ClassFileInput;
import gnu.bytecode.ClassType;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * @author Darkyen
 */
@SuppressWarnings("unused")
public class AgentMain {

    public static void agentmain(String agentArgs, Instrumentation inst){
        System.out.println("YES, I'm running! (agentmain)"+(agentArgs)+" & "+inst);
        initialize(agentArgs, inst);
    }

    public static void premain(String agentArgs, Instrumentation inst){
        System.out.println("YES, I'm running! (premain)  "+(agentArgs)+" & "+inst);
        initialize(agentArgs, inst);
    }

    private static void initialize(String argumentArgs, final Instrumentation inst){
        final int port = Integer.parseInt(argumentArgs);

        final Thread changeThread = new Thread("Hotswap listening thread"){
            @Override
            public void run() {
                try {
                    ServerSocket listener = new ServerSocket(port);
                    Socket socket = listener.accept();

                    DataInputStream in = new DataInputStream(socket.getInputStream());

                    ArrayList<ClassDefinition> pendingChanges = new ArrayList<>();

                    while(socket.isConnected() && !socket.isClosed()){
                        final byte action = in.readByte();
                        final String fileString = (action == ProtocolActions.FLUSH) ? "" : in.readUTF();
                        final File file = new File(fileString);

                        if(action == ProtocolActions.FILE_CHANGED){
                            ClassDefinition definition = createReDefinition(file);
                            if(definition == null){
                                System.err.println("Failed to create class redefinition for "+file.getAbsolutePath());
                            }else{
                                pendingChanges.add(definition);
                            }
                        }else if(action == ProtocolActions.FLUSH){
                            try {
                                inst.redefineClasses(pendingChanges.toArray(new ClassDefinition[pendingChanges.size()]));
                            } catch (ClassNotFoundException e) {
                                System.err.println("Class for redefinition not found!");
                                e.printStackTrace();
                            } catch (UnmodifiableClassException e) {
                                System.err.println("Class for redefinition not modifiable!");
                                e.printStackTrace();
                            }
                        }else{
                            System.out.println("WARN: Unimplemented action "+action);
                        }
                    }

                    try {
                        socket.close();
                    } catch (Exception ignored) {}
                    try {
                        listener.close();
                    } catch (Exception ignored) {}
                } catch (IOException e) {
                    System.err.println("Hotswap listening thread crashed:");
                    e.printStackTrace();
                }
            }
        };
        changeThread.setDaemon(true);
        changeThread.start();
    }

    private static ClassDefinition createReDefinition(File from){
        try {
            final byte[] bytes = Files.readAllBytes(from.toPath());
            final ClassType type = ClassFileInput.readClassType(new ByteArrayInputStream(bytes));
            final String name = type.getName();
            Class clazz = Class.forName(name);
            return new ClassDefinition(clazz, bytes);
        } catch (Exception e) {
            System.err.println("Failed to create definition for " + from.getAbsolutePath());
            e.printStackTrace();
            return null;
        }
    }
}
