import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The Class PageRankImpl class.
 */
public class PageRankImpl {

  /** The Constant DAMPING_FACTOR. */
  private static final double DAMPING_FACTOR = 0.85;

  /** The Constant SPACE. */
  private static final String SPACE = " ";

  /** The Constant UNDERSCORE. */
  private static final String UNDERSCORE = "_";

  /** The run. */
  private static int run = 1;

  /** The current page. */
  private String currentPage;

  /** The sink rank. */
  private double sinkRank;

  /** The set of pages. */
  private Set<String> allPages;

  /** The set of sink of nodes. */
  private Set<String> sinkOfNodes;

  /** The map of pages having links. */
  private Map<String, String> pagesHavingLinks;

  /** The map of page rank. */
  private Map<String, Double> pageRank;

  /** The map of page rank local. */
  private Map<String, Double> pageRankIntermediate;

  /** The map of out linkes from p. */
  private Map<String, Integer> outLinkesFromP;

  /** The int array of combined perplexity. */
  private int[] combinedPerplexity;

  /** The combined pages. */
  private int combinedPages;

  /** The perplexity. */
  private double perplexity;

  /** The in link sorted. */
  private InLinkSorted inLinkSorted;

  /** The sort by rank. */
  private RankSorted rankSorted;

  /** The sorted pages by ranks. */
  private SortedMap<String, Double> rankSortedPages;

  /** The sorted pages by inlinks. */
  private SortedMap<String, String> inLinkSortedPages;

  /**
   * Instantiates a new page rank impl instance member
   * variables.
   */
  public PageRankImpl() {
    allPages = new HashSet<String>();
    sinkOfNodes = new HashSet<String>();
    pagesHavingLinks = new HashMap<String, String>();
    pageRank = new HashMap<String, Double>();
    pageRankIntermediate = new HashMap<String, Double>();
    outLinkesFromP = new HashMap<String, Integer>();
    inLinkSorted = new InLinkSorted(pagesHavingLinks);
    rankSorted = new RankSorted(pageRank);
    rankSortedPages = new TreeMap<String, Double>(rankSorted);
    inLinkSortedPages = new TreeMap<String, String>(inLinkSorted);
    combinedPerplexity = new int[4];
  }

  /**
   * The Class InLinkSorted.
   */
  public class InLinkSorted implements Comparator<String> {

    /** The pair. */
    Map<String, String> pair;

    /**
     * Instantiates a new in link sorted.
     * 
     * @param pair the pair
     */
    public InLinkSorted(Map<String, String> pair) {
      this.pair = pair;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object,
     * java.lang.Object)
     */
    public int compare(String to, String with) {
      return (pair.get(to).split(SPACE).length >= pair.get(with).split(
          SPACE).length) ? -1 : 1;
    }
  }

  /**
   * The Class RankSorted.
   */
  public class RankSorted implements Comparator<String> {

    /** The pair. */
    Map<String, Double> pair;

    /**
     * Instantiates a new sort by rank.
     * 
     * @param pair the pair
     */
    public RankSorted(Map<String, Double> pair) {
      this.pair = pair;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object,
     * java.lang.Object)
     */
    public int compare(String to, String from) {
      return (pair.get(to) >= pair.get(from)) ? -1 : 1;
    }
  }

  /**
   * Calculate perplexity.
   * 
   * @param map the map
   * @return the double
   */
  public double calculatePerplexity(Map<String, Double> map) {
    return Math.pow(2.0, entropy(map));
  }

  /**
   * Entropy.
   * 
   * @param map the map
   * @return the double
   */
  public static double entropy(Map<String, Double> map) {
    double val = 0.0;
    for (String page : map.keySet()) {
      val += ((Math.log(map.get(page))) / (Math.log(2.0)) * map.get(page));
    }
    return -val;
  }

