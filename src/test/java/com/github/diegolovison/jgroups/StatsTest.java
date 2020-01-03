package com.github.diegolovison.jgroups;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.protocols.MsgStats;
import org.jgroups.protocols.STATS;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

public class StatsTest {

   private static final Logger log = LogManager.getLogger(StatsTest.class);

   private JChannel channel1;
   private JChannel channel2;

   @BeforeClass
   public static void instrument() {
      AgentBuilder.Default agentBuilder = new AgentBuilder.Default();
      agentBuilder.type(ElementMatchers.isSubTypeOf(Protocol.class))
            .transform((builder, type, classLoader, module) -> {
                     return builder.method(named("down").and(takesArguments(Message.class))).intercept(MethodDelegation.to(ProtocolDownInterceptor.class));
                  }
            ).installOn(ByteBuddyAgent.install());
   }

   @Before
   public void createClusterAndSendHiMessage() throws Exception {

      cleanLogFile();
      StatsStore.getInstance().clear();

      this.channel1 = createJChannel();
      this.channel2 = createJChannel();

      assertEquals(2, channel1.getView().getMembers().size());
      assertEquals(2, channel2.getView().getMembers().size());

      channel1.send(channel2.address(), "Hi");
      channel2.send(channel1.address(), "Hey");

      // wait
      Thread.sleep(5_000);
   }

   @Test
   public void testSTATS() throws IOException {

      STATS stats1 = channel1.getProtocolStack().findProtocol(STATS.class);
      STATS stats2 = channel2.getProtocolStack().findProtocol(STATS.class);

      long sentBytesChannel1 = stats1.getSentBytes();
      long sentBytesChannel2 = stats2.getSentBytes();
      long total = sentBytesChannel1 + sentBytesChannel2;

      assertEquals(countSizeInLog(), total);
   }

   @Test
   public void testTrace() throws IOException {

      List<StatsValue> values = StatsStore.getInstance().getValues();

      // filter per node
      Map<Class<? extends Protocol>, Long> statsPerChannel = values.stream()
            .filter(s -> s.getSrc() != null)
            .collect(
                  groupingBy(s -> s.getProtocol(),
                  summingLong(s -> s.getSize()))
            );

      long total = statsPerChannel.get(TP.class);

      assertEquals(countSizeInLog(), total);
   }

   @Test
   public void testTPMessageStats() throws IOException {

      TP tp1 = this.channel1.getProtocolStack().findProtocol(TP.class);
      TP tp2 = this.channel2.getProtocolStack().findProtocol(TP.class);

      MsgStats stats1 = tp1.getMessageStats();
      MsgStats stats2 = tp2.getMessageStats();

      long sentBytesChannel1 = stats1.getNumBytesSent();
      long sentBytesChannel2 = stats2.getNumBytesSent();
      long total = sentBytesChannel1 + sentBytesChannel2;

      assertEquals(countSizeInLog(), total);
   }

   @After
   public void close() {
      this.channel1.close();
      this.channel2.close();
   }

   private static JChannel createJChannel() throws Exception {
      JChannel channel = new JChannel();

      STATS stats = new STATS();
      channel.getProtocolStack().insertProtocol(stats, ProtocolStack.Position.ABOVE, TP.class);
      channel.connect("fooCluster");
      return channel;
   }

   private void cleanLogFile() throws IOException {
      log.info("create log file");
      try (PrintWriter writer = new PrintWriter(getLogFile())) {
         writer.print("");
      }
   }

   private File getLogFile() {
      URL traceLog = StatsTest.class.getResource("/trace.log");
      File traceLogFile = new File(traceLog.getFile());
      return traceLogFile;
   }

   private long countSizeInLog() throws IOException {
      AtomicLong total = new AtomicLong();
      final Pattern pattern = Pattern.compile("size=(.+?),", Pattern.DOTALL);
      File logFile = getLogFile();
      try (Stream<String> stream = Files.lines(Paths.get(logFile.getPath()))) {
         stream.forEach(line -> {
            if (line.contains("[UDP]") && line.contains("sending msg to")) {
               final Matcher matcher = pattern.matcher(line);
               matcher.find();
               long size = Long.valueOf(matcher.group(1));
               total.addAndGet(size);
            }
         });
      }
      return total.get();
   }
}
