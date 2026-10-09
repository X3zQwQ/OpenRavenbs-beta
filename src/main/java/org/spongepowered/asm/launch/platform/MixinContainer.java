package org.spongepowered.asm.launch.platform;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.service.MixinService;

public class MixinContainer {

    private static final List<String> agentClasses = new ArrayList<>();

    private final Logger logger = LogManager.getLogger("mixin");

    private final URI uri;

    private final List<IMixinPlatformAgent> agents = new ArrayList<>();

    public MixinContainer(MixinPlatformManager manager, URI uri) {
        this.uri = uri;
        for (String agentClass : agentClasses) {
            try {
                Class<IMixinPlatformAgent> clazz = (Class<IMixinPlatformAgent>) Class.forName(agentClass);
                Constructor<IMixinPlatformAgent> ctor = clazz.getDeclaredConstructor(MixinPlatformManager.class, URI.class);
                this.logger.debug("Instancing new {} for {}", new Object[] { clazz.getSimpleName(), this.uri });
                IMixinPlatformAgent agent = ctor.newInstance(manager, uri);
                this.agents.add(agent);
            } catch (Exception exception) {
                this.logger.catching(exception);
            }
        }
    }

    public URI getURI() {
        return this.uri;
    }

    public Collection<String> getPhaseProviders() {
        List<String> phaseProviders = new ArrayList<>();
        for (IMixinPlatformAgent agent : this.agents) {
            String phaseProvider = agent.getPhaseProvider();
            if (phaseProvider != null) {
                phaseProviders.add(phaseProvider);
            }
        }
        return phaseProviders;
    }

    public void prepare() {
        for (IMixinPlatformAgent agent : this.agents) {
            this.logger.debug("Processing prepare() for {}", new Object[] { agent });
            agent.prepare();
        }
    }

    public void initPrimaryContainer() {
        for (IMixinPlatformAgent agent : this.agents) {
            this.logger.debug("Processing launch tasks for {}", new Object[] { agent });
            agent.initPrimaryContainer();
        }
    }

    public void inject() {
        for (IMixinPlatformAgent agent : this.agents) {
            this.logger.debug("Processing inject() for {}", new Object[] { agent });
            agent.inject();
        }
    }

    public String getLaunchTarget() {
        for (IMixinPlatformAgent agent : this.agents) {
            String launchTarget = agent.getLaunchTarget();
            if (launchTarget != null) {
                return launchTarget;
            }
        }
        return null;
    }

    static {
        GlobalProperties.put("mixin.agents", agentClasses);
        for (String agent : MixinService.getService().getPlatformAgents()) {
            agentClasses.add(agent);
        }
        agentClasses.add("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
    }
}
