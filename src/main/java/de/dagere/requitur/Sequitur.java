package de.dagere.requitur;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.requitur.content.Content;
import de.dagere.requitur.content.StringContent;

public class Sequitur {

   private static final Logger LOG = LogManager.getLogger(Sequitur.class);

   protected Map<Digram, Digram> digrams = new HashMap<>();
   protected Map<String, Rule> rules = new HashMap<>();
   protected List<Rule> ununsedRules = new LinkedList<>();
   protected Symbol startSymbol = new Symbol(this, (StringContent) null);
   protected Symbol lastSymbol = startSymbol;
   protected int ruleindex = 0;
   protected List<Content> addingElements;

   Digram link(final Symbol start, final Symbol end) {
      start.setSuccessor(end);
      end.setPredecessor(start);
      if (start.getValue() != null && end.getValue() != null) {
         final Digram newDigram = new Digram(start, end);
         handleDigram(newDigram);
         return newDigram;
      } else {
         return null;
      }
   }

   public void addElement(final Symbol symbol) {
      // TraceStateTester.assureCorrectState(this);
      if (startSymbol == null) {
         startSymbol = symbol;
         lastSymbol = symbol;
      } else {
         lastSymbol.setSuccessor(symbol);
         symbol.setPredecessor(lastSymbol);
         lastSymbol = symbol;
         if (symbol.getPredecessor().getValue() != null) {
            final Digram digram = new Digram(symbol.getPredecessor(), symbol);
            handleDigram(digram);
         }
      }
      // TraceStateTester.assureCorrectState(this);
   }

   void handleDigram(final Digram digram) {
      final Digram existing = digrams.get(digram);
      if (existing != null) {
         if (digram.getStart() != existing.getEnd()) {
            if (existing.rule != null) {
               existing.rule.use(digram);
            } else {
               Rule rule;
               if (ununsedRules.size() > 0) {
                  rule = ununsedRules.remove(0);
                  rule.setDigram(existing);
               } else {
                  rule = new Rule(this, ruleindex, existing);
                  ruleindex++;
               }
               rules.put(rule.getName(), rule);

               rule.use(digram);
            }
         }
      } else {
         digrams.put(digram, digram);
      }
   }

   public List<Content> getTrace() {
      Symbol iterator = startSymbol.getSuccessor();
      final List<Content> trace = new LinkedList<>();
      while (iterator != null) {
         trace.add(iterator.getValue());
         iterator = iterator.getSuccessor();
      }
      return trace;
   }

   public List<Content> getUncompressedTrace() {
      Symbol iterator = startSymbol.getSuccessor();
      final List<Content> trace = new LinkedList<>();
      while (iterator != null) {
         for (int i = 0; i < iterator.getOccurences(); i++) {
            trace.add(iterator.getValue());
         }
         iterator = iterator.getSuccessor();
      }
      return trace;
   }

   public Map<String, Rule> getRules() {
      return rules;
   }

   @Override
   public String toString() {
      return getTrace().toString();
   }

   public void addElements(final List<String> mytrace) {
      addingElements = new LinkedList<>();
      for (final String element : mytrace) {
         addingElements.add(new StringContent(element));
         final Symbol symbol = new Symbol(this, new StringContent(element));
         addElement(symbol);
      }
   }

   public Symbol getStartSymbol() {
      return startSymbol;
   }

   public static List<String> getExpandedTrace(final File methodTraceFile) throws IOException, FileNotFoundException {
      final List<String> trace1 = new LinkedList<>();
      try (BufferedReader br = new BufferedReader(new FileReader(methodTraceFile))) {
         String line;
         while ((line = br.readLine()) != null) {
            final List<String> elements = getCurrentValues(line, br).elements;
            trace1.addAll(elements);
         }
      }
      return trace1;
   }

   public static List<String> getExpandedTrace(List<String> lines) throws IOException {
      final List<String> trace1 = new LinkedList<>();
      for (Iterator<String> lineIterator = lines.iterator(); lineIterator.hasNext(); ) {
         String line = lineIterator.next();
         final List<String> elements = getCurrentValues(line, lineIterator).elements;
         trace1.addAll(elements);
      }
      return trace1;
   }

   static class Return {
      int readLines = 1;
      List<String> elements = new LinkedList<>();
   }
   
   public static Return getCurrentValues(String line, final Iterator<String> reader) throws IOException {
      final Return current = new Return();
      final String trimmedLine = line.trim();
      if (line.matches("[ ]*[0-9]+ x [#]?[0-9]* \\([0-9]+\\)")) {
         final String[] parts = trimmedLine.split(" ");
         final int count = Integer.parseInt(parts[0]);
         final int length = Integer.parseInt(parts[3].replaceAll("[\\(\\)]", ""));
         final List<String> subList = new LinkedList<>();
         for (int i = 0; i < length;) {
            line = reader.next();
            final Return lines = getCurrentValues(line, reader);
            current.readLines += lines.readLines;
            i += lines.readLines;
            subList.addAll(lines.elements);
         }
         for (int i = 0; i < count; i++) {
            current.elements.addAll(subList);
         }
      } else if (line.matches("[ ]*[0-9]+ x .*$")) {
         final String method = trimmedLine.substring(trimmedLine.indexOf("x") + 2);
         final String countString = trimmedLine.substring(0, trimmedLine.indexOf("x") - 1);
         final int count = Integer.parseInt(countString);
         for (int i = 0; i < count; i++) {
            current.elements.add(method);
         }
      } else if (line.matches("[ ]*[#]?[0-9]* \\([0-9]+\\)")) {
         // Do nothing - just info element, that same trace pattern occurs twice
      } else {
         current.elements.add(trimmedLine);

      }
      return current;
   }

   public static Return getCurrentValues(String line, final BufferedReader reader) throws IOException {
      final Return current = new Return();
      final String trimmedLine = line.trim();
      if (line.matches("[ ]*[0-9]+ x [#]?[0-9]* \\([0-9]+\\)")) {
         final String[] parts = trimmedLine.split(" ");
         final int count = Integer.parseInt(parts[0]);
         final int length = Integer.parseInt(parts[3].replaceAll("[\\(\\)]", ""));
         final List<String> subList = new LinkedList<>();
         for (int i = 0; i < length;) {
            line = reader.readLine();
            final Return lines = getCurrentValues(line, reader);
            current.readLines += lines.readLines;
            i += lines.readLines;
            subList.addAll(lines.elements);
         }
         for (int i = 0; i < count; i++) {
            current.elements.addAll(subList);
         }
      } else if (line.matches("[ ]*[0-9]+ x .*$")) {
         final String method = trimmedLine.substring(trimmedLine.indexOf("x") + 2);
         final String countString = trimmedLine.substring(0, trimmedLine.indexOf("x") - 1);
         final int count = Integer.parseInt(countString);
         for (int i = 0; i < count; i++) {
            current.elements.add(method);
         }
      } else if (line.matches("[ ]*[#]?[0-9]* \\([0-9]+\\)")) {
         // Do nothing - just info element, that same trace pattern occurs twice
      } else {
         current.elements.add(trimmedLine);

      }
      return current;
   }

   public List<Content> getAddingElements() {
      return addingElements;
   }

}
