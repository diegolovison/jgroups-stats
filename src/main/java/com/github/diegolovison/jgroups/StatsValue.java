package com.github.diegolovison.jgroups;

import java.util.Date;

import org.jgroups.stack.Protocol;

public class StatsValue {

   private String src;
   private String dest;
   private Class<? extends Protocol> protocol;
   private Date when;
   private long size;

   public StatsValue(String src, String dest, Class<? extends Protocol> protocol, Date when, long size) {
      this.src = src;
      this.dest = dest;
      this.protocol = protocol;
      this.when = when;
      this.size = size;
   }

   public String getSrc() {
      return src;
   }

   public String getDest() {
      return dest;
   }

   public Class<? extends Protocol> getProtocol() {
      return protocol;
   }

   public Date getWhen() {
      return when;
   }

   public long getSize() {
      return size;
   }

   @Override
   public String toString() {
      return
            src + "\t" +
                  dest + "\t" +
                  protocol + "\t" +
                  size;
   }
}