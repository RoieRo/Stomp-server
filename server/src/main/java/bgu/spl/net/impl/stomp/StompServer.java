package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.*;


public class StompServer {
    public static void main(String[] args) {
        Boolean tpcOrReactor;
        if(args[1].equals("tpc"))
        {
        tpcOrReactor=true;
        }
        if (tpcOrReactor=true) {
            Server.threadPerClient(
                Integer.parseInt(args[0]), // port
                    () -> new StompMessagingProtocolImp<String>(), // protocol factory
                    StompMessageEncodeDecode::new // message encoder decoder factory
            ).serve();
        }
        if (tpcOrReactor=false) {

            Server.reactor(
                    Runtime.getRuntime().availableProcessors(),
                    Integer.parseInt(args[0]), // port
                    () -> new StompMessagingProtocolImp<>(), // protocol factory
                    StompMessageEncodeDecode::new // message encoder decoder factory
            ).serve();
        }
    }
}