  /**
   * Calculate rank.
   */
  private void calculateRank() {
    // calculate initial perplexity for page ranks
    // available.
    perplexity = calculatePerplexity(pageRank);

    // till page rank has converged we run the algorithm
    while (hasConverged()) {
      perplexity = calculatePerplexity(pageRank);
      System.out.println("Run:" + run + " and Perplexity:" + perplexity);

      // calculate sink rank of sink pages
      sinkRank = 0.0;
      for (String sinkPage : sinkOfNodes) {
        sinkRank += pageRank.get(sinkPage);
      }

      // for all pages calculate intermediate page rank
      for (String page : allPages) {
        pageRankIntermediate.put(page, (1.0 - DAMPING_FACTOR)
            / combinedPages);
        pageRankIntermediate.put(page, pageRankIntermediate.get(page)
            + (DAMPING_FACTOR * sinkRank / combinedPages));
        // if the page is already in the map of pages having
        // links
        if (pagesHavingLinks.containsKey(page)) {
          // if the value for the page in the map is not
          // underscore
          if (!pagesHavingLinks.get(page).equals(UNDERSCORE)) {
            // for each link the page inside the map
            // calculate page
            // rank and put it inside the page rank
            // intermediate
            // hash map
            for (String link : pagesHavingLinks.get(page).split(SPACE)) {
              pageRankIntermediate.put(page,
                  pageRankIntermediate.get(page) + DAMPING_FACTOR
                      * pageRank.get(link) / outLinkesFromP.get(link));
            }
          }
        }
      }

      for (String page : pageRank.keySet()) {
        pageRank.put(page, pageRankIntermediate.get(page));
      }
      run++;
    }
  }

  /**
   * Split links inside the pages.
   * 
   * @param line the line from a file
   */
  private void splitPagesLinks(String line) {
    // split the links inside the page
    String[] currentPageLinks = line.split(SPACE, 2);

    // if more than one link found
    if (currentPageLinks.length > 1) {

      // put the first link as a key and second link as a
      // value inside
      // the hashmap containing pages with links
      pagesHavingLinks.put(currentPageLinks[0], currentPageLinks[1]);
      currentPage = currentPageLinks[0];

      // if current page not in the all pages set add to it.
      if (!allPages.contains(currentPage)) {
        allPages.add(currentPage);
      }

      String[] links = currentPageLinks[1].split(SPACE);
      // for all links in the current page

      for (String link : links) {
        // put each link inside the all pages set.
        if (!allPages.contains(link)) {
          allPages.add(link);
        }
        // if the given link already inside the map
        // outlinkfromP
        // then increase the value for the link by one.
        if (outLinkesFromP.containsKey(link)) {
          outLinkesFromP.put(link, 1 + outLinkesFromP.get(link));
        } else {
          outLinkesFromP.put(link, 1);
        }
      }
    } else {
      // else put underscore as the value for the current
      // link inside
      // the hashmap containing pages with links
      pagesHavingLinks.put(currentPageLinks[0], UNDERSCORE);
      // add current page to all pages set
      allPages.add(currentPageLinks[0]);
    }
  }

  /**
   * Sorted by rank.
   */
  private void sortedByRank() {
    // put all page ranks inside rank sorted pages map
    rankSortedPages.putAll(pageRank);
    int count = 1;
    System.out.println("Top 50 pages sorted by Pagerank");
    // print first 50 values of the map
    for (Entry<String, Double> entry : rankSortedPages.entrySet()) {
      if (count > 50) {
        break;
      } else
        System.out.println((count++) + " - " + entry.getKey() + " - "
            + entry.getValue());
    }
  }


  /**
   * Sort by in link.
   */
  private void sortByInLink() {
    // put all page ranks inside inlink sorted pages map
    inLinkSortedPages.putAll(pagesHavingLinks);
    int count = 1;
    System.out.println("Top 50 pages sorted by In Link count");
    // print first 50 values of the map
    for (Entry<String, String> entry : inLinkSortedPages.entrySet()) {
      if (count > 50) {
        break;
      } else
        System.out.println((count++) + " - " + entry.getKey() + " - "
            + entry.getValue().split(SPACE).length);
    }
  }

