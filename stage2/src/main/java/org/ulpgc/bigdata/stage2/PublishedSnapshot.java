package org.ulpgc.bigdata.stage2;

import java.util.List;
import java.util.Map;

record PublishedSnapshot(Map<String, List<Integer>> index, Map<String, Book> metadata) {}
