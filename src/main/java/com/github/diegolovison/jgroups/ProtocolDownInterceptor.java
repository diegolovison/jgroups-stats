package com.github.diegolovison.jgroups;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.Callable;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.stack.Protocol;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class ProtocolDownInterceptor {

   @RuntimeType
   public static Object intercept(@This Protocol obj, @Origin Method method, @AllArguments Object[] args, @SuperCall Callable<?> callable) throws Exception {
      Object value = callable.call();
      Message message = (Message) args[0];
      if (message != null) {
         Address addressSrc = message.getSrc();
         Address addressDest = message.getDest();

         String src = addressSrc != null ? addressSrc.toString() : null;
         String dest = addressDest != null ? addressDest.toString() : null;
         Class<? extends Protocol> protocol = (Class<? extends Protocol>) method.getDeclaringClass();
         Date when = new Date();
         long size = message.size();

         StatsStore.getInstance().add(new StatsValue(src, dest, protocol, when, size));
      }
      return value;
   }
}