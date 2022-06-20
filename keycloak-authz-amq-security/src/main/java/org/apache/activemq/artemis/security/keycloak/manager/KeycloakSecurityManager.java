package org.apache.activemq.artemis.security.keycloak.manager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.apache.activemq.artemis.security.keycloak.KeycloakResourcesUtils;
import org.apache.activemq.artemis.security.keycloak.model.DestinationNameKey;
import org.apache.activemq.artemis.security.keycloak.KeycloakAuthzUtils;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.activemq.artemis.utils.CompositeAddress;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KeycloakSecurityManager extends ActiveMQJAASSecurityManager implements PropertyChangeListener {

   private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakSecurityManager.class);

   private KeycloakAuthzUtils redHatSSOCommons;

   private HierarchicalRepository<DestinationNameKey> destinationNamesResolver;

   // public KeycloakSecurityManager() {
   //    super();
   //    init();
   // }

   // public KeycloakSecurityManager(String configurationName) {
   //    super();
   //    init();
   // }

   // public KeycloakSecurityManager(String configurationName, String certificateConfigurationName) {
   //    super(configurationName, certificateConfigurationName);
   //    init();
   // }

   // public KeycloakSecurityManager(String configurationName, SecurityConfiguration configuration) {
   //    super(configurationName, configuration);
   //    init();
   // }

   // public KeycloakSecurityManager(String configurationName,
   //                                    String certificateConfigurationName,
   //                                    SecurityConfiguration configuration,
   //                                    SecurityConfiguration certificateConfiguration) {
   //    super(configurationName, certificateConfigurationName, configuration, certificateConfiguration);
   //    init();
   // }

   private void init(){
      this.redHatSSOCommons = new KeycloakAuthzUtils();
      // Subscribe current instance to receve notification about Destination confguration change
      KeycloakResourcesUtils.getInstance().addPropertyChangeListener(this);
   }

   @Override
   public boolean authorize(Subject subject, Set<Role> roles, CheckType checkType, String address) {
      if(this.destinationNamesResolver == null){
         this.destinationNamesResolver = KeycloakResourcesUtils.getInstance().getDestinationNamesResolver();
      }
      for(KeycloakPrincipal<RefreshableKeycloakSecurityContext> curPrincipal : subject.getPrincipals(KeycloakPrincipal.class)){
         final DestinationNameKey destinationNameKey = destinationNamesResolver.getMatch(CompositeAddress.extractAddressName(address));
         return redHatSSOCommons.authorize(curPrincipal.getKeycloakSecurityContext().getTokenString(), destinationNameKey.getResourceName(), checkType.name());
      } 
      return Boolean.FALSE;
   }

   @Override
   public KeycloakSecurityManager init(Map<String, String> properties) {
      super.setConfigurationName(properties.get("domain"));
      this.init();
      return this;
   }

   @Override
   public void propertyChange(PropertyChangeEvent event) {
      this.destinationNamesResolver = (HierarchicalRepository<DestinationNameKey>) event.getNewValue();
      LOGGER.debug("Applyed new configuration of Destination");
   }

}