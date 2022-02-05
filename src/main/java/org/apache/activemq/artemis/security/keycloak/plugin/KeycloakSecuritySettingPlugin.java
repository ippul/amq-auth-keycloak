package org.apache.activemq.artemis.security.keycloak.plugin;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.SecuritySettingPlugin;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeycloakSecuritySettingPlugin implements SecuritySettingPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakSecuritySettingPlugin.class);

    private HierarchicalRepository<Set<Role>> securityRepository;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> scheduledFuture;

    private KeycloakDestinationUtils redHatSSOUtils;

    @Override
    public Map<String, Set<Role>> getSecurityRoles() {
        LOGGER.info("Loading Destinations security settings from Keycloak");
        Map<String, Set<Role>> allResources = redHatSSOUtils.getSecurityRoles();
        LOGGER.info("Found {} Destinations security settings on Keycloak", (allResources != null ? allResources.keySet().size() : 0));
        return allResources;
    }

    @Override
    public KeycloakSecuritySettingPlugin init(Map<String, String> options) {
        final Integer configurationRefreshStartDelay = Integer.valueOf(options.getOrDefault("configurationRefreshStartDelay", "120"));
        final Integer configurationRefreshInterval = Integer.valueOf(options.getOrDefault("configurationRefreshInterval", "30"));
        redHatSSOUtils = new KeycloakDestinationUtils();
        scheduledFuture = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                LOGGER.debug("Start Keycloak security settings data refresh");
                try {
                    redHatSSOUtils.checkAndApplyConfigurationsUpdate(securityRepository);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                LOGGER.debug("End Keycloak security settings data refresh");
            }
        }, configurationRefreshStartDelay, configurationRefreshInterval, TimeUnit.SECONDS);
        return this;
    }

    @Override
    public void setSecurityRepository(HierarchicalRepository<Set<Role>> securityRepository) {
        this.securityRepository = securityRepository;
    }

    @Override
    public SecuritySettingPlugin stop() {
        scheduledFuture.cancel(false);
        scheduler.shutdown();
        return this;
    }    
}