  /**
   * Checks for converged.
   * 
   * @return true, if successful
   */
  private boolean hasConverged() {
    boolean result = true;
    if (combinedPerplexity.length == 4) {

      // if first three values of the perplexity array are
      // same
      // to the perplexity.
      combinedPerplexity[0] = combinedPerplexity[1];
      combinedPerplexity[1] = combinedPerplexity[2];
      combinedPerplexity[2] = combinedPerplexity[3];
      combinedPerplexity[3] = (int) perplexity;

      // calculate total perplexity
      int totalCombinedPerplexity =
          combinedPerplexity[0] + combinedPerplexity[1]
              + combinedPerplexity[2] + combinedPerplexity[3];

      // it total perplexity = 4 * current perplexity print
      // it.
      if (totalCombinedPerplexity - (4 * (int) perplexity) == 0) {
        System.out.println("Perplexity at run: " + (run - 1) + " : "
            + perplexity);
        result = false;
      } else {
        result = true;
      }
    } else {
      combinedPerplexity[run % 4] = (int) perplexity;
      result = true;
    }
    return result;
  }

  /**
   * Initialize graph.
   * 
   * @param file the file
   */
  private void initializeGraph(String file) {
    String line = "";
    BufferedReader bufferedReader = null;
    try {
      // read the file contents line by line
      bufferedReader = new BufferedReader(new FileReader(new File(file)));
      while ((line = bufferedReader.readLine()) != null) {
        splitPagesLinks(line);
      }
      combinedPages = allPages.size();

      double start_page_rank = 1.0 / combinedPages;

      // initialize page rank for all pages
      for (String page : allPages) {
        // initialize sink of nodes for all pages
        if (!outLinkesFromP.containsKey(page)) {
          sinkOfNodes.add(page);
        }
        pageRank.put(page, start_page_rank);
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets the run.
   * 
   * @return the run
   */
  public static int getRun() {
    return run;
  }

  /**
   * Sets the run.
   * 
   * @param run the new run
   */
  public static void setRun(int run) {
    PageRankImpl.run = run;
  }

  /**
   * Gets the current page.
   * 
   * @return the current page
   */
  public String getCurrentPage() {
    return currentPage;
  }

  /**
   * Sets the current page.
   * 
   * @param currentPage the new current page
   */
  public void setCurrentPage(String currentPage) {
    this.currentPage = currentPage;
  }

  /**
   * Gets the sink rank.
   * 
   * @return the sink rank
   */
  public double getSinkRank() {
    return sinkRank;
  }

  /**
   * Sets the sink rank.
   * 
   * @param sinkRank the new sink rank
   */
  public void setSinkRank(double sinkRank) {
    this.sinkRank = sinkRank;
  }

  /**
   * Gets the sets the of pages.
   * 
   * @return the sets the of pages
   */
  public Set<String> getSetOfPages() {
    return allPages;
  }

  /**
   * Sets the sets the of pages.
   * 
   * @param setOfPages the new sets the of pages
   */
  public void setSetOfPages(Set<String> setOfPages) {
    this.allPages = setOfPages;
  }

  /**
   * Gets the sink of nodes.
   * 
   * @return the sink of nodes
   */
  public Set<String> getSinkOfNodes() {
    return sinkOfNodes;
  }

  /**
   * Sets the sink of nodes.
   * 
   * @param sinkOfNodes the new sink of nodes
   */
  public void setSinkOfNodes(Set<String> sinkOfNodes) {
    this.sinkOfNodes = sinkOfNodes;
  }

  /**
   * Gets the pages having links.
   * 
   * @return the pages having links
   */
  public Map<String, String> getPagesHavingLinks() {
    return pagesHavingLinks;
  }

  /**
   * Sets the pages having links.
   * 
   * @param pagesHavingLinks the pages having links
   */
  public void setPagesHavingLinks(Map<String, String> pagesHavingLinks) {
    this.pagesHavingLinks = pagesHavingLinks;
  }

  /**
   * Gets the page rank.
   * 
   * @return the page rank
   */
  public Map<String, Double> getPageRank() {
    return pageRank;
  }

  /**
   * Sets the page rank.
   * 
   * @param pageRank the page rank
   */
  public void setPageRank(Map<String, Double> pageRank) {
    this.pageRank = pageRank;
  }

  /**
   * Gets the page rank local.
   * 
   * @return the page rank local
   */
  public Map<String, Double> getPageRankLocal() {
    return pageRankIntermediate;
  }

  /**
   * Sets the page rank local.
   * 
   * @param pageRankLocal the page rank local
   */
  public void setPageRankLocal(Map<String, Double> pageRankLocal) {
    this.pageRankIntermediate = pageRankLocal;
  }

  /**
   * Gets the out linkes from p.
   * 
   * @return the out linkes from p
   */
  public Map<String, Integer> getOutLinkesFromP() {
    return outLinkesFromP;
  }

  /**
   * Sets the out linkes from p.
   * 
   * @param outLinkesFromP the out linkes from p
   */
  public void setOutLinkesFromP(Map<String, Integer> outLinkesFromP) {
    this.outLinkesFromP = outLinkesFromP;
  }

  /**
   * Gets the combined perplexity.
   * 
   * @return the combined perplexity
   */
  public int[] getCombinedPerplexity() {
    return combinedPerplexity;
  }

  /**
   * Sets the combined perplexity.
   * 
   * @param combinedPerplexity the new combined perplexity
   */
  public void setCombinedPerplexity(int[] combinedPerplexity) {
    this.combinedPerplexity = combinedPerplexity;
  }

  /**
   * Gets the combined pages.
   * 
   * @return the combined pages
   */
  public int getCombinedPages() {
    return combinedPages;
  }

  /**
   * Sets the combined pages.
   * 
   * @param combinedPages the new combined pages
   */
  public void setCombinedPages(int combinedPages) {
    this.combinedPages = combinedPages;
  }

  /**
   * Gets the perplexity.
   * 
   * @return the perplexity
   */
  public double getPerplexity() {
    return perplexity;
  }

  /**
   * Sets the perplexity.
   * 
   * @param perplexity the new perplexity
   */
  public void setPerplexity(double perplexity) {
    this.perplexity = perplexity;
  }

  /**
   * Gets the in link sorted.
   * 
   * @return the in link sorted
   */
  public InLinkSorted getInLinkSorted() {
    return inLinkSorted;
  }

  /**
   * Sets the in link sorted.
   * 
   * @param inLinkSorted the new in link sorted
   */
  public void setInLinkSorted(InLinkSorted inLinkSorted) {
    this.inLinkSorted = inLinkSorted;
  }

  /**
   * Gets the rank sorted.
   * 
   * @return the rank sorted
   */
  public RankSorted getRankSorted() {
    return rankSorted;
  }

  /**
   * Sets the rank sorted.
   * 
   * @param rankSorted the new rank sorted
   */
  public void setRankSorted(RankSorted rankSorted) {
    this.rankSorted = rankSorted;
  }

  /**
   * Gets the rank sorted pages.
   * 
   * @return the rank sorted pages
   */
  public SortedMap<String, Double> getRankSortedPages() {
    return rankSortedPages;
  }

  /**
   * Sets the rank sorted pages.
   * 
   * @param rankSortedPages the rank sorted pages
   */
  public void setRankSortedPages(SortedMap<String, Double> rankSortedPages) {
    this.rankSortedPages = rankSortedPages;
  }

  /**
   * Gets the in link sorted pages.
   * 
   * @return the in link sorted pages
   */
  public SortedMap<String, String> getInLinkSortedPages() {
    return inLinkSortedPages;
  }

  /**
   * Sets the in link sorted pages.
   * 
   * @param inLinkSortedPages the in link sorted pages
   */
  public void setInLinkSortedPages(
      SortedMap<String, String> inLinkSortedPages) {
    this.inLinkSortedPages = inLinkSortedPages;
  }

  /**
   * The main method. It takes only one argument which is
   * the location of the file in inlink format.
   * 
   * @param args the arguments
   */
  public static void main(String[] args) {
    PageRankImpl pageRankImpl = new PageRankImpl();

    // e.g. arg[0] =
    // "/home/anuj/Courses/IR/IRProject/src/WT2g.txt"
    pageRankImpl.initializeGraph(args[0]);
    pageRankImpl.calculateRank();
    pageRankImpl.sortedByRank();
    pageRankImpl.sortByInLink();
  }

}
