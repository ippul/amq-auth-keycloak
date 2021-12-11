package com.redhat.amq.security.manager;

import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import com.redhat.amq.security.rhsso.RedHatSSOCommons;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.activemq.artemis.utils.CompositeAddress;


public class RedHatSSOSecurityManager extends ActiveMQJAASSecurityManager {

   @Override
   public boolean authorize(Subject subject, Set<Role> roles, CheckType checkType, String address) {
      for(Object credential : subject.getPrivateCredentials(String.class)) {
         return new RedHatSSOCommons().permissionCheck(String.valueOf(credential), CompositeAddress.extractAddressName(address), checkType.name());
      }
      return Boolean.FALSE;
   }

   @Override
   public RedHatSSOSecurityManager init(Map<String, String> properties) {
      super.setConfigurationName(properties.get("domain"));
      return this;
   }
}