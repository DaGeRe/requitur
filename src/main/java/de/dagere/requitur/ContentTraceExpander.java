package de.dagere.requitur;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.requitur.content.Content;
import de.dagere.requitur.content.RuleContent;

public class ContentTraceExpander {
   
   private static final Logger LOG = LogManager.getLogger(ExistingRuleMarker.class);

   public static List<Content> expandContentTrace(final List<Content> trace, final Map<String, Rule> rules) {
      final List<Content> result = new LinkedList<>();
      for (final Content element : trace) {
         if (element instanceof RuleContent) {
            final String value = ((RuleContent) element).getValue();
            final Rule rule = rules.get(value);
            result.addAll(ContentTraceExpander.expandTrace(rule.getElements(), rules));
         } else {
            result.add(element);
         }
      }
      return result;
   }

   public static List<Content> expandTrace(final List<ReducedTraceElement> trace, final Map<String, Rule> rules) {
      final List<Content> result = new LinkedList<>();
      for (final ReducedTraceElement element : trace) {
         for (int i = 0; i < element.getOccurences(); i++) {
            if (element.getValue() instanceof RuleContent) {
               final String value = ((RuleContent) element.getValue()).getValue();
               final Rule rule = rules.get(value);
               LOG.trace("Expanding: ", value);
               final List<Content> expandedElements = expandTrace(rule.getElements(), rules);
               result.addAll(expandedElements);
            } else {
               result.add(element.getValue());
            }
         }
      }
      return result;
   }

}
