package me.gb2022.apm.local;

public class Test {
    public static void main(String[] args) {
        PluginMessenger.EVENT_BUS.registerEventListener(new Test());

        System.out.println(PluginMessenger.queryKickMessage("114","514","114514"));
    }

    @PluginMessageHandler(PluginMessenger.FETCH_KICK_MESSAGE)
    public void handle(MappedQueryEvent event){
        event.setProperty("message","114514");
    }
}
