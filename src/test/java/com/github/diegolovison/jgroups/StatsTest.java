package com.github.diegolovison.jgroups;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;

import java.util.List;
import java.util.Map;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
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
   public void testSTATS() {

      STATS stats1 = channel1.getProtocolStack().findProtocol(STATS.class);
      STATS stats2 = channel2.getProtocolStack().findProtocol(STATS.class);

      long sentBytesChannel1 = stats1.getSentBytes();
      long sentBytesChannel2 = stats2.getSentBytes();
      long total = sentBytesChannel1 + sentBytesChannel2;

      assertTrue(total > 0);
   }

   @Test
   public void testTrace() {

      List<StatsValue> values = StatsStore.getInstance().getValues();

      // filter per node
      Map<Object, Long> statsPerChannel = values.stream()
            .filter(s -> s.getSrc() != null && s.getProtocol().equals(TP.class))
            .collect(
                  groupingBy(s -> s.getSrc(),
                  summingLong(s -> s.getSize()))
            );

      long total = statsPerChannel.values().stream().reduce(0L, Long::sum);

      assertTrue(total > 0);
   }

   @Test
   public void testTPMessageStats() {

      TP tp1 = this.channel1.getProtocolStack().findProtocol(TP.class);
      TP tp2 = this.channel2.getProtocolStack().findProtocol(TP.class);

      MsgStats stats1 = tp1.getMessageStats();
      MsgStats stats2 = tp2.getMessageStats();

      long sentBytesChannel1 = stats1.getNumBytesSent();
      long sentBytesChannel2 = stats2.getNumBytesSent();
      long total = sentBytesChannel1 + sentBytesChannel2;

      assertTrue(total > 0);
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
}
