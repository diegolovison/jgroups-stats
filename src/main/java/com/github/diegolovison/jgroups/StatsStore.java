package com.github.diegolovison.jgroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatsStore {

   private static final StatsStore INSTANCE = new StatsStore();

   private final List<StatsValue> values;

   private StatsStore() {
      this.values = new ArrayList<>();
   }

   public static StatsStore getInstance() {
      return INSTANCE;
   }

   public void add(StatsValue value) {
      this.values.add(value);
   }

   public List<StatsValue> getValues() {
      return Collections.unmodifiableList(this.values);
   }
}
