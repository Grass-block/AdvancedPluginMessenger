import me.gb2022.pluginsmessenger.event.RemoteMessage;
import me.gb2022.pluginsmessenger.event.RemoteMessageHandler;
import me.gb2022.pluginsmessenger.event.local.ListedBroadcastEvent;
import me.gb2022.pluginsmessenger.event.local.PluginMessageHandler;
import me.gb2022.pluginsmessenger.protocol.MessageType;

public class Test {

    @PluginMessageHandler("apm:test")
    public void onEvent(ListedBroadcastEvent event) {

    }

    @RemoteMessageHandler(channel = "apm:remote", type = MessageType.MESSAGE)
    public void onEvent(RemoteMessage message) {

    }
}
