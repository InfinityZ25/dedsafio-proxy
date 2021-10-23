package us.jcedeno.commands;

import java.util.concurrent.TimeUnit;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.minimessage.MiniMessage;
import us.jcedeno.DedsafioProxy;

public class GetResourcePack implements SimpleCommand {

    private final DedsafioProxy proxy;
    private static MiniMessage mini = MiniMessage.get();

    public GetResourcePack(DedsafioProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        var src = invocation.source();
        if (src instanceof Player player) {
            player.sendMessage(mini.parse("<green>You've successfully requested the resourcepack."));
            this.proxy.getServer().getScheduler()
                    .buildTask(this.proxy, () -> proxy.sendResourcepack(player, DedsafioProxy.RESOURCEPACK_URL))
                    .delay(3, TimeUnit.SECONDS).schedule();
        }

    }

}