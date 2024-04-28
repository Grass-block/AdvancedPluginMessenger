package me.gb2022.apm.local;

public class Test {
    public static void main(String[] args) {
        Messenger.EVENT_BUS.registerEventListener(new Test());

        System.out.println(Messenger.queryKickMessage("114","514","114514"));
    }

    @PluginMessageHandler(Messenger.FETCH_KICK_MESSAGE)
    public void handle(MappedQueryEvent event){
        event.setProperty("message","114514");
    }
}
