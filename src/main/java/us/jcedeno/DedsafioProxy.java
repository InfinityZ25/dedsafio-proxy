package us.jcedeno;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

import org.slf4j.Logger;

@Plugin(id = "dedsafio-bingo", name = "Dedsafio Bingo", version = "1.0", authors = { "jcedeno" })
public class DedsafioProxy {
    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public DedsafioProxy(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

}
