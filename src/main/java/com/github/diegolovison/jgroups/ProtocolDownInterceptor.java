package com.github.diegolovison.jgroups;

import java.util.Date;
import java.util.concurrent.Callable;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.stack.Protocol;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

public class ProtocolDownInterceptor {

   @RuntimeType
   public static Object intercept(@Origin Class clazz, @AllArguments Object[] args, @SuperCall Callable<?> callable) throws Exception {

      Object value = callable.call();

      Message message = (Message) args[0];
      Address addressSrc = message.getSrc();
      Address addressDest = message.getDest();
      String src = addressSrc != null ? addressSrc.toString() : null;
      String dest = addressDest != null ? addressDest.toString() : null;
      Class<? extends Protocol> protocolClass = clazz;
      Date when = new Date();
      long size = message.size();

      StatsStore.getInstance().add(new StatsValue(src, dest, protocolClass, when, size));

      return value;
   }
}